package com.xiaoquexing.app.data

import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.AchievementEventEntity
import com.xiaoquexing.app.data.db.entity.AchievementEventTypes
import com.xiaoquexing.app.data.db.entity.AchievementProgressEntity
import com.xiaoquexing.app.data.db.entity.ConditionTypes
import com.xiaoquexing.app.data.db.entity.SyncStates
import com.xiaoquexing.app.util.StreakCalculator
import java.time.LocalDate

/**
 * 成就与植物解锁的统一重估器（ADR-001 D9 / D6.4）。
 *
 * - 输入是当前数据库快照（发布/编辑/删除事务内调用，与其他写入原子）；
 * - 进度永远 = 当前统计值；条件不再满足的已解锁项回锁并写 RELOCKED 事件（D6.4）；
 * - 离线单机版无法求值的条件（共享事件/分享/邀请）保持现状不动，等数据源接入。
 */
class AchievementEvaluator(private val db: AppDatabase) {

    data class Counters(
        val recordCount: Int,
        val streakDays: Int,
        val recordDayCount: Int,
        val distinctLocationCount: Int,
        val photoCount: Int,
        val distinctSongCount: Int,
        val unlockedPlantCount: Int
    )

    suspend fun collectCounters(spaceId: Long): Counters {
        val recordDao = db.recordDao()
        val keys = recordDao.distinctDateKeys(spaceId).toHashSet()
        return Counters(
            recordCount = recordDao.countRecords(spaceId),
            streakDays = StreakCalculator.calculateFromKeys(keys, LocalDate.now().toEpochDay().toInt()),
            recordDayCount = keys.size,
            distinctLocationCount = recordDao.countDistinctLocations(spaceId),
            photoCount = recordDao.countPhotos(spaceId),
            distinctSongCount = recordDao.countDistinctSongs(spaceId),
            unlockedPlantCount = db.plantDao().countUnlockedPlants()
        )
    }

    /** 在调用方的事务内执行；不得自行开事务。 */
    suspend fun evaluate(spaceId: Long, scopeKey: String, reasonJson: String? = null) {
        val counters = collectCounters(spaceId)
        evaluate(spaceId, scopeKey, counters, reasonJson)
    }

