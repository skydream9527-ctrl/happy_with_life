package com.xiaoquexing.app.util

import com.xiaoquexing.app.data.repository.AchievementRepository
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * 发布后批量触发成就 / 植物解锁。
 *
 * 触发时机：在 RecordRepository.insert 之后调一次即可。
 *
 * 输入是当前数据库快照（已包含刚插入的记录），用 Flow.first() 拿一次值。
 * 如果 Flow 还没就绪，触发失败会吞掉异常 —— 成就/植物解锁是 best-effort，
 * 不该因为统计失败让用户看不到发布成功的 GP 动画。
 */
class AchievementTrigger(
    private val recordRepo: RecordRepository,
    private val plantRepo: PlantRepository,
    private val achievementRepo: AchievementRepository
) {

    suspend fun onRecordPublished() {
        runCatching {
            val totalCount = recordRepo.getTotalCount().firstOrNull() ?: 0
            val totalGp = recordRepo.getTotalGp().firstOrNull() ?: 0
            val photoCount = recordRepo.getPhotoRecordCount().firstOrNull() ?: 0
            val musicCount = recordRepo.getMusicRecordCount().firstOrNull() ?: 0
            val locationCount = recordRepo.getLocationRecordCount().firstOrNull() ?: 0
            val streak = recordRepo.calculateStreakDays()

            // 记录条数类
            achievementRepo.updateProgress("first_record", totalCount.coerceAtLeast(1))
            achievementRepo.updateProgress("record_10", totalCount)
            achievementRepo.updateProgress("record_50", totalCount)
            achievementRepo.updateProgress("record_100", totalCount)
            achievementRepo.updateProgress("record_500", totalCount)

            // 连续天数类
            achievementRepo.updateProgress("streak_7", streak)
            achievementRepo.updateProgress("streak_30", streak)
            achievementRepo.updateProgress("streak_100", streak)
            achievementRepo.updateProgress("streak_365", streak)

            // 内容类
            achievementRepo.updateProgress("photographer", photoCount)
            achievementRepo.updateProgress("singer", musicCount)
            achievementRepo.updateProgress("traveler", locationCount)

            // 植物解锁 + botanist 成就
            plantRepo.checkUnlocks(totalGp)
            val unlockedPlants = plantRepo.getUnlockedCount().firstOrNull() ?: 0
            achievementRepo.updateProgress("botanist", unlockedPlants)
        }
    }
}
