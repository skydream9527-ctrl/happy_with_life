package com.xiaoquexing.app.data.entity

/** 领域层空间（v1 兼容形状）。Room 实体是 data.db.entity.SpaceEntity。 */
data class Space(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: SpaceType = SpaceType.PERSONAL,
    val memberCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SpaceType {
    PERSONAL, SHARED
}
