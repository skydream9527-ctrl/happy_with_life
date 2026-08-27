package com.xiaoquexing.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: SpaceType = SpaceType.PERSONAL,
    val memberCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SpaceType {
    PERSONAL, SHARED
}
