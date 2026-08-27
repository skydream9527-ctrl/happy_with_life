package com.xiaoquexing.app.util

import com.xiaoquexing.app.data.db.dao.RecordDao
import java.time.LocalDate

/**
 * 连续记录天数（ADR-001 D2 冻结）。
 *
 * - 口径 = 空间内未删除记录的 occurred_date_key 去重集合（同日多条只算 1 天）；
 * - 展示口径（todayKey 起算）：今天有记录从今天数；没有但昨天有则从昨天数（宽限一天）；
 *   两天都没有 → 0；
 * - 补记过去某天会自然接续/修复 streak（推导式而非事件式，D2.4）；
 * - 全部基于 epoch day 的整数加减，无毫秒运算，天然 DST 安全（D2.6）。
 */
object StreakCalculator {

    suspend fun calculate(
        dao: RecordDao,
        spaceId: Long,
        todayKey: Int = LocalDate.now().toEpochDay().toInt()
    ): Int {
        val keys = dao.distinctDateKeys(spaceId).toHashSet()
        return calculateFromKeys(keys, todayKey)
    }

    fun calculateFromKeys(keys: Set<Int>, todayKey: Int): Int {
        val start = when {
            keys.contains(todayKey) -> todayKey
            keys.contains(todayKey - 1) -> todayKey - 1
            else -> return 0
        }
        return streakEndingAt(keys, start)
    }

    /** 连续系数中的 N：含结束日在内的连续天数（D2.5）。 */
    fun streakEndingAt(keys: Set<Int>, endDateKey: Int): Int {
        var streak = 0
        var cursor = endDateKey
        while (keys.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }
}
