package com.xiaoquexing.app.data.db.entity

/**
 * v2 数据库的字符串/整数枚举词汇表（ADR-001 / room-v2-schema.md）。
 *
 * 统一用常量而非 Kotlin 枚举 + TypeConverter：列保持 TEXT/INTEGER，SQL 可直接
 * 比较（WHERE type = 'PHOTO'），也避免转换器漂移。新增取值只加常量，不改 schema。
 */
object SyncStates {
    const val SYNCED = 0
    const val SYNC_PENDING = 1
    const val DELETE_PENDING = 2
    const val CONFLICT = 3
}

object MediaTypes {
    const val PHOTO = "PHOTO"
    const val VOICE = "VOICE"
    const val MUSIC = "MUSIC"
    const val LINK = "LINK"
    const val LOCATION = "LOCATION"
}

object MediaStatus {
    const val READY = "READY"
    const val PENDING_COPY = "PENDING_COPY"
    const val MISSING = "MISSING"
}

object TagScopes {
    const val USER = "USER"
    const val SPACE = "SPACE"
}

object TagKinds {
    const val MOOD = "MOOD"
    const val STATUS = "STATUS"
    const val CUSTOM = "CUSTOM"
}

object MemberRoles {
    const val OWNER = "OWNER"
    const val ADMIN = "ADMIN"
    const val MEMBER = "MEMBER"
}

object PlantEventTypes {
    const val STAGE_UP = "STAGE_UP"
    const val STAGE_DOWN = "STAGE_DOWN"
    const val PLANT_SWITCHED = "PLANT_SWITCHED"
    const val PLANT_RETIRED = "PLANT_RETIRED"
    const val MIGRATED_BASELINE = "MIGRATED_BASELINE"
}

object AchievementEventTypes {
    const val UNLOCKED = "UNLOCKED"
    const val RELOCKED = "RELOCKED"
    const val SERVER_CALIBRATED = "SERVER_CALIBRATED"
}

/** 植物与成就的解锁条件类型（ADR-001 D9.1）。 */
object ConditionTypes {
    const val DEFAULT = "DEFAULT"
    const val TOTAL_GP = "TOTAL_GP"
    const val STREAK_DAYS = "STREAK_DAYS"
    const val RECORD_COUNT = "RECORD_COUNT"
    const val RECORD_DAY_COUNT = "RECORD_DAY_COUNT"
    const val DISTINCT_LOCATION_COUNT = "DISTINCT_LOCATION_COUNT"
    const val PHOTO_COUNT = "PHOTO_COUNT"
    const val MUSIC_SONG_COUNT = "MUSIC_SONG_COUNT"
    const val SHARE_COUNT = "SHARE_COUNT"
    const val UNLOCKED_PLANT_COUNT = "UNLOCKED_PLANT_COUNT"
    const val SHARE_INVITE_USER_COUNT = "SHARE_INVITE_USER_COUNT"
    const val SHARED_SPACE_EVENT = "SHARED_SPACE_EVENT"
    const val HIDDEN_ACHIEVEMENT_COUNT = "HIDDEN_ACHIEVEMENT_COUNT"
    const val PREMIUM = "PREMIUM"
}

object OutboxStates {
    const val PENDING = "PENDING"
    const val IN_FLIGHT = "IN_FLIGHT"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
}

object OutboxOps {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}
