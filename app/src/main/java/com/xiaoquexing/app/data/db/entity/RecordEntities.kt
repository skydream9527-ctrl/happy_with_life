package com.xiaoquexing.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * records 表（room-v2-schema §2.1）。
 *
 * 领域语义见 ADR-001：GP 属于空间（space_id），occurred_date_key 是每日额度与
 * 统计的唯一日期口径，mood_tag 非空（心情必选）。
 */
@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["author_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["space_id", "occurred_date_key"]),
        Index(value = ["space_id", "occurred_at"]),
        Index(value = ["author_id", "occurred_at"]),
        Index(value = ["sync_state"]),
        Index(value = ["server_id"], unique = true)
    ]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "author_id") val authorId: Long,
    @ColumnInfo(name = "content_text") val contentText: String? = null,
    @ColumnInfo(name = "mood_tag") val moodTag: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    /** 发生日 epoch day（本地时区），额度/分组/streak 口径（ADR D2/D3） */
    @ColumnInfo(name = "occurred_date_key") val occurredDateKey: Int,
    @ColumnInfo(name = "is_backdated") val isBackdated: Boolean = false,
    @ColumnInfo(name = "gp_final") val gpFinal: Int = 0,
    @ColumnInfo(name = "gp_breakdown_json") val gpBreakdownJson: String? = null,
    @ColumnInfo(name = "is_capped") val isCapped: Boolean = false,
    // ---- 同步列（§1.2）----
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/**
 * record_media 表（§2.2）：照片/录音/音乐/链接/地点全部正规化到行。
 * local_path 是渲染唯一来源；content:// 原始地址只存 source_uri 溯源。
 */
@Entity(
    tableName = "record_media",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["record_id"]),
        Index(value = ["type"]),
        Index(value = ["server_id"], unique = true)
    ]
)
data class RecordMediaEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "record_id") val recordId: Long,
    /** MediaTypes 常量 */
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "local_path") val localPath: String? = null,
    @ColumnInfo(name = "source_uri") val sourceUri: String? = null,
    @ColumnInfo(name = "remote_uri") val remoteUri: String? = null,
    /** MediaStatus 常量 */
    @ColumnInfo(name = "media_status") val mediaStatus: String = MediaStatus.PENDING_COPY,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "width") val width: Int? = null,
    @ColumnInfo(name = "height") val height: Int? = null,
    /** LOCATION=地点名；MUSIC=歌名；LINK=标题。DISTINCT_LOCATION_COUNT 按 title 去重 */
    @ColumnInfo(name = "title") val title: String? = null,
    /** MUSIC=歌手；LOCATION=地址 */
    @ColumnInfo(name = "subtitle") val subtitle: String? = null,
    /** LINK 摘要/平台、MUSIC 专辑、LOCATION lat/lng、LINK OG 数据 */
    @ColumnInfo(name = "extra_json") val extraJson: String? = null,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** record_tag_cross_ref（§2.3） */
@Entity(
    tableName = "record_tag_cross_ref",
    primaryKeys = ["record_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["record_id"]), Index(value = ["tag_id"])]
)
data class RecordTagCrossRef(
    @ColumnInfo(name = "record_id") val recordId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)

/** tags 表（§2.3）。space_id=0 是 USER 作用域哨兵（故意的，不加外键） */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["scope", "space_id", "kind", "name"], unique = true),
        Index(value = ["server_id"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    /** TagScopes 常量 */
    @ColumnInfo(name = "scope") val scope: String = TagScopes.USER,
    @ColumnInfo(name = "space_id") val spaceId: Long = 0,
    /** TagKinds 常量 */
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String = "",
    @ColumnInfo(name = "color") val color: String? = null,
    @ColumnInfo(name = "use_count") val useCount: Int = 0,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)
