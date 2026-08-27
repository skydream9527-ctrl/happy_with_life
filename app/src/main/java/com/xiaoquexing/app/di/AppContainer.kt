package com.xiaoquexing.app.di

import android.content.Context
import com.xiaoquexing.app.data.DataBootstrap
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.media.MediaImporter
import com.xiaoquexing.app.data.repository.AchievementRepository
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository

/**
 * 生产环境依赖容器（ADR-002）。
 *
 * ViewModel 一律通过构造函数接收 Repository；单元测试直接传 fake 实现即可构造
 * ViewModel，不需要 Android 运行时。禁止在 ViewModel 里向下转型 Application 取仓库。
 */
class AppContainer(private val appContext: Context) {

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val bootstrap: DataBootstrap by lazy { DataBootstrap(database) }

    val mediaImporter: MediaImporter by lazy { MediaImporter(appContext, database) }

    val recordRepository: RecordRepository by lazy { RecordRepository(database) }

    val plantRepository: PlantRepository by lazy { PlantRepository(database) }

    val achievementRepository: AchievementRepository by lazy { AchievementRepository(database) }
}
