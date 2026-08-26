package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.PlantType
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plant_states")
    fun getAllPlants(): Flow<List<PlantState>>

    @Query("SELECT * FROM plant_states WHERE isActive = 1 LIMIT 1")
    fun getActivePlant(): Flow<PlantState?>

    @Query("SELECT * FROM plant_states WHERE plantType = :type LIMIT 1")
    suspend fun getPlantByType(type: PlantType): PlantState?

    @Query("SELECT * FROM plant_states WHERE plantType = :type LIMIT 1")
    fun getPlantByTypeFlow(type: PlantType): Flow<PlantState?>

    @Query("SELECT COUNT(*) FROM plant_states WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalGp), 0) FROM plant_states")
    fun getTotalGp(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plantState: PlantState): Long

    @Update
    suspend fun update(plantState: PlantState)

    @Query("UPDATE plant_states SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()

    @Query("UPDATE plant_states SET isActive = 1, isUnlocked = 1 WHERE plantType = :type")
    suspend fun setActive(type: PlantType)

    @Query("UPDATE plant_states SET totalGp = totalGp + :gp WHERE isActive = 1")
    suspend fun addGpToActive(gp: Int)

    @Delete
    suspend fun delete(plantState: PlantState)
}
