package com.xiaoquexing.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.xiaoquexing.app.data.db.dao.AchievementDao
import com.xiaoquexing.app.data.db.dao.PlantDao
import com.xiaoquexing.app.data.db.dao.RecordDao
import com.xiaoquexing.app.data.db.dao.SpaceDao
import com.xiaoquexing.app.data.entity.Achievement
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.entity.Space
import com.xiaoquexing.app.data.entity.SpaceType

class Converters {
    @TypeConverter
    fun fromPlantType(value: PlantType): String = value.name

    @TypeConverter
    fun toPlantType(value: String): PlantType = PlantType.valueOf(value)

    @TypeConverter
    fun fromSpaceType(value: SpaceType): String = value.name

    @TypeConverter
    fun toSpaceType(value: String): SpaceType = SpaceType.valueOf(value)
}

@Database(
    entities = [Record::class, PlantState::class, Achievement::class, Space::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun plantDao(): PlantDao
    abstract fun achievementDao(): AchievementDao
    abstract fun spaceDao(): SpaceDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