    suspend fun evaluate(spaceId: Long, scopeKey: String, counters: Counters, reasonJson: String?) {
        val now = System.currentTimeMillis()
        val achievementDao = db.achievementDao()
        val plantDao = db.plantDao()

        // ---- 成就进度：可求值条件重算进度并解锁/回锁；不可求值条件保持现状 ----
        achievementDao.getDefs().forEach { def ->
            val progressValue = progressFor(def.conditionType, def.conditionParam, def.conditionSubParam, counters)
            if (progressValue != null) {
                val existing = achievementDao.getProgress(def.code, scopeKey)
                val wasUnlocked = existing?.isUnlocked ?: false
                val unlocked = progressValue >= def.conditionParam && def.conditionParam > 0
                val progress = AchievementProgressEntity(
                    localId = existing?.localId ?: 0,
                    definitionCode = def.code,
                    scopeKey = scopeKey,
                    progress = progressValue,
                    isUnlocked = unlocked,
                    unlockedAt = if (unlocked) (existing?.unlockedAt ?: now) else null,
                    lastEvaluatedAt = now,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    syncState = SyncStates.SYNC_PENDING
                )
                if (existing == null || existing.progress != progressValue || existing.isUnlocked != unlocked) {
                    achievementDao.upsertProgress(progress)
                    if (unlocked && !wasUnlocked) {
                        achievementDao.insertEvent(
                            event(def.code, scopeKey, AchievementEventTypes.UNLOCKED, existing?.progress ?: 0, progressValue, now, reasonJson)
                        )
                    } else if (!unlocked && wasUnlocked) {
                        achievementDao.insertEvent(
                            event(def.code, scopeKey, AchievementEventTypes.RELOCKED, existing?.progress ?: 0, progressValue, now, reasonJson)
                        )
                    }
                }
            }
        }

        // ---- 植物解锁：同一套计数器重估（D6.5，可回锁；快照历史不受影响） ----
        plantDao.getPlantDefs().forEach { plant ->
            val conditionMet = when (plant.conditionType) {
                ConditionTypes.DEFAULT -> true
                ConditionTypes.STREAK_DAYS -> counters.streakDays >= plant.conditionParam
                ConditionTypes.RECORD_COUNT -> counters.recordCount >= plant.conditionParam
                ConditionTypes.RECORD_DAY_COUNT -> counters.recordDayCount >= plant.conditionParam
                ConditionTypes.DISTINCT_LOCATION_COUNT -> counters.distinctLocationCount >= plant.conditionParam
                ConditionTypes.PHOTO_COUNT -> counters.photoCount >= plant.conditionParam
                else -> null // TOTAL_GP 在 v2 中不再是植物解锁条件（D9.2）；共享/会员/隐藏成就离线版不可求值
            }
            if (conditionMet == true && !plant.isUnlocked) {
                plantDao.unlockPlant(plant.plantType, now, now)
            } else if (conditionMet == false && plant.isUnlocked &&
                plant.conditionType != ConditionTypes.DEFAULT
            ) {
                plantDao.relockPlant(plant.plantType, now)
            }
        }
        // UNLOCKED_PLANT_COUNT 依赖上面的植物解锁结果，循环后重新取一次计数再补估
        val afterPlantUnlocks = plantDao.countUnlockedPlants()
        if (afterPlantUnlocks != counters.unlockedPlantCount) {
            achievementDao.getDefs().forEach { def ->
                if (def.conditionType == ConditionTypes.UNLOCKED_PLANT_COUNT) {
                    val existing = achievementDao.getProgress(def.code, scopeKey)
                    val unlocked = afterPlantUnlocks >= def.conditionParam && def.conditionParam > 0
                    if (existing == null || existing.progress != afterPlantUnlocks || existing.isUnlocked != unlocked) {
                        achievementDao.upsertProgress(
                            AchievementProgressEntity(
                                localId = existing?.localId ?: 0,
                                definitionCode = def.code,
                                scopeKey = scopeKey,
                                progress = afterPlantUnlocks,
                                isUnlocked = unlocked,
                                unlockedAt = if (unlocked) (existing?.unlockedAt ?: now) else null,
                                lastEvaluatedAt = now,
                                createdAt = existing?.createdAt ?: now,
                                updatedAt = now,
                                syncState = SyncStates.SYNC_PENDING
                            )
                        )
                    }
                }
            }
        }
    }

    /** 返回 null 表示该条件在离线单机版不可求值（保持现状）。 */
    private fun progressFor(
        conditionType: String,
        conditionParam: Int,
        conditionSubParam: String?,
        counters: Counters
    ): Int? = when (conditionType) {
        ConditionTypes.RECORD_COUNT -> counters.recordCount
        ConditionTypes.STREAK_DAYS -> counters.streakDays
        ConditionTypes.RECORD_DAY_COUNT -> counters.recordDayCount
        ConditionTypes.DISTINCT_LOCATION_COUNT -> counters.distinctLocationCount
        ConditionTypes.PHOTO_COUNT -> counters.photoCount
        ConditionTypes.MUSIC_SONG_COUNT -> counters.distinctSongCount
        ConditionTypes.UNLOCKED_PLANT_COUNT -> counters.unlockedPlantCount
        // SHARE_COUNT / SHARE_INVITE_USER_COUNT / SHARED_SPACE_EVENT / PREMIUM / HIDDEN_ACHIEVEMENT_COUNT：
        // 数据源未接入（I2/M4/M5），离线版保持既有进度
        else -> null
    }

    private fun event(
        code: String,
        scopeKey: String,
        type: String,
        before: Int,
        after: Int,
        now: Long,
        reasonJson: String?
    ) = AchievementEventEntity(
        definitionCode = code,
        scopeKey = scopeKey,
        eventType = type,
        progressBefore = before,
        progressAfter = after,
        occurredAt = now,
        reasonJson = reasonJson,
        createdAt = now,
        updatedAt = now
    )
}
