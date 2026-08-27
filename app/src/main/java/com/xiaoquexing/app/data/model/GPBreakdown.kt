package com.xiaoquexing.app.data.model

data class GPBreakdown(
    val baseGp: Int = 10,
    val textBonus: Int = 0,
    val photoBonus: Int = 0,
    val photoCount: Int = 0,
    val voiceBonus: Int = 0,
    val musicBonus: Int = 0,
    val linkBonus: Int = 0,
    val locationBonus: Int = 0,
    val moodBonus: Int = 0,
    val statusBonus: Int = 0,
    val statusCount: Int = 0,
    val streakMultiplier: Float = 1f,
    val streakDays: Int = 0,
    val isBackdated: Boolean = false,
    val backdateMultiplier: Float = 1f,
    val rawTotal: Int = 0,
    val finalGp: Int = 0,
    val isCapped: Boolean = false,
    val dailyLimit: Int = 100
)
