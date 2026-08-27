package com.xiaoquexing.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xiaoquexing.app.data.SeedData
import com.xiaoquexing.app.util.DateKeys

private fun SupportSQLiteDatabase.executeInsert(sql: String, bindArgs: Array<out Any?>): Long {
    val statement = compileStatement(sql)
    return try {
        bindArgs.forEachIndexed { index, value ->
            val parameterIndex = index + 1
            when (value) {
                null -> statement.bindNull(parameterIndex)
                is ByteArray -> statement.bindBlob(parameterIndex, value)
                is Float -> statement.bindDouble(parameterIndex, value.toDouble())
                is Double -> statement.bindDouble(parameterIndex, value)
                is Number -> statement.bindLong(parameterIndex, value.toLong())
                is Boolean -> statement.bindLong(parameterIndex, if (value) 1L else 0L)
                else -> statement.bindString(parameterIndex, value.toString())
            }
        }
        statement.executeInsert()
    } finally {
        statement.close()
    }
}

/**
 * v1 → v2 迁移（android/docs/room-v2-schema.md §5）。
 *
 * 硬性规则：
 * - Room 在事务中执行本迁移，任一步抛异常整体回滚，v1 数据保持完整；
 * - 禁止任何形式的清库回退；
 * - GP 裁决：space.total_gp 一律取记录集合之和（修复 Demo 116 GP 与植物 0 分的分裂，K3）。
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val todayKey = DateKeys.epochDay(now)

        // ---- 步骤 1：旧表改名 ----
        db.execSQL("ALTER TABLE `records` RENAME TO `records_v1`")
        db.execSQL("ALTER TABLE `plant_states` RENAME TO `plant_states_v1`")
        db.execSQL("ALTER TABLE `achievements` RENAME TO `achievements_v1`")
        db.execSQL("ALTER TABLE `spaces` RENAME TO `spaces_v1`")

        // ---- 步骤 2：建 v2 表（DDL 与 Room 生成的 schema 逐列镜像） ----
        createTables(db)

        // ---- 步骤 3：种子（产品内容，非 Demo；ADR D12） ----
        seedUsers(db, now)
        db.execSQL(
            "INSERT INTO `spaces` (`local_id`, `name`, `space_type`, `is_default`, `total_gp`, `created_at`, `updated_at`, " +
                "`sync_state`, `version`) SELECT `id`, `name`, CASE `type` WHEN 'SHARED' THEN 'FRIEND' ELSE `type` END, " +
                "0, 0, `createdAt`, ?, 0, 0 FROM `spaces_v1`",
            arrayOf<Any>(now)
        )
        seedPlants(db, now)
        seedAchievementDefs(db, now)
        seedTags(db, now)

        // v1 spaces 保留原 id 迁入后，再创建默认个人空间（自增 id = max+1，无冲突）
        db.execSQL(
            "INSERT INTO `spaces` (`name`, `space_type`, `is_default`, `total_gp`, `created_at`, `updated_at`, " +
                "`sync_state`, `version`) VALUES (?, 'PERSONAL', 1, 0, ?, ?, 0, 0)",
            arrayOf(SeedData.DEFAULT_SPACE_NAME, now, now)
        )
        val defaultSpaceId = db.query(
            "SELECT `local_id` FROM `spaces` WHERE `is_default` = 1"
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else error("默认空间创建失败") }
        db.execSQL(
            "INSERT INTO `space_members` (`space_id`, `user_id`, `role`, `joined_at`, `contributed_gp`, `created_at`, " +
                "`updated_at`, `sync_state`, `version`) VALUES (?, 1, 'OWNER', ?, 0, ?, ?, 0, 0)",
            arrayOf(defaultSpaceId, now, now, now)
        )

        // ---- 步骤 4：记录搬迁 + 媒体/标签拆表 ----
        val tagIdCache = HashMap<String, Long>()
        db.query("SELECT * FROM `records_v1`").use { cursor ->
            while (cursor.moveToNext()) {
                migrateOneRecord(db, cursor, defaultSpaceId, tagIdCache, now)
            }
        }

        // ---- 步骤 5：植物状态与成就进度迁移 ----
        migratePlantStates(db, defaultSpaceId, now)
        migrateAchievements(db, now)

        // ---- 步骤 6：派生重建（唯一真相来源 = records） ----
        db.execSQL(
            "UPDATE `spaces` SET `total_gp` = " +
                "(SELECT COALESCE(SUM(`gp_final`), 0) FROM `records` " +
                " WHERE `space_id` = `spaces`.`local_id` AND `deleted_at` IS NULL), `updated_at` = ?",
            arrayOf<Any>(now)
        )
        db.execSQL(
            "INSERT INTO `daily_space_stats` (`space_id`, `date_key`, `gp_total`, `record_count`, `distinct_author_count`) " +
                "SELECT `space_id`, `occurred_date_key`, SUM(`gp_final`), COUNT(*), COUNT(DISTINCT `author_id`) " +
                "FROM `records` WHERE `deleted_at` IS NULL GROUP BY `space_id`, `occurred_date_key`"
        )
        // 迁移基线快照（画册时间轴起点）
        db.execSQL(
            "INSERT INTO `plant_snapshots` (`space_id`, `plant_type`, `event_type`, `stage`, `gp_at_event`, " +
                "`occurred_at`, `occurred_date_key`, `server_id`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                "SELECT s.`local_id`, COALESCE(sp.`plant_type`, 'TREE'), 'MIGRATED_BASELINE', " +
                "CASE WHEN s.`total_gp` < 50 THEN 0 WHEN s.`total_gp` < 200 THEN 1 WHEN s.`total_gp` < 500 THEN 2 " +
                "WHEN s.`total_gp` < 1500 THEN 3 WHEN s.`total_gp` < 4000 THEN 4 WHEN s.`total_gp` < 10000 THEN 5 " +
                "ELSE 6 END, s.`total_gp`, ?, ?, NULL, ?, ?, 0, 0 " +
                "FROM `spaces` s LEFT JOIN `space_plants` sp " +
                "ON sp.`space_id` = s.`local_id` AND sp.`is_active` = 1",
            arrayOf(now, todayKey, now, now)
        )

        // ---- 步骤 7：删旧表 + 完整性校验 ----
        db.execSQL("DROP TABLE IF EXISTS `records_v1`")
        db.execSQL("DROP TABLE IF EXISTS `plant_states_v1`")
        db.execSQL("DROP TABLE IF EXISTS `achievements_v1`")
        db.execSQL("DROP TABLE IF EXISTS `spaces_v1`")

        val fkErrors = db.query("PRAGMA foreign_key_check").use { c -> c.count }
        check(fkErrors == 0) { "迁移后外键完整性校验失败：$fkErrors 行违规" }
    }

    private fun createTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`display_name` TEXT NOT NULL, `avatar_local_path` TEXT, `premium_expire_at` INTEGER, " +
                "`server_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plants` (`plant_type` TEXT NOT NULL, `display_name` TEXT NOT NULL, " +
                "`emoji` TEXT NOT NULL, `condition_type` TEXT NOT NULL, `condition_param` INTEGER NOT NULL, " +
                "`condition_sub_param` TEXT, `is_unlocked` INTEGER NOT NULL, `unlocked_at` INTEGER, " +
                "`sort_order` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`plant_type`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `achievement_definitions` (`code` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, `emoji` TEXT NOT NULL, `category` TEXT NOT NULL, `is_hidden` INTEGER NOT NULL, " +
                "`condition_type` TEXT NOT NULL, `condition_param` INTEGER NOT NULL, `condition_sub_param` TEXT, " +
                "`reward_type` TEXT, `reward_value` TEXT, `sort_order` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`code`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`scope` TEXT NOT NULL, `space_id` INTEGER NOT NULL, `kind` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`emoji` TEXT NOT NULL, `color` TEXT, `use_count` INTEGER NOT NULL, `server_id` TEXT, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `spaces` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `space_type` TEXT NOT NULL, `is_default` INTEGER NOT NULL, " +
                "`total_gp` INTEGER NOT NULL, `server_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "`deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `space_members` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`space_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `role` TEXT NOT NULL, " +
                "`joined_at` INTEGER NOT NULL, `contributed_gp` INTEGER NOT NULL, `server_id` TEXT, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`user_id`) REFERENCES `users`(`local_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `records` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`space_id` INTEGER NOT NULL, `author_id` INTEGER NOT NULL, `content_text` TEXT, " +
                "`mood_tag` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `occurred_date_key` INTEGER NOT NULL, " +
                "`is_backdated` INTEGER NOT NULL, `gp_final` INTEGER NOT NULL, `gp_breakdown_json` TEXT, " +
                "`is_capped` INTEGER NOT NULL, `server_id` TEXT, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`author_id`) REFERENCES `users`(`local_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `record_media` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`record_id` INTEGER NOT NULL, `type` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `local_path` TEXT, " +
                "`source_uri` TEXT, `remote_uri` TEXT, `media_status` TEXT NOT NULL, `mime_type` TEXT, " +
                "`duration_ms` INTEGER, `width` INTEGER, `height` INTEGER, `title` TEXT, `subtitle` TEXT, `extra_json` TEXT, " +
                "`server_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`record_id`) REFERENCES `records`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `record_tag_cross_ref` (`record_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, " +
                "PRIMARY KEY(`record_id`, `tag_id`), " +
                "FOREIGN KEY(`record_id`) REFERENCES `records`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tag_id`) REFERENCES `tags`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `space_plants` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`space_id` INTEGER NOT NULL, `plant_type` TEXT NOT NULL, `is_active` INTEGER NOT NULL, " +
                "`started_at` INTEGER NOT NULL, `ended_at` INTEGER, `server_id` TEXT, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`plant_type`) REFERENCES `plants`(`plant_type`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plant_snapshots` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`space_id` INTEGER NOT NULL, `plant_type` TEXT NOT NULL, `event_type` TEXT NOT NULL, " +
                "`stage` INTEGER NOT NULL, `gp_at_event` INTEGER NOT NULL, `occurred_at` INTEGER NOT NULL, " +
                "`occurred_date_key` INTEGER NOT NULL, `server_id` TEXT, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `achievement_progress` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`definition_code` TEXT NOT NULL, `scope_key` TEXT NOT NULL, `progress` INTEGER NOT NULL, " +
                "`is_unlocked` INTEGER NOT NULL, `unlocked_at` INTEGER, `last_evaluated_at` INTEGER NOT NULL, " +
                "`server_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`definition_code`) REFERENCES `achievement_definitions`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `achievement_events` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`definition_code` TEXT NOT NULL, `scope_key` TEXT NOT NULL, `event_type` TEXT NOT NULL, " +
                "`progress_before` INTEGER NOT NULL, `progress_after` INTEGER NOT NULL, `occurred_at` INTEGER NOT NULL, " +
                "`reason_json` TEXT, `server_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "`deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`definition_code`) REFERENCES `achievement_definitions`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `albums` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`space_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `theme` TEXT NOT NULL, `range_type` TEXT NOT NULL, " +
                "`stage_start` INTEGER, `stage_end` INTEGER, `date_start` INTEGER, `date_end` INTEGER, " +
                "`entry_count` INTEGER NOT NULL, `page_count` INTEGER NOT NULL, `layout_seed` INTEGER NOT NULL, " +
                "`entry_hash` TEXT NOT NULL, `cover_local_path` TEXT, `server_id` TEXT, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_state` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `album_pages` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`album_id` INTEGER NOT NULL, `page_index` INTEGER NOT NULL, `page_type` TEXT NOT NULL, " +
                "`payload_json` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`album_id`) REFERENCES `albums`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_space_stats` (`space_id` INTEGER NOT NULL, `date_key` INTEGER NOT NULL, " +
                "`gp_total` INTEGER NOT NULL, `record_count` INTEGER NOT NULL, `distinct_author_count` INTEGER NOT NULL, " +
                "PRIMARY KEY(`space_id`, `date_key`), " +
                "FOREIGN KEY(`space_id`) REFERENCES `spaces`(`local_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `outbox_events` (`local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`entity_type` TEXT NOT NULL, `entity_local_id` INTEGER NOT NULL, `operation` TEXT NOT NULL, " +
                "`payload_json` TEXT, `state` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `last_error` TEXT, " +
                "`next_retry_at` INTEGER, `created_at` INTEGER NOT NULL)"
        )

        // 声明索引（名称与 Room 注解生成的完全一致）
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_records_space_id_occurred_date_key` ON `records` (`space_id`, `occurred_date_key`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_records_space_id_occurred_at` ON `records` (`space_id`, `occurred_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_records_author_id_occurred_at` ON `records` (`author_id`, `occurred_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_records_sync_state` ON `records` (`sync_state`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_records_server_id` ON `records` (`server_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_record_media_record_id` ON `record_media` (`record_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_record_media_type` ON `record_media` (`type`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_record_media_server_id` ON `record_media` (`server_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_record_tag_cross_ref_record_id` ON `record_tag_cross_ref` (`record_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_record_tag_cross_ref_tag_id` ON `record_tag_cross_ref` (`tag_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_scope_space_id_kind_name` ON `tags` (`scope`, `space_id`, `kind`, `name`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_server_id` ON `tags` (`server_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_spaces_server_id` ON `spaces` (`server_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_server_id` ON `users` (`server_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_space_members_space_id_user_id` ON `space_members` (`space_id`, `user_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_space_plants_space_id` ON `space_plants` (`space_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plant_snapshots_space_id_occurred_at` ON `plant_snapshots` (`space_id`, `occurred_at`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievement_progress_definition_code_scope_key` ON `achievement_progress` (`definition_code`, `scope_key`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievement_events_scope_key_event_type` ON `achievement_events` (`scope_key`, `event_type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_space_id_created_at` ON `albums` (`space_id`, `created_at`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_albums_server_id` ON `albums` (`server_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_album_pages_album_id_page_index` ON `album_pages` (`album_id`, `page_index`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_events_state_next_retry_at` ON `outbox_events` (`state`, `next_retry_at`)")

        // 注：spaces.is_default / space_plants.is_active 的部分唯一索引暂不创建。
        // Room 迁移后的 TableInfo 校验对「实际库多出的索引」是否宽容未经 CI 验证（T16），
        // 在此验证前以应用层保证唯一性：retireActiveSpacePlant 先退后插、
        // DataBootstrap 事务内双检。CI 验证通过后于后续版本补建（room-v2-schema §3 注）。
    }

    private fun seedUsers(db: SupportSQLiteDatabase, now: Long) {
        db.execSQL(
            "INSERT INTO `users` (`display_name`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                "VALUES (?, ?, ?, 0, 0)",
            arrayOf(SeedData.LOCAL_USER_NAME, now, now)
        )
    }

    private fun seedPlants(db: SupportSQLiteDatabase, now: Long) {
        val insert = "INSERT INTO `plants` (`plant_type`, `display_name`, `emoji`, `condition_type`, " +
            "`condition_param`, `condition_sub_param`, `is_unlocked`, `unlocked_at`, `sort_order`, `updated_at`) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        SeedData.plantDefs(now).forEach { p ->
            db.execSQL(
                insert,
                arrayOf(
                    p.plantType, p.displayName, p.emoji, p.conditionType, p.conditionParam,
                    p.conditionSubParam, if (p.isUnlocked) 1 else 0, p.unlockedAt, p.sortOrder, p.updatedAt
                )
            )
        }
    }

    private fun seedAchievementDefs(db: SupportSQLiteDatabase, now: Long) {
        val insert = "INSERT INTO `achievement_definitions` (`code`, `title`, `description`, `emoji`, `category`, " +
            "`is_hidden`, `condition_type`, `condition_param`, `condition_sub_param`, `reward_type`, `reward_value`, " +
            "`sort_order`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        SeedData.achievementDefs(now).forEach { d ->
            db.execSQL(
                insert,
                arrayOf(
                    d.code, d.title, d.description, d.emoji, d.category,
                    if (d.isHidden) 1 else 0, d.conditionType, d.conditionParam, d.conditionSubParam,
                    d.rewardType, d.rewardValue, d.sortOrder, d.updatedAt
                )
            )
        }
    }

    private fun seedTags(db: SupportSQLiteDatabase, now: Long) {
        val insert = "INSERT INTO `tags` (`scope`, `space_id`, `kind`, `name`, `emoji`, `use_count`, `created_at`, " +
            "`updated_at`, `sync_state`, `version`) VALUES (?, 0, ?, ?, ?, 0, ?, ?, 0, 0)"
        SeedData.tags(now).forEach { t ->
            db.execSQL(insert, arrayOf(t.scope, t.kind, t.name, t.emoji, t.createdAt, t.updatedAt))
        }
    }

    private fun migrateOneRecord(
        db: SupportSQLiteDatabase,
        cursor: android.database.Cursor,
        defaultSpaceId: Long,
        tagIdCache: HashMap<String, Long>,
        now: Long
    ) {
        fun col(name: String): Int = cursor.getColumnIndexOrThrow(name)
        fun long(name: String): Long = cursor.getLong(col(name))
        fun string(name: String): String? = if (cursor.isNull(col(name))) null else cursor.getString(col(name))

        val v1Id = long("id")
        val createdAt = long("createdAt")
        val dateKey = DateKeys.epochDay(createdAt)
        val mood = string("moodTag") ?: "平静" // R4：v1 允许 NULL，迁移回填默认心情（ADR 原则 1）

        val newId = db.executeInsert(
            "INSERT INTO `records` (`local_id`, `space_id`, `author_id`, `content_text`, `mood_tag`, `occurred_at`, " +
                "`occurred_date_key`, `is_backdated`, `gp_final`, `gp_breakdown_json`, `is_capped`, `created_at`, `updated_at`, " +
                "`sync_state`, `version`) VALUES (?, ?, 1, ?, ?, ?, ?, 0, ?, NULL, 0, ?, ?, 0, 0)",
            arrayOf(v1Id, defaultSpaceId, string("text"), mood, createdAt, dateKey, long("gpEarned"), createdAt, now)
        )
        check(newId == v1Id) { "记录 $v1Id 迁移后主键漂移：$newId" }

        // ---- 媒体拆表 ----
        val ts = createdAt
        string("photoUris")?.takeIf { it.isNotBlank() }?.split('|')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.forEachIndexed { index, uri ->
                val isFile = uri.startsWith("file:") || uri.startsWith("/")
                db.execSQL(
                    "INSERT INTO `record_media` (`record_id`, `type`, `sort_order`, `local_path`, `source_uri`, " +
                        "`media_status`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                        "VALUES (?, 'PHOTO', ?, ?, ?, ?, ?, ?, 0, 0)",
                    arrayOf(v1Id, index, if (isFile) uri else null, uri, if (isFile) "READY" else "PENDING_COPY", ts, now)
                )
            }
        string("voiceUri")?.let { voice ->
            db.execSQL(
                "INSERT INTO `record_media` (`record_id`, `type`, `sort_order`, `local_path`, `media_status`, " +
                    "`duration_ms`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                    "VALUES (?, 'VOICE', 0, ?, 'READY', ?, ?, ?, 0, 0)",
                arrayOf(v1Id, voice, long("voiceDuration"), ts, now)
            )
        }
        string("musicTitle")?.let { title ->
            db.execSQL(
                "INSERT INTO `record_media` (`record_id`, `type`, `sort_order`, `title`, `subtitle`, `extra_json`, " +
                    "`media_status`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                    "VALUES (?, 'MUSIC', 0, ?, ?, ?, 'READY', ?, ?, 0, 0)",
                arrayOf(v1Id, title, string("musicArtist"), string("musicUri"), ts, now)
            )
        }
        string("linkUrl")?.let { url ->
            val summary = string("linkSummary")
            val extra = if (summary != null) "{\"summary\":${jsonEscape(summary)}}" else null
            db.execSQL(
                "INSERT INTO `record_media` (`record_id`, `type`, `sort_order`, `source_uri`, `title`, `extra_json`, " +
                    "`media_status`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                    "VALUES (?, 'LINK', 0, ?, ?, ?, 'READY', ?, ?, 0, 0)",
                arrayOf(v1Id, url, string("linkTitle"), extra, ts, now)
            )
        }
        string("locationName")?.let { name ->
            val lat = if (cursor.isNull(col("locationLat"))) null else cursor.getDouble(col("locationLat"))
            val lng = if (cursor.isNull(col("locationLng"))) null else cursor.getDouble(col("locationLng"))
            val extra = buildString {
                append('{')
                lat?.let { append("\"lat\":$it,") }
                lng?.let { append("\"lng\":$it") }
                append('}')
            }
            db.execSQL(
                "INSERT INTO `record_media` (`record_id`, `type`, `sort_order`, `title`, `extra_json`, `media_status`, " +
                    "`created_at`, `updated_at`, `sync_state`, `version`) " +
                    "VALUES (?, 'LOCATION', 0, ?, ?, 'READY', ?, ?, 0, 0)",
                arrayOf(v1Id, name, extra, ts, now)
            )
        }

        // ---- 标签拆表（STATUS 注册表命中否则 CUSTOM） ----
        string("statusTags")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.forEach { name ->
            val tagId = tagIdCache.getOrPut(name) {
                db.query(
                    "SELECT `local_id`, `kind` FROM `tags` WHERE `scope` = 'USER' AND `space_id` = 0 AND `name` = ? LIMIT 1",
                    arrayOf(name)
                ).use { c ->
                    if (c.moveToFirst()) c.getLong(0) else -1L
                }.let { existing ->
                    if (existing > 0) {
                        existing
                    } else {
                        db.executeInsert(
                            "INSERT INTO `tags` (`scope`, `space_id`, `kind`, `name`, `emoji`, `use_count`, `created_at`, " +
                                "`updated_at`, `sync_state`, `version`) VALUES ('USER', 0, 'CUSTOM', ?, '', 0, ?, ?, 0, 0)",
                            arrayOf(name, now, now)
                        )
                    }
                }
            }
            db.execSQL(
                "INSERT OR IGNORE INTO `record_tag_cross_ref` (`record_id`, `tag_id`) VALUES (?, ?)",
                arrayOf(v1Id, tagId)
            )
        }
    }

    private fun migratePlantStates(db: SupportSQLiteDatabase, defaultSpaceId: Long, now: Long) {
        var hasActive = false
        db.query("SELECT `plantType`, `isActive`, `isUnlocked`, `plantedAt` FROM `plant_states_v1`").use { c ->
            while (c.moveToNext()) {
                val type = c.getString(0)
                val isActive = c.getInt(1) == 1
                val isUnlocked = c.getInt(2) == 1
                val plantedAt = c.getLong(3)
                if (isUnlocked) {
                    db.execSQL(
                        "UPDATE `plants` SET `is_unlocked` = 1, `unlocked_at` = COALESCE(`unlocked_at`, ?), `updated_at` = ? " +
                            "WHERE `plant_type` = ? AND `is_unlocked` = 0",
                        arrayOf(plantedAt, now, type)
                    )
                }
                if (isActive) {
                    hasActive = true
                    db.execSQL(
                        "INSERT INTO `space_plants` (`space_id`, `plant_type`, `is_active`, `started_at`, `created_at`, " +
                            "`updated_at`, `sync_state`, `version`) VALUES (?, ?, 1, ?, ?, ?, 0, 0)",
                        arrayOf(defaultSpaceId, type, plantedAt, now, now)
                    )
                }
            }
        }
        if (!hasActive) {
            db.execSQL(
                "INSERT INTO `space_plants` (`space_id`, `plant_type`, `is_active`, `started_at`, `created_at`, " +
                    "`updated_at`, `sync_state`, `version`) VALUES (?, 'TREE', 1, ?, ?, ?, 0, 0)",
                arrayOf(defaultSpaceId, now, now, now)
            )
        }
    }

    private fun migrateAchievements(db: SupportSQLiteDatabase, now: Long) {
        db.execSQL(
            "INSERT INTO `achievement_progress` (`definition_code`, `scope_key`, `progress`, `is_unlocked`, `unlocked_at`, " +
                "`last_evaluated_at`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                "SELECT a.`code`, 'u:1', a.`progress`, a.`isUnlocked`, a.`unlockedAt`, ?, COALESCE(a.`unlockedAt`, ?), ?, 0, 0 " +
                "FROM `achievements_v1` a WHERE a.`code` IN (SELECT `code` FROM `achievement_definitions`)",
            arrayOf<Any>(now, now, now)
        )
        db.execSQL(
            "INSERT INTO `achievement_events` (`definition_code`, `scope_key`, `event_type`, `progress_before`, `progress_after`, " +
                "`occurred_at`, `reason_json`, `created_at`, `updated_at`, `sync_state`, `version`) " +
                "SELECT a.`code`, 'u:1', 'UNLOCKED', 0, a.`progress`, COALESCE(a.`unlockedAt`, ?), 'migrated_from_v1', ?, ?, 0, 0 " +
                "FROM `achievements_v1` a WHERE a.`isUnlocked` = 1 AND a.`code` IN (SELECT `code` FROM `achievement_definitions`)",
            arrayOf<Any>(now, now, now)
        )
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
