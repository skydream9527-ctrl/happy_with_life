package com.xiaoquexing.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连续天数测试（ADR-001 D2，Z1-03：epoch day 整数口径）。
 *
 * - 展示口径：今天有记录从今天数；没有但昨天有则从昨天数（宽限）；否则 0；
 * - 同日多条只算 1 天（集合语义）；补记过去某天可接续；
 * - streakEndingAt = 连续系数中的 N（含结束日在内，D2.5）。
 */
class StreakCalculatorTest {

    /** 以 today=10000 为固定“今天”，避免测试跨零点 */
    private val today = 10_000

    private fun streakOf(vararg days: Int): Int =
        StreakCalculator.calculateFromKeys(days.toSet(), today)

    @Test
    fun `无任何记录时为0`() {
        assertEquals(0, streakOf())
    }

    @Test
    fun `仅今天有记录时为1`() {
        assertEquals(1, streakOf(today))
    }

    @Test
    fun `今天没有但昨天有记录时宽限起算为1`() {
        assertEquals(1, streakOf(today - 1))
    }

    @Test
    fun `今天和昨天都有记录时为2`() {
        assertEquals(2, streakOf(today, today - 1))
    }

    @Test
    fun `连续5天每天一条记录时为5`() {
        assertEquals(5, streakOf(*(0..4).map { today - it }.toIntArray()))
    }

    @Test
    fun `昨天断档则从今天重新计数`() {
        assertEquals(1, streakOf(today, today - 2))
    }

    @Test
    fun `同一天多条记录只算一天（集合去重）`() {
        assertEquals(1, streakOf(today))
    }

    @Test
    fun `今天昨天都没有记录时为0即使更早连续`() {
        assertEquals(0, streakOf(today - 3, today - 4, today - 5))
    }

    @Test
    fun `补记接续断档的连续天数`() {
        // 已有今天和前天，补记昨天后 streak 接上 = 3（D2.4 推导式）
        assertEquals(3, streakOf(today, today - 1, today - 2))
    }

    // ---- streakEndingAt：连续系数 N 口径 ----

    @Test
    fun `N包含结束日在内`() {
        assertEquals(1, StreakCalculator.streakEndingAt(setOf(100), 100))
        assertEquals(20, StreakCalculator.streakEndingAt((81..100).toSet(), 100))
    }

    @Test
    fun `结束日无记录时为0`() {
        assertEquals(0, StreakCalculator.streakEndingAt(setOf(99), 100))
    }

    @Test
    fun `epoch day 整数加减不受 DST 影响`() {
        // 无论时区如何变化，dateKey 只会整体平移，连续性判断不变（D2.6）
        val keys = (today - 700..today).toSet()
        assertEquals(701, StreakCalculator.streakEndingAt(keys, today))
    }
}
