package com.xiaoquexing.app.di

import android.content.Context
import com.xiaoquexing.app.BuildConfig
import com.xiaoquexing.app.data.DataBootstrap
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.media.MediaImporter
import com.xiaoquexing.app.data.media.PhotoUploader
import com.xiaoquexing.app.data.remote.SessionRepository
import com.xiaoquexing.app.data.remote.DraftStore
import com.xiaoquexing.app.data.remote.PlanStore
import com.xiaoquexing.app.data.remote.SettingsStore
import com.xiaoquexing.app.data.remote.SyncEngine
import com.xiaoquexing.app.data.remote.TokenHolder
import com.xiaoquexing.app.data.remote.TokenStore
import com.xiaoquexing.app.data.remote.createApiService
import com.xiaoquexing.app.data.repository.AchievementRepository
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import com.xiaoquexing.app.data.repository.SpaceRepository

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

    val tokenHolder = TokenHolder()

    val tokenStore: TokenStore by lazy { TokenStore(appContext) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val draftStore: DraftStore by lazy { DraftStore(appContext) }

    val planStore: PlanStore by lazy { PlanStore(appContext) }

    val apiService by lazy {
        createApiService(
            baseUrl = BuildConfig.XQX_API_BASE,
            deviceId = tokenHolder.deviceId,
            appVersion = BuildConfig.VERSION_NAME,
            tokenProvider = { tokenHolder.accessToken },
            deviceIdProvider = { tokenHolder.deviceId },
        )
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(apiService, tokenStore, tokenHolder)
    }

    val photoUploader: PhotoUploader by lazy {
        PhotoUploader(database, apiService, BuildConfig.XQX_API_BASE)
    }

    val syncEngine: SyncEngine by lazy {
        SyncEngine(database, apiService, tokenStore, photoUploader)
    }

    val spaceRepository: SpaceRepository by lazy {
        SpaceRepository(database, apiService)
    }
}
