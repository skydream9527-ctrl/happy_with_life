package com.xiaoquexing.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * plants 表（§2.5）：用户级植物目录 + 解锁状态。
 * 解锁条件类型化（ADR D9），植物实例不持有 GP（D7）。
 */
@Entity(tableName = "plants")
data class PlantDefEntity(
    @PrimaryKey @ColumnInfo(name = "plant_type") val plantType: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    /** ConditionTypes 常量 */
    @ColumnInfo(name = "condition_type") val conditionType: String,
    @ColumnInfo(name = "condition_param") val conditionParam: Int,
    @ColumnInfo(name = "condition_sub_param") val conditionSubParam: String? = null,
    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** space_plants 表（§2.5）：空间内的植物实例——「载体」。每空间至多一棵活动植物。 */
@Entity(
    tableName = "space_plants",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlantDefEntity::class,
            parentColumns = ["plant_type"],
            childColumns = ["plant_type"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["space_id"])]
)
data class SpacePlantEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "plant_type") val plantType: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** plant_snapshots 表（§2.5，append-only）：图鉴历史 + 画册生长时间轴。 */
@Entity(
    tableName = "plant_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["space_id", "occurred_at"])]
)
data class PlantSnapshotEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "plant_type") val plantType: String,
    /** PlantEventTypes 常量 */
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "stage") val stage: Int,
    @ColumnInfo(name = "gp_at_event") val gpAtEvent: Int,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "occurred_date_key") val occurredDateKey: Int,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)
