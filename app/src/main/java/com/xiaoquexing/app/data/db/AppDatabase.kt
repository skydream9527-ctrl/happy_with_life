package com.xiaoquexing.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xiaoquexing.app.data.db.dao.AchievementDao
import com.xiaoquexing.app.data.db.dao.AlbumDao
import com.xiaoquexing.app.data.db.dao.DailyStatDao
import com.xiaoquexing.app.data.db.dao.MediaDao
import com.xiaoquexing.app.data.db.dao.OutboxDao
import com.xiaoquexing.app.data.db.dao.PlantDao
import com.xiaoquexing.app.data.db.dao.RecordDao
import com.xiaoquexing.app.data.db.dao.SpaceDao
import com.xiaoquexing.app.data.db.dao.SpaceMemberDao
import com.xiaoquexing.app.data.db.dao.TagDao
import com.xiaoquexing.app.data.db.dao.UserDao
import com.xiaoquexing.app.data.db.entity.AchievementDefEntity
import com.xiaoquexing.app.data.db.entity.AchievementEventEntity
import com.xiaoquexing.app.data.db.entity.AchievementProgressEntity
import com.xiaoquexing.app.data.db.entity.AlbumEntity
import com.xiaoquexing.app.data.db.entity.AlbumPageEntity
import com.xiaoquexing.app.data.db.entity.DailySpaceStatEntity
import com.xiaoquexing.app.data.db.entity.OutboxEventEntity
import com.xiaoquexing.app.data.db.entity.PlantDefEntity
import com.xiaoquexing.app.data.db.entity.PlantSnapshotEntity
import com.xiaoquexing.app.data.db.entity.RecordEntity
import com.xiaoquexing.app.data.db.entity.RecordMediaEntity
import com.xiaoquexing.app.data.db.entity.RecordTagCrossRef
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import com.xiaoquexing.app.data.db.entity.SpaceMemberEntity
import com.xiaoquexing.app.data.db.entity.SpacePlantEntity
import com.xiaoquexing.app.data.db.entity.TagEntity
import com.xiaoquexing.app.data.db.entity.UserEntity

/**
 * 小确幸本地库 v2（android/docs/room-v2-schema.md）。
 *
 * - 版本升级必须提供显式 Migration（禁止 fallbackToDestructiveMigration，ADR-001 K4）；
 * - schema JSON 导出基线：app/schemas/（必须提交入库）；
 * - 领域规则（GP 归属、每日额度、软删除）见 ADR-001。
 */
@Database(
    entities = [
        RecordEntity::class,
        RecordMediaEntity::class,
        RecordTagCrossRef::class,
        TagEntity::class,
        SpaceEntity::class,
        SpaceMemberEntity::class,
        UserEntity::class,
        PlantDefEntity::class,
        SpacePlantEntity::class,
        PlantSnapshotEntity::class,
        AchievementDefEntity::class,
        AchievementProgressEntity::class,
        AchievementEventEntity::class,
        AlbumEntity::class,
        AlbumPageEntity::class,
        DailySpaceStatEntity::class,
        OutboxEventEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun tagDao(): TagDao
    abstract fun mediaDao(): MediaDao
    abstract fun spaceDao(): SpaceDao
    abstract fun userDao(): UserDao
    abstract fun spaceMemberDao(): SpaceMemberDao
    abstract fun plantDao(): PlantDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun outboxDao(): OutboxDao
    abstract fun albumDao(): AlbumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xiaoquexing.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // 禁止 fallbackToDestructiveMigration（ADR-001 K4）：
                    // 未提供 migration 的版本升级必须直接失败，不允许清库。
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
