package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaoquexing.app.data.entity.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE createdAt >= :startTime AND createdAt < :endTime ORDER BY createdAt DESC")
    fun getRecordsByDateRange(startTime: Long, endTime: Long): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): Record?

    @Query("SELECT * FROM records ORDER BY createdAt DESC LIMIT 1")
    fun getLatestRecord(): Flow<Record?>

    @Query("SELECT * FROM records ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<Record>>

    @Query("SELECT COALESCE(SUM(gpEarned), 0) FROM records WHERE createdAt >= :startOfDay AND createdAt < :endOfDay")
    fun getTodayGp(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(gpEarned), 0) FROM records")
    fun getTotalGp(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE createdAt >= :dayStart AND createdAt < :dayEnd")
    suspend fun hasRecordsOnDay(dayStart: Long, dayEnd: Long): Int

    @Query("SELECT COUNT(*) FROM records WHERE photoUris != ''")
    fun getPhotoRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE musicTitle IS NOT NULL")
    fun getMusicRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE locationName IS NOT NULL")
    fun getLocationRecordCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT substr(createdAt, 1, 8)) FROM records")
    fun getRecordDays(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record): Long

    @Update
    suspend fun update(record: Record)

    @Delete
    suspend fun delete(record: Record)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
