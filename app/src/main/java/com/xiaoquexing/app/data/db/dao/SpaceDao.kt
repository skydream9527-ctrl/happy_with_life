package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {

    @Insert
    suspend fun insert(space: SpaceEntity): Long

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun countSpaces(): Int

    @Query("SELECT * FROM spaces WHERE is_default = 1 AND deleted_at IS NULL LIMIT 1")
    suspend fun getDefaultSpace(): SpaceEntity?

    @Query("SELECT * FROM spaces WHERE deleted_at IS NULL")
    suspend fun getAllSpaces(): List<SpaceEntity>

    @Query("SELECT * FROM spaces WHERE is_default = 1 AND deleted_at IS NULL LIMIT 1")
    fun observeDefaultSpace(): Flow<SpaceEntity?>

    @Query("SELECT total_gp FROM spaces WHERE local_id = :spaceId")
    suspend fun getTotalGp(spaceId: Long): Int

    @Query("UPDATE spaces SET total_gp = :totalGp, updated_at = :now WHERE local_id = :spaceId")
    suspend fun setTotalGp(spaceId: Long, totalGp: Int, now: Long)

    @Query("UPDATE spaces SET server_id = :serverId, updated_at = :now WHERE local_id = :spaceId")
    suspend fun bindServerId(spaceId: Long, serverId: String, now: Long)

    @Query("SELECT * FROM spaces WHERE local_id = :spaceId LIMIT 1")
    suspend fun getById(spaceId: Long): SpaceEntity?

    @Query("SELECT * FROM spaces WHERE server_id = :serverId LIMIT 1")
    suspend fun findByServerId(serverId: String): SpaceEntity?

    @Query("UPDATE spaces SET is_default = 0, updated_at = :now WHERE deleted_at IS NULL")
    suspend fun clearDefault(now: Long)

    @Query("UPDATE spaces SET is_default = 1, updated_at = :now WHERE local_id = :spaceId")
    suspend fun setDefault(spaceId: Long, now: Long)

    @Query("SELECT * FROM spaces WHERE deleted_at IS NULL ORDER BY is_default DESC, created_at ASC")
    fun observeAll(): Flow<List<SpaceEntity>>
}
