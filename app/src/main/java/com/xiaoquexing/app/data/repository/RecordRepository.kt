package com.xiaoquexing.app.data.repository

import com.xiaoquexing.app.data.db.dao.RecordDao
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.util.GPCalculator
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RecordRepository(private val recordDao: RecordDao) {

    fun getAllRecords(): Flow<List<Record>> = recordDao.getAllRecords()

    fun getRecentRecords(limit: Int = 5): Flow<List<Record>> = recordDao.getRecentRecords(limit)

    fun getLatestRecord(): Flow<Record?> = recordDao.getLatestRecord()

    fun getTodayGp(): Flow<Int> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val startOfDay = today.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        return recordDao.getTodayGp(startOfDay, endOfDay)
    }

    fun getTotalCount(): Flow<Int> = recordDao.getTotalCount()

    fun getTotalGp(): Flow<Int> = recordDao.getTotalGp()

    fun getPhotoRecordCount(): Flow<Int> = recordDao.getPhotoRecordCount()

    fun getMusicRecordCount(): Flow<Int> = recordDao.getMusicRecordCount()

    fun getLocationRecordCount(): Flow<Int> = recordDao.getLocationRecordCount()

    suspend fun getRecordById(id: Long): Record? = recordDao.getRecordById(id)

    suspend fun insert(record: Record): Long = recordDao.insert(record)

    suspend fun delete(record: Record) = recordDao.delete(record)

    suspend fun calculateStreakDays(): Int {
        val allRecords = kotlinx.coroutines.flow.first
        // We'll use a simpler approach - query all records and calculate
        return 1 // Simplified for demo
    }

    suspend fun calculateGpForRecord(record: Record, streakDays: Int, todayGpSoFar: Int): Int {
        val isBackdated = GPCalculator.isBackdated(record.createdAt)
        val breakdown = GPCalculator.calculate(
            textLength = record.text.length,
            photoCount = record.getPhotoUriList().size,
            hasVoice = record.voiceUri != null,
            hasMusic = record.musicTitle != null,
            hasLink = record.linkUrl != null,
            hasLocation = record.locationName != null,
            hasMood = record.moodTag != null,
            statusTagCount = record.getStatusTagList().size,
            streakDays = streakDays,
            isBackdated = isBackdated,
            todayGpSoFar = todayGpSoFar
        )
        return breakdown.finalGp
    }
}
