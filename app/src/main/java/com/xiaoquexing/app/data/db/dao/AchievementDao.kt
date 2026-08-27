package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xiaoquexing.app.data.db.entity.AchievementDefEntity
import com.xiaoquexing.app.data.db.entity.AchievementEventEntity
import com.xiaoquexing.app.data.db.entity.AchievementProgressEntity
import kotlinx.coroutines.flow.Flow

/** 定义 + 进度的一次性联查结果（LEFT JOIN，无进度行时 progress 为 null）。 */
data class DefWithProgress(
    @Embedded(prefix = "d_") val def: AchievementDefEntity,
    @Embedded(prefix = "p_") val progress: AchievementProgressEntity?
)

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDefs(defs: List<AchievementDefEntity>)

    @Query("SELECT * FROM achievement_definitions ORDER BY sort_order")
    suspend fun getDefs(): List<AchievementDefEntity>

    @Query("SELECT * FROM achievement_definitions WHERE code = :code LIMIT 1")
    suspend fun getDef(code: String): AchievementDefEntity?

    @Query("SELECT COUNT(*) FROM achievement_definitions")
    suspend fun countDefs(): Int

    @Query("SELECT * FROM achievement_progress WHERE definition_code = :code AND scope_key = :scopeKey LIMIT 1")
    suspend fun getProgress(code: String, scopeKey: String): AchievementProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: AchievementProgressEntity)

    @Insert
    suspend fun insertEvent(event: AchievementEventEntity)

    @Query("SELECT COUNT(*) FROM achievement_progress WHERE scope_key = :scopeKey AND is_unlocked = 1")
    fun observeUnlockedCount(scopeKey: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievement_progress WHERE scope_key = :scopeKey AND is_unlocked = 1")
    suspend fun countUnlocked(scopeKey: String): Int

    @Query("SELECT COUNT(*) FROM achievement_events WHERE definition_code = :code AND scope_key = :scopeKey")
    suspend fun countEvents(code: String, scopeKey: String): Int

    @Transaction
    @Query(
        "SELECT d.code AS d_code, d.title AS d_title, d.description AS d_description, d.emoji AS d_emoji, " +
            "d.category AS d_category, d.is_hidden AS d_is_hidden, d.condition_type AS d_condition_type, " +
            "d.condition_param AS d_condition_param, d.condition_sub_param AS d_condition_sub_param, " +
            "d.reward_type AS d_reward_type, d.reward_value AS d_reward_value, " +
            "d.sort_order AS d_sort_order, d.updated_at AS d_updated_at, " +
            "p.local_id AS p_local_id, p.definition_code AS p_definition_code, p.scope_key AS p_scope_key, " +
            "p.progress AS p_progress, p.is_unlocked AS p_is_unlocked, p.unlocked_at AS p_unlocked_at, " +
            "p.last_evaluated_at AS p_last_evaluated_at, p.server_id AS p_server_id, " +
            "p.created_at AS p_created_at, p.updated_at AS p_updated_at, p.deleted_at AS p_deleted_at, " +
            "p.sync_state AS p_sync_state, p.version AS p_version " +
            "FROM achievement_definitions d " +
            "LEFT JOIN achievement_progress p ON p.definition_code = d.code AND p.scope_key = :scopeKey " +
            "ORDER BY p.is_unlocked DESC, d.sort_order"
    )
    fun observeAchievements(scopeKey: String): Flow<List<DefWithProgress>>
}
