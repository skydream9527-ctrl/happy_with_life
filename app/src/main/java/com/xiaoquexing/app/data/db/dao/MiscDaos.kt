package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoquexing.app.data.db.entity.AlbumEntity
import com.xiaoquexing.app.data.db.entity.AlbumPageEntity
import com.xiaoquexing.app.data.db.entity.DailySpaceStatEntity
import com.xiaoquexing.app.data.db.entity.OutboxEventEntity
import com.xiaoquexing.app.data.db.entity.RecordMediaEntity
import com.xiaoquexing.app.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM record_media WHERE record_id = :recordId")
    suspend fun forRecord(recordId: Long): List<RecordMediaEntity>

    @Query("SELECT * FROM record_media WHERE type = 'PHOTO' AND media_status = 'PENDING_COPY' AND record_id = :recordId")
    suspend fun pendingPhotos(recordId: Long): List<RecordMediaEntity>

    @Query("UPDATE record_media SET local_path = :localPath, media_status = :status, updated_at = :now WHERE local_id = :mediaId")
    suspend fun updateLocalPath(mediaId: Long, localPath: String?, status: String, now: Long)

    /**
     * 编辑时重写子行（v1 离线物理删除；M4 接同步后改为软删保留墓碑）。
     * 记录本体仍在，级联不会触发，必须显式清理。
     */
    @Query("DELETE FROM record_media WHERE record_id = :recordId")
    suspend fun deleteForRecord(recordId: Long)

    @Query(
        "SELECT m.* FROM record_media m JOIN records r ON m.record_id = r.local_id " +
            "WHERE r.deleted_at IS NOT NULL AND r.deleted_at < :cutoff AND m.local_path IS NOT NULL"
    )
    suspend fun filesOfRecordsDeletedBefore(cutoff: Long): List<RecordMediaEntity>
}

@Dao
interface DailyStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailySpaceStatEntity)

    @Query("SELECT gp_total FROM daily_space_stats WHERE space_id = :spaceId AND date_key = :dateKey")
    suspend fun gpOnDay(spaceId: Long, dateKey: Int): Int?

    @Query("SELECT date_key FROM daily_space_stats WHERE space_id = :spaceId")
    suspend fun dayKeys(spaceId: Long): List<Int>

    @Query("DELETE FROM daily_space_stats WHERE space_id = :spaceId AND date_key = :dateKey")
    suspend fun clearDay(spaceId: Long, dateKey: Int)

    /** 全量重建：永远以 records 为唯一真相来源（ADR D7 / §2.6） */
    @Query("DELETE FROM daily_space_stats")
    suspend fun clearAll()

    @Query(
        "INSERT INTO daily_space_stats (space_id, date_key, gp_total, record_count, distinct_author_count) " +
            "SELECT space_id, occurred_date_key, SUM(gp_final), COUNT(*), COUNT(DISTINCT author_id) " +
            "FROM records WHERE deleted_at IS NULL GROUP BY space_id, occurred_date_key"
    )
    suspend fun rebuildAll()
}

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(event: OutboxEventEntity): Long

    @Query("SELECT COUNT(*) FROM outbox_events WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface TagDao {

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE scope = :scope AND space_id = :spaceId AND kind = :kind AND name = :name AND deleted_at IS NULL LIMIT 1")
    suspend fun findTag(scope: String, spaceId: Long, kind: String, name: String): TagEntity?

    @Query("UPDATE tags SET use_count = use_count + 1, updated_at = :now WHERE local_id = :tagId")
    suspend fun incrementUseCount(tagId: Long, now: Long)
}

@Dao
interface AlbumDao {

    @Insert
    suspend fun insert(album: AlbumEntity): Long

    @Insert
    suspend fun insertPages(pages: List<AlbumPageEntity>)

    @Query("SELECT * FROM albums WHERE space_id = :spaceId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAlbums(spaceId: Long): Flow<List<AlbumEntity>>

    @Query("UPDATE albums SET deleted_at = :now WHERE local_id = :albumId")
    suspend fun softDelete(albumId: Long, now: Long): Int
}
