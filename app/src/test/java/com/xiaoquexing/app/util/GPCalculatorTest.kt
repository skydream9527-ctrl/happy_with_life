package com.xiaoquexing.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GP 公式测试（ADR-001 D1 冻结口径，Z1-03）。
 *
 * 基线：基础 10；文字 +5 / >50 字再 +5；照片 +3/张（≤9）；语音/音乐/链接 +5、地点 +3；
 * 状态标签 +2/个（≤3）；心情必选不加分；连续系数 1+N×0.05（第 20 天 ×2 封顶）；
 * 补记 ×0.8；先连续后补记、每步向下取整；最终受 (空间, 发生日) 剩余额度封顶。
 */
class GPCalculatorTest {

    private fun calc(
        textLength: Int = 0,
        photoCount: Int = 0,
        hasVoice: Boolean = false,
        hasMusic: Boolean = false,
        hasLink: Boolean = false,
        hasLocation: Boolean = false,
        hasMood: Boolean = true,
        statusTagCount: Int = 0,
        streakDays: Int = 1,
        isBackdated: Boolean = false,
        remainingQuota: Int = GPCalculator.DAILY_GP_LIMIT,
        specialEventBonus: Int = 0
    ) = GPCalculator.calculate(
        textLength, photoCount, hasVoice, hasMusic, hasLink, hasLocation,
        hasMood, statusTagCount, streakDays, isBackdated, remainingQuota, specialEventBonus
    )

    // ---- 基础（心情必选、不加分） ----

    @Test
    fun `仅心情的最小记录只得基础10`() {
        // 10×1.05=10.5 → floor 10
        assertEquals(10, calc().finalGp)
    }

    @Test
    fun `无心情参数不改变分数（发布前置校验在仓储层）`() {
        assertEquals(10, calc(hasMood = false).finalGp)
    }

    // ---- 文字两档（+5 / >50 字 +10） ----

    @Test
    fun `有文字加5`() {
        assertEquals(15, calc(textLength = 1).finalGp) // 15×1.05=15.75→15
    }

    @Test
    fun `50字仍是第一档`() {
        assertEquals(15, calc(textLength = 50).finalGp)
    }

    @Test
    fun `超过50字升到加10`() {
        assertEquals(21, calc(textLength = 51).finalGp) // 20×1.05=21
    }

    // ---- 照片 +3/张、封顶 9 张 ----

    @Test
    fun `三张照片加9`() {
        assertEquals(19, calc(photoCount = 3).finalGp) // 19×1.05=19.95→19
    }

    @Test
    fun `照片张数封顶9张`() {
        val capped = calc(photoCount = 12)
        assertEquals(38, capped.finalGp) // (10+27)×1.05=38.85→38
        assertEquals(9, capped.photoCount)
        assertEquals(27, capped.photoBonus)
    }

    // ---- 连续系数：第 20 天翻倍封顶（D1，替换旧实现 7 天翻倍） ----

    @Test
    fun `连续7天系数1_35`() {
        assertEquals(13, calc(streakDays = 7).finalGp) // 10×1.35=13.5→13
    }

    @Test
    fun `连续20天恰好翻倍`() {
        val b = calc(streakDays = 20)
        assertEquals(20, b.finalGp)
        assertEquals(2.0f, b.streakMultiplier)
    }

    @Test
    fun `连续30天仍为2倍封顶`() {
        assertEquals(20, calc(streakDays = 30).finalGp)
    }

    // ---- 补记 ×0.8：先连续系数后补记、每步取整 ----

    @Test
    fun `补记整单乘0_8`() {
        val backdated = calc(isBackdated = true) // 10 →×0.8 = 8
        assertEquals(8, backdated.finalGp)
        assertEquals(0.8f, backdated.backdateMultiplier)
    }

    @Test
    fun `补记作用于连续系数之后`() {
        // 71×1.05=74.55→74；74×0.8=59.2→59
        assertEquals(
            59,
            calc(
                textLength = 51, photoCount = 9, hasVoice = true, hasMusic = true,
                hasLink = true, hasLocation = true, statusTagCount = 5, isBackdated = true
            ).finalGp
        )
    }

    // ---- 每日额度（空间+发生日剩余额度封顶） ----

    @Test
    fun `超丰富记录被每日100额度封顶`() {
        val rich = calc(
            textLength = 51, photoCount = 9, hasVoice = true, hasMusic = true,
            hasLink = true, hasLocation = true, statusTagCount = 5, streakDays = 20
        )
        assertEquals(142, rich.rawTotal) // 71×2
        assertEquals(100, rich.finalGp)
        assertTrue(rich.isCapped)
    }

    @Test
    fun `剩余额度不足时截断到剩余值`() {
        val capped = calc(streakDays = 20, remainingQuota = 10) // raw=20
        assertEquals(10, capped.finalGp)
        assertTrue(capped.isCapped)
    }

    @Test
    fun `当日额度耗尽时得0分`() {
        val zero = calc(streakDays = 20, remainingQuota = 0)
        assertEquals(0, zero.finalGp)
        assertTrue(zero.isCapped)
    }

    @Test
    fun `未触及额度时不算封顶`() {
        assertFalse(calc().isCapped)
    }

    // ---- 特殊事件加分（共振/节日，v1 由参数注入） ----

    @Test
    fun `特殊事件加在乘法之后且参与封顶`() {
        assertEquals(25, calc(specialEventBonus = 15).finalGp) // 10+15
        val capped = calc(streakDays = 20, specialEventBonus = 15) // 20×2? no: 10×2=20, +15=35
        assertEquals(35, capped.finalGp)
        assertFalse(capped.isCapped)
    }
}
