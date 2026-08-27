package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xiaoquexing.app.data.db.entity.PlantDefEntity
import com.xiaoquexing.app.data.db.entity.PlantSnapshotEntity
import com.xiaoquexing.app.data.db.entity.SpacePlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    // ---- 目录与解锁 ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlantDefs(plants: List<PlantDefEntity>)

    @Query("SELECT * FROM plants ORDER BY sort_order")
    suspend fun getPlantDefs(): List<PlantDefEntity>

    @Query("SELECT * FROM plants ORDER BY sort_order")
    fun observePlantDefs(): Flow<List<PlantDefEntity>>

    @Query("SELECT * FROM plants WHERE plant_type = :plantType LIMIT 1")
    suspend fun getPlantDef(plantType: String): PlantDefEntity?

    @Query("UPDATE plants SET is_unlocked = 1, unlocked_at = COALESCE(unlocked_at, :unlockedAt), updated_at = :now WHERE plant_type = :plantType AND is_unlocked = 0")
    suspend fun unlockPlant(plantType: String, unlockedAt: Long, now: Long): Int

    @Query("UPDATE plants SET is_unlocked = 0, unlocked_at = NULL, updated_at = :now WHERE plant_type = :plantType")
    suspend fun relockPlant(plantType: String, now: Long)

    @Query("SELECT COUNT(*) FROM plants WHERE is_unlocked = 1")
    suspend fun countUnlockedPlants(): Int

    @Query("SELECT COUNT(*) FROM plants WHERE is_unlocked = 1")
    fun observeUnlockedPlantCount(): Flow<Int>

    // ---- 空间植物实例 ----

    @Insert
    suspend fun insertSpacePlant(spacePlant: SpacePlantEntity): Long

    @Query("SELECT * FROM space_plants WHERE space_id = :spaceId AND is_active = 1 AND deleted_at IS NULL LIMIT 1")
    suspend fun getActiveSpacePlant(spaceId: Long): SpacePlantEntity?

    @Query("SELECT COUNT(*) FROM space_plants WHERE space_id = :spaceId AND is_active = 1 AND deleted_at IS NULL")
    suspend fun countActivePlants(spaceId: Long): Int

    @Query("SELECT * FROM space_plants WHERE space_id = :spaceId AND is_active = 1 AND deleted_at IS NULL LIMIT 1")
    fun observeActiveSpacePlant(spaceId: Long): Flow<SpacePlantEntity?>

    @Query("UPDATE space_plants SET is_active = 0, ended_at = :endedAt, updated_at = :endedAt WHERE space_id = :spaceId AND is_active = 1")
    suspend fun retireActiveSpacePlant(spaceId: Long, endedAt: Long)

    // ---- 快照 ----

    @Insert
    suspend fun insertSnapshot(snapshot: PlantSnapshotEntity)

    @Query("SELECT * FROM plant_snapshots WHERE space_id = :spaceId ORDER BY occurred_at DESC LIMIT 1")
    suspend fun getLatestSnapshot(spaceId: Long): PlantSnapshotEntity?
}
