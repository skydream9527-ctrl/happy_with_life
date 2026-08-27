package com.xiaoquexing.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xiaoquexing.app.fixtures.V1SchemaFixture
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1 -> v2 真实迁移测试（room-v2-schema §5 / §10 T3-T8、T16）。
 *
 * 不用 MigrationTestHelper：v1 从未导出 schema JSON，这里以手写 DDL fixture 建
 * v1 文件库（user_version=1），再用 Room 打开触发 MIGRATION_1_2。打开时 Room 会对
 * 结果库做 TableInfo 校验（room_master_table 缺失 -> validateMigration），因此本测试
 * 同时固化 T16：迁移 DDL 与 @Entity 声明必须逐列/逐索引一致，否则直接失败。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationFromV1Test {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val dbName = "migration-v1-test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        if (this::db.isInitialized) db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `v1黄金数据迁移后无损且派生正确`() = runBlocking {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 3600 * 1000

        // ---- 1) 建 v1 库并写入黄金数据（含已知缺陷：moodTag NULL）----
        val v1 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        try {
            V1SchemaFixture.CREATE_STATEMENTS.forEach(v1::execSQL)
            v1.execSQL(
                "INSERT INTO records (text, moodTag, statusTags, photoUris, voiceDuration, gpEarned, createdAt, isBackdated) " +
                    "VALUES ('记录A', '开心', '自然,美食', 'content://p1|content://p2', 0, 60, ?, 0)",
                arrayOf<Any>(now)
            )
            v1.execSQL(
                "INSERT INTO records (text, moodTag, statusTags, photoUris, voiceDuration, gpEarned, createdAt, isBackdated) " +
                    "VALUES ('记录B', NULL, '', 'file:///data/user/0/app/files/photo.jpg', 0, 56, ?, 0)",
                arrayOf<Any>(now - dayMs)
            )
            v1.execSQL(
                "INSERT INTO plant_states (plantType, totalGp, isActive, isUnlocked, plantedAt) VALUES ('TREE', 0, 1, 1, ?)",
                arrayOf<Any>(now)
            )
            v1.execSQL(
                "INSERT INTO plant_states (plantType, totalGp, isActive, isUnlocked, plantedAt) VALUES ('SAKURA', 0, 0, 0, ?)",
                arrayOf<Any>(now)
            )
            v1.execSQL(
                "INSERT INTO achievements (code, title, description, emoji, requirement, progress, isUnlocked, unlockedAt) " +
                    "VALUES ('first_record', '初次记录', '第一条小确幸', '🌱', 1, 2, 1, ?)",
                arrayOf<Any>(now)
            )
            v1.execSQL(
                "INSERT INTO spaces (name, description, type, memberCount, createdAt) VALUES ('旧空间', '', 'PERSONAL', 1, ?)",
                arrayOf<Any>(now)
            )
            v1.version = 1
        } finally {
            v1.close()
        }

        // ---- 2) Room 打开：跑迁移 + 隐式 schema 校验（失败即抛 IllegalStateException）----
        db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        db.query("SELECT COUNT(*) FROM sqlite_master").use { it.moveToFirst() }

        // ---- 3) 断言映射结果 ----
        // v1 spaces 行保留原 id；默认个人空间另建（自增 id=2）
        val space = db.spaceDao().getDefaultSpace()
        assertNotNull(space)
        assertTrue(space!!.localId > 1)

        // K3 裁决：total_gp 以记录集合为准（60+56=116），植物表不再有 GP 列
        assertEquals(116, db.recordDao().sumAllGp(space.localId))
        assertEquals(116, db.spaceDao().getTotalGp(space.localId))
        assertEquals(2, db.recordDao().countRecords(space.localId))

        // 记录 A：主键保留、心情保留、媒体拆表
        val detailA = db.recordDao().getRecordDetailById(1L)
        assertNotNull(detailA)
        assertEquals("开心", detailA!!.record.moodTag)
        assertEquals(60, detailA.record.gpFinal)
        assertEquals(2, detailA.media.count { it.type == "PHOTO" })
        assertTrue(detailA.media.filter { it.type == "PHOTO" }.all { it.mediaStatus == "PENDING_COPY" })
        assertEquals(setOf("自然", "美食"), detailA.tags.map { it.name }.toSet())

        // 记录 B：NULL 心情回填「平静」（R4：回填而非丢行）；file:// 照片视为已就绪
        val detailB = db.recordDao().getRecordDetailById(2L)
        assertNotNull(detailB)
        assertEquals("平静", detailB!!.record.moodTag)
        val photoB = detailB.media.first { it.type == "PHOTO" }
        assertEquals("READY", photoB.mediaStatus)
        assertEquals("file:///data/user/0/app/files/photo.jpg", photoB.localPath)

        // 活动植物 = TREE 实例行；SAKURA 未解锁保持锁定（条件已改为连续30天）
        val active = db.plantDao().getActiveSpacePlant(space.localId)
        assertEquals("TREE", active?.plantType)
        assertEquals(true, db.plantDao().getPlantDef("TREE")?.isUnlocked)
        assertEquals(false, db.plantDao().getPlantDef("SAKURA")?.isUnlocked)

        // 基线快照：116 GP -> 发芽(stage 1)
        val baseline = db.plantDao().getLatestSnapshot(space.localId)
        assertEquals("MIGRATED_BASELINE", baseline?.eventType)
        assertEquals(1, baseline?.stage)

        // 成就进度迁移 + 合成 UNLOCKED 事件
        val progress = db.achievementDao().getProgress("first_record", "u:1")
        assertNotNull(progress)
        assertTrue(progress!!.isUnlocked)
        assertEquals(2, progress.progress)
        assertEquals(1, db.achievementDao().countEvents("first_record", "u:1"))

        // 当日额度缓存重建
        val todayKey = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay().toInt()
        assertEquals(60, db.recordDao().sumGpOnDate(space.localId, todayKey))
    }
}
