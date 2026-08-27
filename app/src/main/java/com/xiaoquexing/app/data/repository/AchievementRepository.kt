package com.xiaoquexing.app.data.repository

import com.xiaoquexing.app.data.SeedData
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.dao.DefWithProgress
import com.xiaoquexing.app.data.entity.Achievement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 成就仓库 v2：定义是产品内容（SeedData 分发），进度只由 AchievementEvaluator 写入。
 *
 * scopeKey 固定 "u:1"：离线单机只有一个本地用户（DataBootstrap 保证其 local_id=1）。
 * M4 接账号后改为从会话注入当前用户。
 */
class AchievementRepository(private val db: AppDatabase) {

    private val achievementDao = db.achievementDao()

    fun getAllAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAchievements(USER_SCOPE).map { list -> list.map(::toDomain) }

    fun getUnlockedAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAchievements(USER_SCOPE).map { list ->
            list.filter { it.progress?.isUnlocked == true }.map(::toDomain)
        }

    fun getUnlockedCount(): Flow<Int> = achievementDao.observeUnlockedCount(USER_SCOPE)

    suspend fun initializeDefaults() {
        val now = System.currentTimeMillis()
        achievementDao.upsertDefs(SeedData.achievementDefs(now))
    }

    private fun toDomain(d: DefWithProgress): Achievement = Achievement(
        id = d.def.sortOrder.toLong(),
        code = d.def.code,
        title = d.def.title,
        description = d.def.description,
        emoji = d.def.emoji,
        requirement = d.def.conditionParam,
        progress = d.progress?.progress ?: 0,
        isUnlocked = d.progress?.isUnlocked ?: false,
        unlockedAt = d.progress?.unlockedAt
    )

    companion object {
        const val USER_SCOPE = "u:1"
    }
}
