package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.xiaoquexing.app.data.db.entity.RecordEntity
import com.xiaoquexing.app.data.db.entity.RecordMediaEntity
import com.xiaoquexing.app.data.db.entity.RecordTagCrossRef
import com.xiaoquexing.app.data.db.entity.SyncStates
import com.xiaoquexing.app.data.db.relation.RecordWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    // ---- 写入 ----

    @Insert
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert
    suspend fun insertMedia(media: List<RecordMediaEntity>)

    @Insert
    suspend fun insertCrossRefs(refs: List<RecordTagCrossRef>)

    @Query("UPDATE records SET gp_final = :gpFinal, is_capped = :isCapped, gp_breakdown_json = :breakdownJson, updated_at = :now WHERE local_id = :recordId")
    suspend fun updateGp(recordId: Long, gpFinal: Int, isCapped: Boolean, breakdownJson: String?, now: Long)

    /** 编辑专用：内容与发生时间变更，版本 +1 并置待同步（ADR D5） */
    @Query(
        "UPDATE records SET content_text = :text, mood_tag = :mood, occurred_at = :occurredAt, " +
            "occurred_date_key = :dateKey, is_backdated = :isBackdated, sync_state = :syncState, " +
            "version = version + 1, updated_at = :now WHERE local_id = :recordId"
    )
    suspend fun updateEditable(
        recordId: Long,
        text: String?,
        mood: String,
        occurredAt: Long,
        dateKey: Int,
        isBackdated: Boolean,
        syncState: Int,
        now: Long
    )

    @Query("UPDATE records SET deleted_at = :now, sync_state = :syncState, version = version + 1, updated_at = :now WHERE local_id = :recordId AND deleted_at IS NULL")
    suspend fun softDelete(recordId: Long, now: Long, syncState: Int = SyncStates.DELETE_PENDING): Int

    @Query("DELETE FROM record_tag_cross_ref WHERE record_id = :recordId")
    suspend fun clearCrossRefs(recordId: Long)

    // ---- 读（含媒体与标签） ----

    @Transaction
    @Query("SELECT * FROM records WHERE deleted_at IS NULL ORDER BY occurred_at DESC")
    fun observeAllRecordDetails(): Flow<List<RecordWithDetails>>

    @Transaction
    @Query("SELECT * FROM records WHERE deleted_at IS NULL ORDER BY occurred_at DESC LIMIT :limit")
    fun observeRecentRecordDetails(limit: Int): Flow<List<RecordWithDetails>>

    @Transaction
    @Query("SELECT * FROM records WHERE deleted_at IS NULL ORDER BY occurred_at DESC LIMIT 1")
    fun observeLatestRecordDetail(): Flow<RecordWithDetails?>

    @Transaction
    @Query("SELECT * FROM records WHERE local_id = :recordId AND deleted_at IS NULL")
    suspend fun getRecordDetailById(recordId: Long): RecordWithDetails?

    @Query("SELECT * FROM records WHERE local_id = :recordId")
    suspend fun getRawById(recordId: Long): RecordEntity?

    // ---- 派生统计（ADR：occurred_date_key 是唯一日期口径） ----

    @Query("SELECT COUNT(*) FROM records WHERE space_id = :spaceId AND deleted_at IS NULL")
    suspend fun countRecords(spaceId: Long): Int

    @Query("SELECT COUNT(*) FROM records WHERE space_id = :spaceId AND deleted_at IS NULL")
    fun observeTotalCount(spaceId: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT occurred_date_key) FROM records WHERE space_id = :spaceId AND deleted_at IS NULL")
    suspend fun countRecordDays(spaceId: Long): Int

    @Query("SELECT DISTINCT occurred_date_key FROM records WHERE space_id = :spaceId AND deleted_at IS NULL")
    suspend fun distinctDateKeys(spaceId: Long): List<Int>

    @Query("SELECT COALESCE(SUM(gp_final), 0) FROM records WHERE space_id = :spaceId AND occurred_date_key = :dateKey AND deleted_at IS NULL")
    suspend fun sumGpOnDate(spaceId: Long, dateKey: Int): Int

    @Query("SELECT COALESCE(SUM(gp_final), 0) FROM records WHERE space_id = :spaceId AND occurred_date_key = :dateKey AND deleted_at IS NULL AND local_id != :excludeRecordId")
    suspend fun sumGpOnDateExcluding(spaceId: Long, dateKey: Int, excludeRecordId: Long): Int

    @Query("SELECT COALESCE(SUM(gp_final), 0) FROM records WHERE space_id = :spaceId AND deleted_at IS NULL")
    suspend fun sumAllGp(spaceId: Long): Int

    @Query("SELECT COUNT(*) FROM records WHERE space_id = :spaceId AND occurred_date_key = :dateKey AND deleted_at IS NULL")
    suspend fun countOnDate(spaceId: Long, dateKey: Int): Int

    @Query("SELECT COUNT(DISTINCT author_id) FROM records WHERE space_id = :spaceId AND occurred_date_key = :dateKey AND deleted_at IS NULL")
    suspend fun countAuthorsOnDate(spaceId: Long, dateKey: Int): Int

    // ---- UI 兼容 Flow（单空间时代口径：全表未删除） ----

    @Query("SELECT COUNT(*) FROM records WHERE deleted_at IS NULL")
    fun observeTotalCountAll(): Flow<Int>

    @Query("SELECT COALESCE(SUM(gp_final), 0) FROM records WHERE deleted_at IS NULL")
    fun observeTotalGpAll(): Flow<Int>

    @Query("SELECT COALESCE(SUM(gp_final), 0) FROM records WHERE occurred_date_key = :dateKey AND deleted_at IS NULL")
    fun observeGpOnDateAll(dateKey: Int): Flow<Int>

    // ---- 媒体口径统计（K7：照片张数 / 不同地点 / 不同歌曲） ----

    @Query(
        "SELECT COUNT(*) FROM record_media m JOIN records r ON m.record_id = r.local_id " +
            "WHERE m.type = 'PHOTO' AND m.deleted_at IS NULL AND r.deleted_at IS NULL AND r.space_id = :spaceId"
    )
    suspend fun countPhotos(spaceId: Long): Int

    @Query(
        "SELECT COUNT(DISTINCT m.title) FROM record_media m JOIN records r ON m.record_id = r.local_id " +
            "WHERE m.type = 'LOCATION' AND m.deleted_at IS NULL AND r.deleted_at IS NULL AND r.space_id = :spaceId"
    )
    suspend fun countDistinctLocations(spaceId: Long): Int

    @Query(
        "SELECT COUNT(DISTINCT m.title || '|' || COALESCE(m.subtitle, '')) FROM record_media m " +
            "JOIN records r ON m.record_id = r.local_id " +
            "WHERE m.type = 'MUSIC' AND m.deleted_at IS NULL AND r.deleted_at IS NULL AND r.space_id = :spaceId"
    )
    suspend fun countDistinctSongs(spaceId: Long): Int

    // ---- 同步扫描（M4） ----

    @Query("SELECT COUNT(*) FROM records WHERE sync_state != 0")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM records WHERE sync_state != 0 ORDER BY updated_at ASC LIMIT 50")
    suspend fun listPendingSync(): List<RecordEntity>

    @Query(
        "UPDATE records SET server_id = :serverId, gp_final = :gpFinal, version = :version, " +
            "sync_state = 0, updated_at = :now WHERE local_id = :recordId"
    )
    suspend fun markSynced(recordId: Long, serverId: String, gpFinal: Int, version: Int, now: Long)
}
