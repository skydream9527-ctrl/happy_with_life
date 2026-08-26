package com.xiaoquexing.app.data.repository

import com.xiaoquexing.app.data.db.dao.AchievementDao
import com.xiaoquexing.app.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

class AchievementRepository(private val achievementDao: AchievementDao) {

    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    fun getUnlockedAchievements(): Flow<List<Achievement>> = achievementDao.getUnlockedAchievements()

    fun getUnlockedCount(): Flow<Int> = achievementDao.getUnlockedCount()

    suspend fun initializeDefaults() {
        val defaults = listOf(
            Achievement(code = "first_record", title = "初次记录", description = "记录你的第一个小确幸", emoji = "🌱", requirement = 1),
            Achievement(code = "record_10", title = "记录达人", description = "累计记录10条小确幸", emoji = "📝", requirement = 10),
            Achievement(code = "record_50", title = "生活记录者", description = "累计记录50条小确幸", emoji = "📖", requirement = 50),
            Achievement(code = "record_100", title = "坚持者", description = "累计记录100条小确幸", emoji = "💪", requirement = 100),
            Achievement(code = "record_500", title = "幸福收藏家", description = "累计记录500条小确幸", emoji = "🏆", requirement = 500),
            Achievement(code = "streak_7", title = "一周坚持", description = "连续记录7天", emoji = "🔥", requirement = 7),
            Achievement(code = "streak_30", title = "月度习惯", description = "连续记录30天", emoji = "⭐", requirement = 30),
            Achievement(code = "streak_100", title = "百日达人", description = "连续记录100天", emoji = "💯", requirement = 100),
            Achievement(code = "streak_365", title = "年度成就", description = "连续记录365天", emoji = "👑", requirement = 365),
            Achievement(code = "photographer", title = "摄影师", description = "添加100张照片", emoji = "📷", requirement = 100),
            Achievement(code = "singer", title = "歌唱家", description = "分享50首音乐", emoji = "🎵", requirement = 50),
            Achievement(code = "traveler", title = "旅行者", description = "记录50个地点", emoji = "✈️", requirement = 50),
            Achievement(code = "share_10", title = "分享达人", description = "分享10次记录", emoji = "📤", requirement = 10),
            Achievement(code = "influencer", title = "传播幸福", description = "分享带来5个新用户", emoji = "🌟", requirement = 5),
            Achievement(code = "botanist", title = "植物学家", description = "解锁所有植物", emoji = "🌿", requirement = 9),
            Achievement(code = "music_collector", title = "旋律收藏家", description = "分享20首不同歌曲", emoji = "🎶", requirement = 20),
            Achievement(code = "sharer", title = "分享者", description = "完成首次分享", emoji = "💌", requirement = 1)
        )
        achievementDao.insertAll(defaults)
    }

    suspend fun updateProgress(code: String, progress: Int) {
        val achievement = achievementDao.getByCode(code) ?: return
        val unlocked = progress >= achievement.requirement && achievement.requirement > 0
        achievementDao.updateProgress(
            code = code,
            progress = progress,
            unlocked = unlocked,
            unlockedAt = if (unlocked && !achievement.isUnlocked) System.currentTimeMillis() else achievement.unlockedAt
        )
    }

    suspend fun unlockAchievement(code: String) {
        val achievement = achievementDao.getByCode(code) ?: return
        achievementDao.updateProgress(
            code = code,
            progress = achievement.requirement,
            unlocked = true,
            unlockedAt = System.currentTimeMillis()
        )
    }
}
