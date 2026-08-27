package com.xiaoquexing.app.util

import com.xiaoquexing.app.data.db.dao.RecordDao
import java.util.Calendar
import java.util.TimeZone

/**
 * 连续记录天数计算器。
 *
 * 规则：
 * - 从今天往回数（基于设备本地时区），如果今天已记录 → streak = 1 开始递增；
 *   如果今天没记录但昨天有 → 从昨天起算（很多用户睡前补录，这里宽容一天）；
 *   如果今天和昨天都没记录 → streak = 0。
 * - 单日多次记录只算 1 天。
 * - 上限 365，避免异常长尾。
 *
 * 实现：用 [RecordDao.hasRecordsOnDay] 一天一天往前查。N 较大时（>100）成本可接受，
 * 因为 streak 在用户每天进 App 时只算一次。如果以后记录数破万想做性能优化，
 * 可以用一条 SQL 取 distinct day 列表：
 *   SELECT DISTINCT date(createdAt/86400000, 'unixepoch', 'localtime') AS day
 * 然后在内存里连续比较。这里保留一天天查是因为实现最简单且错误面最小。
 */
object StreakCalculator {

    private const val MAX_STREAK = 365
    private val DAY_MS: Long = 24L * 60L * 60L * 1000L

    suspend fun calculate(dao: RecordDao, now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = now
        val todayStart = startOfDay(cal)
        val yesterdayStart = todayStart - DAY_MS

        // 起点：今天有记录就从今天算；今天没有但昨天有就从昨天算；都没有返回 0
        val startDay = when {
            hasRecord(dao, todayStart) -> todayStart
            hasRecord(dao, yesterdayStart) -> yesterdayStart
            else -> return 0
        }

        var streak = 0
        var cursor = startDay
        // 防止极端情况下死循环
        while (streak < MAX_STREAK) {
            if (!hasRecord(dao, cursor)) break
            streak++
            cursor -= DAY_MS
        }
        return streak
    }

    private suspend fun hasRecord(dao: RecordDao, dayStart: Long): Boolean {
        return dao.hasRecordsOnDay(dayStart, dayStart + DAY_MS) > 0
    }

    private fun startOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
