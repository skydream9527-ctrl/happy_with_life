package com.xiaoquexing.app.data.entity

/** 领域层成就（v1 兼容形状）。定义与进度在 v2 中拆为三张表，由仓储层联查拼装。 */
data class Achievement(
    val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val emoji: String = "🏆",
    val requirement: Int = 0,
    val progress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
