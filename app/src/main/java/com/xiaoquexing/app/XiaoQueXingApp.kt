package com.xiaoquexing.app

import android.app.Application
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.repository.AchievementRepository
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XiaoQueXingApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val recordRepository by lazy { RecordRepository(database.recordDao()) }
    val plantRepository by lazy { PlantRepository(database.plantDao()) }
    val achievementRepository by lazy { AchievementRepository(database.achievementDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize demo data on first launch
        CoroutineScope(Dispatchers.IO).launch {
            initializeData()
        }
    }

    private suspend fun initializeData() {
        val plantDao = database.plantDao()
        val recordDao = database.recordDao()
        val achDao = database.achievementDao()

        // Check if already initialized
        val existingPlant = plantDao.getPlantByType(PlantType.TREE)
        if (existingPlant != null) return

        // Initialize plants
        plantRepository.initializeDefaultPlants(0)

        // Initialize achievements
        achievementRepository.initializeDefaults()

        // Insert demo records (3-5 records)
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val demoRecords = listOf(
            Record(
                text = "今天阳光很好，和朋友一起在公园散步，看到了很多盛开的花朵，心情特别美丽～",
                moodTag = "开心",
                statusTags = "自然,聚会",
                gpEarned = 25,
                createdAt = now - dayMs * 0
            ),
            Record(
                text = "一个人在家做了一顿美味的意大利面，配着红酒看了部老电影，独处的时光也很治愈。",
                moodTag = "平静",
                statusTags = "美食,居家,电影",
                gpEarned = 30,
                createdAt = now - dayMs * 1
            ),
            Record(
                text = "收到了远方朋友寄来的明信片，上面写着想念我，瞬间泪目。",
                moodTag = "感动",
                statusTags = "想念",
                gpEarned = 15,
                createdAt = now - dayMs * 2
            ),
            Record(
                text = "完成了今天的跑步目标！5公里慢跑，虽然有点累但是特别有成就感，晚上奖励自己一块蛋糕🍰",
                moodTag = "兴奋",
                statusTags = "运动,美食",
                gpEarned = 28,
                createdAt = now - dayMs * 3
            ),
            Record(
                text = "读到一本特别棒的书，《小王子》里说'真正重要的东西，用眼睛是看不见的'，深有感触。",
                moodTag = "平静",
                statusTags = "阅读",
                gpEarned = 18,
                createdAt = now - dayMs * 4
            )
        )
        demoRecords.forEach { recordDao.insert(it) }

        // Update first record achievement
        achievementRepository.updateProgress("first_record", 1)
    }

    companion object {
        lateinit var instance: XiaoQueXingApp
            private set
    }
}
