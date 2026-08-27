package com.xiaoquexing.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** achievement_definitions 表（§2.7）：产品内容，随版本分发。 */
@Entity(tableName = "achievement_definitions")
data class AchievementDefEntity(
    @PrimaryKey @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    /** MILESTONE / EXPLORATION / SEASONAL */
    @ColumnInfo(name = "category") val category: String = "MILESTONE",
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
    /** ConditionTypes 常量 */
    @ColumnInfo(name = "condition_type") val conditionType: String,
    @ColumnInfo(name = "condition_param") val conditionParam: Int,
    @ColumnInfo(name = "condition_sub_param") val conditionSubParam: String? = null,
    /** SKIN / BADGE / PLANT / EMOTE；无 GP 奖励（ADR D7.4 / Q2） */
    @ColumnInfo(name = "reward_type") val rewardType: String? = null,
    @ColumnInfo(name = "reward_value") val rewardValue: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** achievement_progress 表（§2.7）。scope_key = "u:{userId}" / "s:{spaceId}"。 */
@Entity(
    tableName = "achievement_progress",
    foreignKeys = [
        ForeignKey(
            entity = AchievementDefEntity::class,
            parentColumns = ["code"],
            childColumns = ["definition_code"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["definition_code", "scope_key"], unique = true)]
)
data class AchievementProgressEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "definition_code") val definitionCode: String,
    @ColumnInfo(name = "scope_key") val scopeKey: String,
    @ColumnInfo(name = "progress") val progress: Int = 0,
    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null,
    @ColumnInfo(name = "last_evaluated_at") val lastEvaluatedAt: Long = 0,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** achievement_events 表（§2.7，append-only）：解锁动画重放 / 审计 / 服务端校准。 */
@Entity(
    tableName = "achievement_events",
    foreignKeys = [
        ForeignKey(
            entity = AchievementDefEntity::class,
            parentColumns = ["code"],
            childColumns = ["definition_code"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["scope_key", "event_type"])]
)
data class AchievementEventEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "definition_code") val definitionCode: String,
    @ColumnInfo(name = "scope_key") val scopeKey: String,
    /** AchievementEventTypes 常量 */
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "progress_before") val progressBefore: Int,
    @ColumnInfo(name = "progress_after") val progressAfter: Int,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "reason_json") val reasonJson: String? = null,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)
