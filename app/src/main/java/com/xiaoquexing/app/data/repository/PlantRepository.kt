package com.xiaoquexing.app.data.repository

import com.xiaoquexing.app.data.db.dao.PlantDao
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.PlantType
import kotlinx.coroutines.flow.Flow

class PlantRepository(private val plantDao: PlantDao) {

    fun getAllPlants(): Flow<List<PlantState>> = plantDao.getAllPlants()

    fun getActivePlant(): Flow<PlantState?> = plantDao.getActivePlant()

    fun getPlantByType(type: PlantType): Flow<PlantState?> = plantDao.getPlantByTypeFlow(type)

    fun getUnlockedCount(): Flow<Int> = plantDao.getUnlockedCount()

    fun getTotalGp(): Flow<Int> = plantDao.getTotalGp()

    suspend fun addGpToActive(gp: Int) = plantDao.addGpToActive(gp)

    suspend fun setActivePlant(type: PlantType) {
        plantDao.deactivateAll()
        // Unlock if needed
        val existing = plantDao.getPlantByType(type)
        if (existing != null) {
            plantDao.setActive(type)
        } else {
            plantDao.insert(
                PlantState(
                    plantType = type,
                    isActive = true,
                    isUnlocked = true
                )
            )
        }
    }

    suspend fun initializeDefaultPlants(totalGp: Int = 0) {
        // Check if already initialized
        val existing = plantDao.getPlantByType(PlantType.TREE)
        if (existing != null) return

        PlantType.entries.forEach { type ->
            val isUnlocked = type.unlockGp <= totalGp
            val isActive = type == PlantType.TREE
            plantDao.insert(
                PlantState(
                    plantType = type,
                    isActive = isActive,
                    isUnlocked = isUnlocked,
                    totalGp = if (isActive) 0 else 0
                )
            )
        }
    }

    suspend fun checkUnlocks(totalGp: Int) {
        PlantType.entries.forEach { type ->
            if (type.unlockGp in 1..totalGp) {
                val existing = plantDao.getPlantByType(type)
                if (existing != null && !existing.isUnlocked) {
                    plantDao.update(existing.copy(isUnlocked = true))
                }
            }
        }
    }
}
