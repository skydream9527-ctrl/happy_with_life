package com.xiaoquexing.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xiaoquexing.app.data.DataBootstrap
import com.xiaoquexing.app.data.RecomputeService
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.fixtures.testRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * 编辑与补记事务测试（Z1-04 / room-v2-schema T13、§6 editRecord 用例）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordRepositoryEditTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RecordRepository
    private val zone: ZoneId = ZoneId.systemDefault()

    private val todayKey: Int get() = LocalDate.now().toEpochDay().toInt()
    private val yesterdayNoon: Long
        get() = LocalDate.now().minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

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
    fun `编辑内容后重算GP并保持不变量`() = runBlocking {
        // 文字1字+5、照片1张+3 => 18×1.05 = 18
        val r = repo.publish(testRecord(moodTag = "开心", text = "a", photoUris = "p1"))
        assertEquals(18, db.recordDao().getRawById(r.recordId)!!.gpFinal)

        // 编辑为 3 张照片：10+5+9=24×1.05 = 25
        val edited = repo.editRecord(
            r.recordId,
            testRecord(moodTag = "平静", text = "a", photoUris = "p1|p2|p3")
        )
        val raw = db.recordDao().getRawById(r.recordId)!!
        assertEquals(25, raw.gpFinal)
        assertEquals("平静", raw.moodTag)
        assertEquals(25, edited.earnedGp)

        // 媒体重写：3 行 PHOTO；成就重估（record_count 不变）
        assertEquals(3, db.mediaDao().forRecord(r.recordId).count { it.type == "PHOTO" })
        assertEquals(25, db.recordDao().sumAllGp(1L))
        RecomputeService.assertInvariants(db)
    }

    @Test
    fun `编辑把记录移到昨天释放今天额度并触发补记衰减`() = runBlocking {
        // 今天发布：subtotal 29 -> 30
        val r = repo.publish(
            testRecord(moodTag = "开心", text = "a", photoUris = "p1|p2|p3", locationName = "公园", statusTags = "自然")
        )
        assertEquals(30, db.recordDao().sumGpOnDate(1L, todayKey))

        // 移到昨天：相对创建当日（今天）构成补记 -> ×0.8：floor(30×0.8)=24
        repo.editRecord(
            r.recordId,
            testRecord(
                moodTag = "开心", text = "a", photoUris = "p1|p2|p3",
                locationName = "公园", statusTags = "自然", createdAt = yesterdayNoon
            )
        )

        val raw = db.recordDao().getRawById(r.recordId)!!
        assertEquals(24, raw.gpFinal)
        assertTrue(raw.isBackdated)
        // D3/D4：补记占补记日额度，今天额度归零
        assertEquals(0, db.recordDao().sumGpOnDate(1L, todayKey))
        assertEquals(24, db.recordDao().sumGpOnDate(1L, todayKey - 1))
        assertEquals(24, db.recordDao().sumAllGp(1L))
        RecomputeService.assertInvariants(db)
    }

    @Test
    fun `补记发布占用补记日额度不占今天额度`() = runBlocking {
        // T13 口径：直接补记昨天
        val r = repo.publish(testRecord(moodTag = "开心", text = "补昨天的落日", createdAt = yesterdayNoon))
        // 15×1.05=15.75->15；补记 ×0.8 -> floor(15×0.8)=12
        assertEquals(12, db.recordDao().getRawById(r.recordId)!!.gpFinal)

        assertEquals(0, db.recordDao().sumGpOnDate(1L, todayKey))
        assertEquals(12, db.recordDao().sumGpOnDate(1L, todayKey - 1))
        RecomputeService.assertInvariants(db)
    }

    @Test
    fun `编辑可接续断裂的连续天数`() = runBlocking {
        // 前天与今天各一条，昨天缺失 -> streak=1
        val dayBefore = LocalDate.now().minusDays(2).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        repo.publish(testRecord(moodTag = "开心", text = "前天", createdAt = dayBefore))
        val today = repo.publish(testRecord(moodTag = "平静", text = "今天"))
        assertEquals(1, repo.calculateStreakDays())

        // 把今天的记录改到昨天 -> 昨天有、今天无：宽限口径从昨天起算，
        // 昨天+前天连续 -> streak=2（D2.3 宽限起点）
        repo.editRecord(
            today.recordId,
            testRecord(moodTag = "平静", text = "今天", createdAt = yesterdayNoon)
        )
        assertEquals(2, repo.calculateStreakDays())
    }

    @Test
    fun `超过365天窗口的补记被拒绝`() = runBlocking {
        val tooOld = LocalDate.now().minusDays(400).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        try {
            repo.publish(testRecord(moodTag = "开心", text = "太久了", createdAt = tooOld))
            fail("超过 365 天窗口应被拒绝（ADR D4.1）")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(0, db.recordDao().countRecords(1L))
    }

    @Test
    fun `未来时间被拒绝`() = runBlocking {
        try {
            repo.publish(testRecord(moodTag = "开心", text = "来自未来", createdAt = System.currentTimeMillis() + 3600_000))
            fail("未来时间应被拒绝（ADR D4.1）")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `编辑已删除的记录被拒绝`() = runBlocking {
        val r = repo.publish(testRecord(moodTag = "开心", text = "将删除"))
        repo.softDelete(r.recordId)
        try {
            repo.editRecord(r.recordId, testRecord(moodTag = "开心", text = "复活"))
            fail("墓碑记录不可编辑（ADR D6.7）")
        } catch (_: IllegalArgumentException) {
        }
    }
}
