package com.xiaoquexing.app.data

import com.xiaoquexing.app.data.db.entity.AchievementDefEntity
import com.xiaoquexing.app.data.db.entity.ConditionTypes
import com.xiaoquexing.app.data.db.entity.PlantDefEntity
import com.xiaoquexing.app.data.db.entity.TagEntity
import com.xiaoquexing.app.data.db.entity.TagKinds
import com.xiaoquexing.app.data.db.entity.TagScopes
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.model.MoodTag
import com.xiaoquexing.app.data.model.StatusTag

/**
 * v2 种子数据（ADR-001 D9.2 / D9.3）。
 *
 * 这是「产品内容」不是 Demo 数据（ADR D12）：正式包首启也要种下。
 * MIGRATION_1_2 与 DataBootstrap 共用同一份常量，避免两处漂移。
 */
object SeedData {

    const val DEFAULT_SPACE_NAME = "我的小确幸"
    const val LOCAL_USER_NAME = "小确幸用户"

    data class PlantSeed(
        val type: PlantType,
        val conditionType: String,
        val conditionParam: Int,
        val conditionSubParam: String? = null,
        val unlockedByDefault: Boolean = false
    )

    /** 九种植物解锁条件（PRD 3.2.2a 为准，冲突裁决见 ADR D9.2 / Q5） */
    val plants: List<PlantSeed> = listOf(
        PlantSeed(PlantType.TREE, ConditionTypes.DEFAULT, 0, unlockedByDefault = true),
        PlantSeed(PlantType.SUNFLOWER, ConditionTypes.DEFAULT, 0, unlockedByDefault = true),
        PlantSeed(PlantType.SAKURA, ConditionTypes.STREAK_DAYS, 30),
        PlantSeed(PlantType.CACTUS, ConditionTypes.DISTINCT_LOCATION_COUNT, 50),
        PlantSeed(PlantType.SUCCULENT, ConditionTypes.RECORD_COUNT, 100),
        PlantSeed(PlantType.VINE, ConditionTypes.SHARED_SPACE_EVENT, 1, "INVITE_CO_PLANT"),
        PlantSeed(PlantType.ROSE, ConditionTypes.SHARED_SPACE_EVENT, 1, "COUPLE_SPACE_JOINED"),
        PlantSeed(PlantType.BAMBOO, ConditionTypes.RECORD_DAY_COUNT, 365),
        PlantSeed(PlantType.MUSHROOM, ConditionTypes.HIDDEN_ACHIEVEMENT_COUNT, 3)
    )

    fun plantDefs(now: Long): List<PlantDefEntity> = plants.mapIndexed { index, seed ->
        PlantDefEntity(
            plantType = seed.type.name,
            displayName = seed.type.displayName,
            emoji = seed.type.emoji,
            conditionType = seed.conditionType,
            conditionParam = seed.conditionParam,
            conditionSubParam = seed.conditionSubParam,
            isUnlocked = seed.unlockedByDefault,
            unlockedAt = if (seed.unlockedByDefault) now else null,
            sortOrder = index,
            updatedAt = now
        )
    }

    data class AchievementSeed(
        val code: String,
        val title: String,
        val description: String,
        val emoji: String,
        val conditionType: String,
        val conditionParam: Int,
        val isHidden: Boolean = false
    )

    /** 成就定义（v1 code 兼容；口径修正见 ADR D9.3） */
    val achievements: List<AchievementSeed> = listOf(
        AchievementSeed("first_record", "初次记录", "记录你的第一个小确幸", "🌱", ConditionTypes.RECORD_COUNT, 1),
        AchievementSeed("record_10", "记录达人", "累计记录10条小确幸", "📝", ConditionTypes.RECORD_COUNT, 10),
        AchievementSeed("record_50", "生活记录者", "累计记录50条小确幸", "📖", ConditionTypes.RECORD_COUNT, 50),
        AchievementSeed("record_100", "坚持者", "累计记录100条小确幸", "💪", ConditionTypes.RECORD_COUNT, 100),
        AchievementSeed("record_500", "幸福收藏家", "累计记录500条小确幸", "🏆", ConditionTypes.RECORD_COUNT, 500),
        AchievementSeed("streak_7", "一周坚持", "连续记录7天", "🔥", ConditionTypes.STREAK_DAYS, 7),
        AchievementSeed("streak_30", "月度习惯", "连续记录30天", "⭐", ConditionTypes.STREAK_DAYS, 30),
        AchievementSeed("streak_100", "百日达人", "连续记录100天", "💯", ConditionTypes.STREAK_DAYS, 100),
        AchievementSeed("streak_365", "年度成就", "连续记录365天", "👑", ConditionTypes.STREAK_DAYS, 365),
        // K7 修复：按照片张数，而非含照片的记录数
        AchievementSeed("photographer", "摄影师", "累计添加100张照片", "📷", ConditionTypes.PHOTO_COUNT, 100),
        AchievementSeed("singer", "歌唱家", "记录50首不同的歌曲", "🎵", ConditionTypes.MUSIC_SONG_COUNT, 50),
        AchievementSeed("traveler", "旅行者", "在50个不同地点记录", "✈️", ConditionTypes.DISTINCT_LOCATION_COUNT, 50),
        AchievementSeed("share_10", "分享达人", "分享10次记录", "📤", ConditionTypes.SHARE_COUNT, 10),
        AchievementSeed("influencer", "传播幸福", "分享带来5个新用户", "🌟", ConditionTypes.SHARE_INVITE_USER_COUNT, 5),
        AchievementSeed("botanist", "植物学家", "解锁全部9种植物", "🌿", ConditionTypes.UNLOCKED_PLANT_COUNT, 9),
        AchievementSeed("music_collector", "旋律收藏家", "记录20首不同的歌曲", "🎶", ConditionTypes.MUSIC_SONG_COUNT, 20),
        AchievementSeed("sharer", "分享者", "完成首次分享", "💌", ConditionTypes.SHARE_COUNT, 1)
    )

    fun achievementDefs(now: Long): List<AchievementDefEntity> = achievements.mapIndexed { index, seed ->
        AchievementDefEntity(
            code = seed.code,
            title = seed.title,
            description = seed.description,
            emoji = seed.emoji,
            category = if (seed.isHidden) "EXPLORATION" else "MILESTONE",
            isHidden = seed.isHidden,
            conditionType = seed.conditionType,
            conditionParam = seed.conditionParam,
            sortOrder = index,
            updatedAt = now
        )
    }

    /** 标签注册表：状态标签可被记录引用；心情标签仅供 UI 选择器（记录上存 mood_tag 列） */
    fun tags(now: Long): List<TagEntity> =
        StatusTag.defaults.map {
            TagEntity(
                scope = TagScopes.USER,
                spaceId = 0,
                kind = TagKinds.STATUS,
                name = it.name,
                emoji = it.emoji,
                createdAt = now,
                updatedAt = now
            )
        } + MoodTag.defaults.map {
            TagEntity(
                scope = TagScopes.USER,
                spaceId = 0,
                kind = TagKinds.MOOD,
                name = it.name,
                emoji = it.emoji,
                createdAt = now,
                updatedAt = now
            )
        }
}
