package com.xiaoquexing.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xiaoquexing.app.data.DataBootstrap
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.fixtures.testRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 发布/删除事务集成测试（Z1-02 / room-v2-schema §6）。
 *
 * 断言 ADR-001 的核心一致性：发布一条记录后，记录行、媒体行、当日额度、空间
 * totalGp、植物阶段快照、成就进度、Outbox 事件全部就位；软删除后全部按记录集合
 * 重算（GP 回退、成就回锁、阶段可降级）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordRepositoryTransactionTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RecordRepository
    private val todayKey: Int get() = LocalDate.now().toEpochDay().toInt()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RecordRepository(db)
        runBlocking { DataBootstrap(db).ensureSeeded() }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `发布最小记录一次性写入全部派生数据`() = runBlocking {
        // 只有心情 + 照片是 PRD 允许的最小记录；此处用纯心情验证基础分
        val result = repo.publish(testRecord(moodTag = "开心", text = ""))

        assertTrue(result.recordId > 0)
        assertEquals(10, result.earnedGp) // 基础 10 ×1.05 -> 10
        assertEquals(1, result.streakDays)

        val detail = db.recordDao().getRecordDetailById(result.recordId)
        assertNotNull(detail)
        assertEquals("开心", detail!!.record.moodTag)
        assertEquals(10, detail.record.gpFinal)
        assertFalse(detail.record.isBackdated)

        // 空间 totalGp = 记录集合之和（D7 唯一真相来源），缓存列同步
        val space = db.spaceDao().getDefaultSpace()!!
        assertEquals(10, db.recordDao().sumAllGp(space.localId))
        assertEquals(10, db.spaceDao().getTotalGp(space.localId))
        assertEquals(10, db.recordDao().sumGpOnDate(space.localId, todayKey))

        // first_record 解锁 + UNLOCKED 事件
        val progress = db.achievementDao().getProgress("first_record", "u:1")
        assertNotNull(progress)
        assertTrue(progress!!.isUnlocked)
        assertEquals(1, db.achievementDao().countEvents("first_record", "u:1"))

        // Outbox：一条 RECORD UPSERT 待同步
        assertEquals(1, db.outboxDao().observePendingCount().first())

        // T18：五条不变量全链成立
        com.xiaoquexing.app.data.RecomputeService.assertInvariants(db)
    }

    @Test
    fun `发布带照片与标签的记录拆出媒体与交叉引用行`() = runBlocking {
        val result = repo.publish(
            testRecord(
                text = "拍了花",
                photoUris = "content://media/1.jpg|content://media/2.jpg",
                statusTags = "自然, 美食 ,自然,",
                locationName = "公园",
                moodTag = "开心"
            )
        )
        val detail = db.recordDao().getRecordDetailById(result.recordId)!!

        assertEquals(3, detail.media.size) // 2 PHOTO + 1 LOCATION
        assertEquals(2, detail.media.count { it.type == "PHOTO" })
        // content:// 复制落盘前是 PENDING_COPY（K5：渲染走副本，原始 URI 仅溯源）
        assertTrue(detail.media.filter { it.type == "PHOTO" }.all { it.mediaStatus == "PENDING_COPY" })
        // 标签去重：自然+美食 -> 2 个 cross ref（且命中 STATUS 注册表）
        assertEquals(2, detail.tags.size)
        assertTrue(detail.tags.all { it.kind == "STATUS" })

        // 摄影师口径 = 照片张数 2（K7），不同地点数 = 1
        assertEquals(2, db.recordDao().countPhotos(1L))
        assertEquals(1, db.recordDao().countDistinctLocations(1L))
    }

    @Test
    fun `同日连发受剩余额度封顶且当日总入账恰好100`() = runBlocking {
        // 内容分：基础10+文字5+照片3张9+地点3+状态2 = 29；N=1 ×1.05 -> 30
        val draft = testRecord(moodTag = "开心", text = "a", photoUris = "p1|p2|p3", locationName = "公园", statusTags = "自然")

        repeat(3) { repo.publish(draft) } // 30×3 = 90
        val fourth = repo.publish(draft)  // 剩余 10 -> raw 30 被截到 10

        assertTrue(fourth.isCapped)
        assertEquals(10, fourth.earnedGp)

        val space = db.spaceDao().getDefaultSpace()!!
        assertEquals(100, db.recordDao().sumGpOnDate(space.localId, todayKey))
        assertEquals(100, db.recordDao().sumAllGp(space.localId))
    }

    @Test
    fun `无心情的记录被拒绝且不留半写状态`() = runBlocking {
        val before = db.recordDao().observeTotalCountAll().first()
        try {
            repo.publish(testRecord(moodTag = null, text = "没有心情"))
            fail("无心情应抛 IllegalArgumentException（ADR 原则 1）")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(before, db.recordDao().observeTotalCountAll().first())
        assertEquals(0, db.outboxDao().observePendingCount().first())
    }

    @Test
    fun `软删除后GP额度回退且成就回锁`() = runBlocking {
        val r = repo.publish(testRecord(moodTag = "开心", text = "唯一一条"))
        assertTrue(db.achievementDao().getProgress("first_record", "u:1")!!.isUnlocked)

        repo.softDelete(r.recordId)

        // 记录变墓碑，所有统计排除（D6）
        assertNull(db.recordDao().getRecordDetailById(r.recordId))
        val space = db.spaceDao().getDefaultSpace()!!
        assertEquals(0, db.recordDao().sumAllGp(space.localId))
        assertEquals(0, db.spaceDao().getTotalGp(space.localId))
        assertEquals(0, db.recordDao().sumGpOnDate(space.localId, todayKey))

        // 条件不再满足 -> 回锁且进度归零（D6.4）
        val progress = db.achievementDao().getProgress("first_record", "u:1")!!
        assertFalse(progress.isUnlocked)
        assertEquals(0, progress.progress)

        com.xiaoquexing.app.data.RecomputeService.assertInvariants(db)
    }

    @Test
    fun `跨过50GP写STAGE_UP快照删除后降级写STAGE_DOWN`() = runBlocking {
        // 每条：基础10+文字5+9照片27 = 42 ×1.05 -> 44
        val r1 = repo.publish(testRecord(moodTag = "开心", text = "1", photoUris = "p1|p2|p3|p4|p5|p6|p7|p8|p9"))
        val r2 = repo.publish(testRecord(moodTag = "平静", text = "2", photoUris = "p1|p2|p3|p4|p5|p6|p7|p8|p9"))

        val space = db.spaceDao().getDefaultSpace()!!
        assertEquals(88, db.recordDao().sumAllGp(space.localId)) // 44×2 跨过 50

        val up = db.plantDao().getLatestSnapshot(space.localId)
        assertEquals("STAGE_UP", up?.eventType)
        assertEquals(1, up?.stage) // 发芽 [50,200)

        repo.softDelete(r2.recordId)
        assertEquals("STAGE_DOWN", db.plantDao().getLatestSnapshot(space.localId)?.eventType)
        // r1 仍在，GP 回到 44（种子期 <50）
        assertEquals(44, db.recordDao().sumAllGp(space.localId))
        assertEquals(1, db.recordDao().countRecords(space.localId))
        assertTrue(r1.recordId > 0)
    }
}
