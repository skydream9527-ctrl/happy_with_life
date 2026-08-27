package com.xiaoquexing.app.data.entity

/**
 * 领域层植物状态（v1 兼容形状）。
 * v2 起 totalGp 只对「活动植物」有意义（= 空间总分，ADR D7）；植物表不再存 GP。
 */
data class PlantState(
    val id: Long = 0,
    val plantType: PlantType,
    val totalGp: Int = 0,
    val isActive: Boolean = false,
    val isUnlocked: Boolean = false,
    val plantedAt: Long = System.currentTimeMillis(),
    val lastWateredAt: Long? = null
)
