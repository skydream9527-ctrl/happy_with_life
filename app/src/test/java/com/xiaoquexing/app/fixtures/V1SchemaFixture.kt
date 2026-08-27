package com.xiaoquexing.app.fixtures

/**
 * Room v1（当前线上 schema）的手写 DDL 基线。
 *
 * 用途：v1 从未导出过 schema JSON（exportSchema 原为 false），v1→v2 迁移测试
 * （android/docs/room-v2-schema.md §10）需要一份可以在 JVM 上重建 v1 库的 DDL。
 *
 * 权威顺序：CI 生成的 app/schemas/com.xiaoquexing.app.data.db.AppDatabase/1.json
 * 入库后即为唯一权威；若两者不一致，以 1.json 为准修订本文件。
 */
object V1SchemaFixture {

    const val DB_NAME = "xiaoquexing-v1-fixture.db"

    val CREATE_STATEMENTS = listOf(
        // 对应 Record.kt（moodTag 可空是 ADR-001 R4 记录的缺陷，迁移时需回填）
        """
        CREATE TABLE IF NOT EXISTS `records` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `text` TEXT NOT NULL,
            `moodTag` TEXT,
            `statusTags` TEXT NOT NULL,
            `photoUris` TEXT NOT NULL,
            `voiceUri` TEXT,
            `voiceDuration` INTEGER NOT NULL,
            `musicTitle` TEXT,
            `musicArtist` TEXT,
            `musicUri` TEXT,
            `linkUrl` TEXT,
            `linkTitle` TEXT,
            `linkSummary` TEXT,
            `locationName` TEXT,
            `locationLat` REAL,
            `locationLng` REAL,
            `gpEarned` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `isBackdated` INTEGER NOT NULL
        )
        """.trimIndent(),
        // 对应 PlantState.kt
        """
        CREATE TABLE IF NOT EXISTS `plant_states` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `plantType` TEXT NOT NULL,
            `totalGp` INTEGER NOT NULL,
            `isActive` INTEGER NOT NULL,
            `isUnlocked` INTEGER NOT NULL,
            `plantedAt` INTEGER NOT NULL,
            `lastWateredAt` INTEGER
        )
        """.trimIndent(),
        // 对应 Achievement.kt
        """
        CREATE TABLE IF NOT EXISTS `achievements` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `code` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `emoji` TEXT NOT NULL,
            `requirement` INTEGER NOT NULL,
            `progress` INTEGER NOT NULL,
            `isUnlocked` INTEGER NOT NULL,
            `unlockedAt` INTEGER
        )
        """.trimIndent(),
        // 对应 Space.kt
        """
        CREATE TABLE IF NOT EXISTS `spaces` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `type` TEXT NOT NULL,
            `memberCount` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL
        )
        """.trimIndent()
    )
}
