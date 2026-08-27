package com.xiaoquexing.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** albums 表（§2.8，I3 预备）。 */
@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["space_id", "created_at"]),
        Index(value = ["server_id"], unique = true)
    ]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "theme") val theme: String = "fresh_spring",
    /** ALL / STAGE / DATE */
    @ColumnInfo(name = "range_type") val rangeType: String = "ALL",
    @ColumnInfo(name = "stage_start") val stageStart: Int? = null,
    @ColumnInfo(name = "stage_end") val stageEnd: Int? = null,
    @ColumnInfo(name = "date_start") val dateStart: Int? = null,
    @ColumnInfo(name = "date_end") val dateEnd: Int? = null,
    @ColumnInfo(name = "entry_count") val entryCount: Int = 0,
    @ColumnInfo(name = "page_count") val pageCount: Int = 0,
    @ColumnInfo(name = "layout_seed") val layoutSeed: Long = 0,
    @ColumnInfo(name = "entry_hash") val entryHash: String,
    @ColumnInfo(name = "cover_local_path") val coverLocalPath: String? = null,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** album_pages 表（§2.8）。payload_json 由 Z code 生成，MiniMax 只渲染。 */
@Entity(
    tableName = "album_pages",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["album_id", "page_index"], unique = true)]
)
data class AlbumPageEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "page_index") val pageIndex: Int,
    /** COVER / GROWTH_TIMELINE / MOOD / TAG / MAP / BGM / LINK / MONTHLY / BACK_COVER */
    @ColumnInfo(name = "page_type") val pageType: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** daily_space_stats 表（§2.6）：额度缓存，可随时从 records 全量重建。 */
@Entity(
    tableName = "daily_space_stats",
    primaryKeys = ["space_id", "date_key"],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DailySpaceStatEntity(
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "date_key") val dateKey: Int,
    @ColumnInfo(name = "gp_total") val gpTotal: Int = 0,
    @ColumnInfo(name = "record_count") val recordCount: Int = 0,
    @ColumnInfo(name = "distinct_author_count") val distinctAuthorCount: Int = 0
)

/**
 * outbox_events 表（§2.9，M4 同步扩展位）。
 * 无外键是刻意的：Outbox 必须独立于实体物理生命周期，避免级联吞事件。
 */
@Entity(
    tableName = "outbox_events",
    indices = [Index(value = ["state", "next_retry_at"])]
)
data class OutboxEventEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_local_id") val entityLocalId: Long,
    /** OutboxOps 常量 */
    @ColumnInfo(name = "operation") val operation: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String? = null,
    /** OutboxStates 常量 */
    @ColumnInfo(name = "state") val state: String = OutboxStates.PENDING,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
