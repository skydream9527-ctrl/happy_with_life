package com.xiaoquexing.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** spaces 表（§2.4）。total_gp 是缓存列，不变量 = 未删除记录 gp_final 之和（ADR D7）。 */
@Entity(
    tableName = "spaces",
    indices = [Index(value = ["server_id"], unique = true)]
)
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    /** PERSONAL / COUPLE / FAMILY / FRIEND（沿用 SpaceType 枚举名） */
    @ColumnInfo(name = "space_type") val spaceType: String = "PERSONAL",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "total_gp") val totalGp: Int = 0,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** space_members 表（§2.4，D11 权限） */
@Entity(
    tableName = "space_members",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["space_id", "user_id"], unique = true)]
)
data class SpaceMemberEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    /** MemberRoles 常量 */
    @ColumnInfo(name = "role") val role: String = MemberRoles.MEMBER,
    @ColumnInfo(name = "joined_at") val joinedAt: Long,
    @ColumnInfo(name = "contributed_gp") val contributedGp: Int = 0,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)

/** users 表（§2.4）。v1 单机只有一行本地用户，M4 接账号后扩展。 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["server_id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "local_id") val localId: Long = 0,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "avatar_local_path") val avatarLocalPath: String? = null,
    @ColumnInfo(name = "premium_expire_at") val premiumExpireAt: Long? = null,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncStates.SYNCED,
    @ColumnInfo(name = "version") val version: Int = 0
)
