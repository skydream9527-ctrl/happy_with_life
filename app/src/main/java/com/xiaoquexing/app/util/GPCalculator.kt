package com.xiaoquexing.app.util

import com.xiaoquexing.app.data.model.GPBreakdown
import java.util.Calendar
import kotlin.math.min

object GPCalculator {
    private const val BASE_GP = 10
    private const val TEXT_20_BONUS = 10
    private const val TEXT_50_BONUS = 15
    private const val TEXT_200_BONUS = 20
    private const val PHOTO_BONUS_PER = 8
    private const val PHOTO_MAX = 9
    private const val VOICE_BONUS = 8
    private const val MUSIC_BONUS = 5
    private const val LINK_BONUS = 5
    private const val LOCATION_BONUS = 5
    private const val MOOD_BONUS = 3
    private const val STATUS_BONUS_PER = 2
    private const val STATUS_MAX = 3
    private const val DAILY_LIMIT = 100
    private const val BACKDATE_MULTIPLIER = 0.8f

    fun calculate(
        textLength: Int,
        photoCount: Int,
        hasVoice: Boolean,
        hasMusic: Boolean,
        hasLink: Boolean,
        hasLocation: Boolean,
        hasMood: Boolean,
        statusTagCount: Int,
        streakDays: Int,
        isBackdated: Boolean,
        todayGpSoFar: Int = 0
    ): GPBreakdown {
        var textBonus = 0
        when {
            textLength >= 200 -> textBonus = TEXT_200_BONUS
            textLength >= 50 -> textBonus = TEXT_50_BONUS
            textLength >= 20 -> textBonus = TEXT_20_BONUS
        }

        val actualPhotos = min(photoCount, PHOTO_MAX)
        val photoBonus = actualPhotos * PHOTO_BONUS_PER

        val voiceBonus = if (hasVoice) VOICE_BONUS else 0
        val musicBonus = if (hasMusic) MUSIC_BONUS else 0
        val linkBonus = if (hasLink) LINK_BONUS else 0
        val locationBonus = if (hasLocation) LOCATION_BONUS else 0
        val moodBonus = if (hasMood) MOOD_BONUS else 0

        val actualStatusTags = min(statusTagCount, STATUS_MAX)
        val statusBonus = actualStatusTags * STATUS_BONUS_PER

        val streakMultiplier = min(streakDays.toFloat() / 7f, 1f) + 1f

        val backdateMultiplier = if (isBackdated) BACKDATE_MULTIPLIER else 1f

        val subtotal = BASE_GP + textBonus + photoBonus + voiceBonus + musicBonus +
                linkBonus + locationBonus + moodBonus + statusBonus

        var withStreak = (subtotal * streakMultiplier).toInt()
        withStreak = (withStreak * backdateMultiplier).toInt()

        val remaining = DAILY_LIMIT - todayGpSoFar
        val isCapped = withStreak > remaining
        val finalGp = min(withStreak, remaining).coerceAtLeast(0)

        return GPBreakdown(
            baseGp = BASE_GP,
            textBonus = textBonus,
            photoBonus = photoBonus,
            photoCount = actualPhotos,
            voiceBonus = voiceBonus,
            musicBonus = musicBonus,
            linkBonus = linkBonus,
            locationBonus = locationBonus,
            moodBonus = moodBonus,
            statusBonus = statusBonus,
            statusCount = actualStatusTags,
            streakMultiplier = streakMultiplier,
            streakDays = streakDays,
            isBackdated = isBackdated,
            backdateMultiplier = backdateMultiplier,
            rawTotal = withStreak,
            finalGp = finalGp,
            isCapped = isCapped,
            dailyLimit = DAILY_LIMIT
        )
    }

    fun calculateStreak(recordTimestamps: List<Long>): Int {
        if (recordTimestamps.isEmpty()) return 0

        val sorted = recordTimestamps.sortedDescending()
        val cal = Calendar.getInstance()
        var streak = 0

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000

        var checkDay = if (sorted.first() >= todayStart) todayStart else yesterdayStart

        for (ts in sorted) {
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = ts
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (dayStart == checkDay) {
                streak++
                checkDay -= 24 * 60 * 60 * 1000
            } else if (dayStart < checkDay) {
                break
            }
        }
        return streak
    }

    fun isBackdated(createdAt: Long): Boolean {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return createdAt < todayStart
    }

    fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        return getStartOfDay(timestamp) + 24 * 60 * 60 * 1000
    }
}
