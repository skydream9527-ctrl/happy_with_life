package com.xiaoquexing.app.data.repository

import androidx.room.withTransaction
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import com.xiaoquexing.app.data.db.entity.SpacePlantEntity
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.remote.ApiException
import com.xiaoquexing.app.data.remote.ApiService
import com.xiaoquexing.app.data.remote.InviteDto
import com.xiaoquexing.app.data.remote.MemberDto
import com.xiaoquexing.app.data.remote.SpaceDto
import kotlinx.coroutines.flow.Flow

class SpaceRepository(
    private val db: AppDatabase,
    private val api: ApiService,
) {
    fun observeSpaces(): Flow<List<SpaceEntity>> = db.spaceDao().observeAll()

    suspend fun refreshFromServer(): List<SpaceDto> {
        val env = api.spaces()
        env.error?.let { throw ApiException(it) }
        val items = env.data?.items.orEmpty()
        items.forEach { upsertLocal(it) }
        return items
    }

    suspend fun createShared(name: String, spaceType: String = "FRIEND"): SpaceDto {
        val env = api.createSpace(
            mapOf("name" to name.trim(), "spaceType" to spaceType, "plantType" to PlantType.TREE.name)
        )
        val dto = env.data ?: throw ApiException(env.error ?: com.xiaoquexing.app.data.remote.ApiError("SPACE", "创建失败"))
        upsertLocal(dto)
        return dto
    }

    suspend fun invite(serverSpaceId: String): InviteDto {
        val env = api.createInvite(serverSpaceId)
        return env.data ?: throw ApiException(env.error ?: com.xiaoquexing.app.data.remote.ApiError("SPACE", "邀请失败"))
    }

    suspend fun peek(token: String) = api.peekInvite(token.trim()).data

    suspend fun accept(token: String): SpaceDto {
        val env = api.acceptInvite(mapOf("token" to token.trim()))
        val dto = env.data ?: throw ApiException(env.error ?: com.xiaoquexing.app.data.remote.ApiError("SPACE", "接受邀请失败"))
        upsertLocal(dto)
        return dto
    }

    suspend fun members(serverSpaceId: String): List<MemberDto> {
        val env = api.members(serverSpaceId)
        env.error?.let { throw ApiException(it) }
        return env.data?.items.orEmpty()
    }

    suspend fun switchTo(localId: Long) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.spaceDao().clearDefault(now)
            db.spaceDao().setDefault(localId, now)
        }
    }

    private suspend fun upsertLocal(dto: SpaceDto) {
        val existing = db.spaceDao().findByServerId(dto.id)
        val now = System.currentTimeMillis()
        if (existing != null) return
        val localId = db.spaceDao().insert(
            SpaceEntity(
                name = dto.name,
                spaceType = dto.spaceType.ifBlank { "FRIEND" },
                isDefault = false,
                totalGp = dto.totalGp.toInt(),
                serverId = dto.id,
                createdAt = now,
                updatedAt = now,
            )
        )
        db.plantDao().insertSpacePlant(
            SpacePlantEntity(
                spaceId = localId,
                plantType = PlantType.TREE.name,
                isActive = true,
                startedAt = now,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}
