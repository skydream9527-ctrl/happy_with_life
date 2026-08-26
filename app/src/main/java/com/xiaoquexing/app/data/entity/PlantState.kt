package com.xiaoquexing.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_states")
data class PlantState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantType: PlantType,
    val totalGp: Int = 0,
    val isActive: Boolean = false,
    val isUnlocked: Boolean = false,
    val plantedAt: Long = System.currentTimeMillis(),
    val lastWateredAt: Long? = null
)
