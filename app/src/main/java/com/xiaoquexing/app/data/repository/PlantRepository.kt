package com.xiaoquexing.app.data.repository

import androidx.room.withTransaction
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.PlantEventTypes
import com.xiaoquexing.app.data.db.entity.PlantSnapshotEntity
import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.util.DateKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 植物仓库 v2：植物不持有 GP（ADR D7），阶段与分数全部来自空间 totalGp。
 * 换植物只换载体（D8）：切换写 PLANT_SWITCHED 快照，GP 不变。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantRepository(private val db: AppDatabase) {

    private val plantDao = db.plantDao()
    private val spaceDao = db.spaceDao()

    /** 图鉴列表（domain PlantState 的 totalGp 仅活动植物 = 空间总分，其余为 0） */
    fun getAllPlants(): Flow<List<PlantState>> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            if (space == null) {
                flowOf(emptyList())
            } else {
                combine(
                    plantDao.observePlantDefs(),
                    plantDao.observeActiveSpacePlant(space.localId),
                    db.recordDao().observeTotalGp(space.localId)
                ) { defs, active, totalGp ->
                    defs.map { toDomain(it, active?.plantType, totalGp, active?.startedAt) }
                }
            }
        }

    fun getActivePlant(): Flow<PlantState?> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            if (space == null) {
                flowOf(null)
            } else {
                combine(
                    plantDao.observePlantDefs(),
                    plantDao.observeActiveSpacePlant(space.localId),
                    db.recordDao().observeTotalGp(space.localId)
                ) { defs, active, totalGp ->
                    active?.let { a ->
                        defs.find { it.plantType == a.plantType }
                            ?.let { toDomain(it, a.plantType, totalGp, a.startedAt) }
                    }
                }
            }
        }

    fun getUnlockedCount(): Flow<Int> = plantDao.observeUnlockedPlantCount()

    /** 换植物 = 换载体（D8）：GP 与阶段保持不变，写 PLANT_SWITCHED 快照。未解锁不切换。 */
    suspend fun setActivePlant(type: PlantType) = db.withTransaction {
        val space = spaceDao.getDefaultSpace() ?: error("默认空间未初始化")
        val now = System.currentTimeMillis()
        val def = plantDao.getPlantDef(type.name)
        if (def == null || !def.isUnlocked) return@withTransaction

        val totalGp = db.recordDao().sumAllGp(space.localId)
        plantDao.retireActiveSpacePlant(space.localId, now)
        plantDao.insertSpacePlant(
            com.xiaoquexing.app.data.db.entity.SpacePlantEntity(
                spaceId = space.localId,
                plantType = type.name,
                isActive = true,
                startedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
        plantDao.insertSnapshot(
            PlantSnapshotEntity(
                spaceId = space.localId,
                plantType = type.name,
                eventType = PlantEventTypes.PLANT_SWITCHED,
                stage = PlantStage.fromGp(totalGp).ordinal,
                gpAtEvent = totalGp,
                occurredAt = now,
                occurredDateKey = DateKeys.epochDay(now),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun toDomain(
        def: com.xiaoquexing.app.data.db.entity.PlantDefEntity,
        activePlantType: String?,
        spaceTotalGp: Int,
        activeStartedAt: Long?
    ): PlantState = PlantState(
        plantType = PlantType.valueOf(def.plantType),
        totalGp = if (def.plantType == activePlantType) spaceTotalGp else 0,
        isActive = def.plantType == activePlantType,
        isUnlocked = def.isUnlocked,
        plantedAt = if (def.plantType == activePlantType) (activeStartedAt ?: def.unlockedAt ?: 0) else (def.unlockedAt ?: 0),
        lastWateredAt = null
    )
}
