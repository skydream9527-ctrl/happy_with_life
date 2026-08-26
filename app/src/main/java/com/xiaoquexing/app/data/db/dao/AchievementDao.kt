package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaoquexing.app.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, id ASC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): Achievement?

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Update
    suspend fun update(achievement: Achievement)

    @Query("UPDATE achievements SET progress = :progress, isUnlocked = :unlocked, unlockedAt = :unlockedAt WHERE code = :code")
    suspend fun updateProgress(code: String, progress: Int, unlocked: Boolean, unlockedAt: Long?)

    @Query("UPDATE achievements SET progress = :progress WHERE code = :code")
    suspend fun setProgress(code: String, progress: Int)
}
