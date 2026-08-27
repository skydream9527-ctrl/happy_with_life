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
}
