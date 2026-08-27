package com.xiaoquexing.app.util

import com.xiaoquexing.app.data.model.GPBreakdown
import kotlin.math.floor
import kotlin.math.min

/**
 * GP 计算规则（ADR-001 D1 冻结，以 PRD 3.2.1 为基线）。
 *
 * 公式与取整顺序：
 *   subtotal = 基础 10 + 内容加分（全整数）
 *   raw      = floor(subtotal × 连续系数)，连续系数 = min(1 + N×0.05, 2.0)（第 20 天封顶）
 *   补记     raw = floor(raw × 0.8)（occurredDate ≠ 创建当日，ADR D4）
 *   total    = raw + 特殊事件加分（v1 离线版恒为 0，常量见下）
 *   final    = min(total, remainingQuota)，下限 0；被截断时 isCapped = true
 *
 * 心情是发布前置条件而非加分项（D1/R4），hasMood 参数仅为调用方兼容保留。
 */
object GPCalculator {

    // ---- 冻结常量（ADR D1.2；修改必须走 ADR 修订）----
    const val BASE_GP = 10
    const val TEXT_BONUS = 5
    const val TEXT_LONG_BONUS = 5           // 文字 > 50 字再 +5（两档合计 +10）
    const val TEXT_LONG_THRESHOLD = 50
    const val PHOTO_BONUS_PER = 3
    const val PHOTO_MAX = 9
    const val VOICE_BONUS = 5
    const val MUSIC_BONUS = 5
    const val LINK_BONUS = 5
    const val LOCATION_BONUS = 3
    const val STATUS_BONUS_PER = 2
    const val STATUS_MAX = 3
    const val STREAK_RATE = 0.05            // 连续系数 1 + N×0.05
    const val STREAK_MULT_CAP = 2.0         // 第 20 天 ×2 封顶
    const val BACKDATE_MULTIPLIER = 0.8
    /** 补记窗口：最多往前 365 个自然日（ADR D4.1） */
    const val BACKDATE_MAX_DAYS = 365
    const val EVENT_ANNIVERSARY = 20        // M5 实现；v1 不触发
    const val EVENT_FESTIVAL = 10           // I2 实现；v1 不触发
    const val EVENT_RESONANCE = 15          // M5 实现；v1 不触发
    const val DAILY_GP_LIMIT = 100

    fun calculate(
        textLength: Int,
        photoCount: Int,
        hasVoice: Boolean,
        hasMusic: Boolean,
        hasLink: Boolean,
        hasLocation: Boolean,
        @Suppress("UNUSED_PARAMETER") hasMood: Boolean,
        statusTagCount: Int,
        streakDays: Int,
        isBackdated: Boolean,
        remainingQuota: Int = DAILY_GP_LIMIT,
        specialEventBonus: Int = 0
    ): GPBreakdown {
        val textBonus = when {
            textLength > TEXT_LONG_THRESHOLD -> TEXT_BONUS + TEXT_LONG_BONUS
            textLength > 0 -> TEXT_BONUS
            else -> 0
        }

        val actualPhotos = min(photoCount, PHOTO_MAX)
        val photoBonus = actualPhotos * PHOTO_BONUS_PER

        val voiceBonus = if (hasVoice) VOICE_BONUS else 0
        val musicBonus = if (hasMusic) MUSIC_BONUS else 0
        val linkBonus = if (hasLink) LINK_BONUS else 0
        val locationBonus = if (hasLocation) LOCATION_BONUS else 0

        val actualStatusTags = min(statusTagCount, STATUS_MAX)
        val statusBonus = actualStatusTags * STATUS_BONUS_PER

        val subtotal = BASE_GP + textBonus + photoBonus + voiceBonus + musicBonus +
            linkBonus + locationBonus + statusBonus

        // D1.1：先连续系数后补记衰减，每步向下取整
        val streakMultiplier = min(1.0 + streakDays * STREAK_RATE, STREAK_MULT_CAP)
        val afterStreak = floor(subtotal * streakMultiplier).toInt()
        val backdateMultiplier = if (isBackdated) BACKDATE_MULTIPLIER else 1.0
        val rawTotal = floor(afterStreak * backdateMultiplier).toInt()

        val total = rawTotal + specialEventBonus
        val isCapped = total > remainingQuota
        val finalGp = min(total, remainingQuota).coerceAtLeast(0)

        return GPBreakdown(
            baseGp = BASE_GP,
            textBonus = textBonus,
            photoBonus = photoBonus,
            photoCount = actualPhotos,
            voiceBonus = voiceBonus,
            musicBonus = musicBonus,
            linkBonus = linkBonus,
            locationBonus = locationBonus,
            statusBonus = statusBonus,
            statusCount = actualStatusTags,
            streakMultiplier = streakMultiplier.toFloat(),
            streakDays = streakDays,
            isBackdated = isBackdated,
            backdateMultiplier = backdateMultiplier.toFloat(),
            specialEventBonus = specialEventBonus,
            rawTotal = total,
            finalGp = finalGp,
            isCapped = isCapped,
            dailyLimit = DAILY_GP_LIMIT
        )
    }
}
