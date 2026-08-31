package com.xiaoquexing.app.di

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.ui.share.ShareViewModel
import com.xiaoquexing.app.viewmodel.AlbumViewModel
import com.xiaoquexing.app.viewmodel.AuthViewModel
import com.xiaoquexing.app.viewmodel.ConflictViewModel
import com.xiaoquexing.app.viewmodel.FootprintViewModel
import com.xiaoquexing.app.viewmodel.HomeViewModel
import com.xiaoquexing.app.viewmodel.PlanViewModel
import com.xiaoquexing.app.viewmodel.ProfileViewModel
import com.xiaoquexing.app.viewmodel.RecordDetailViewModel
import com.xiaoquexing.app.viewmodel.RecordViewModel
import com.xiaoquexing.app.viewmodel.ReviewViewModel
import com.xiaoquexing.app.viewmodel.SettingsViewModel
import com.xiaoquexing.app.viewmodel.SharedSpaceViewModel
import com.xiaoquexing.app.viewmodel.TimelineViewModel

/**
 * 手工 ViewModel 工厂（ADR-002）。
 *
 * 注意：NavBackStackEntry 有自己的默认工厂，导航目的地内的 viewModel() 不会走
 * Activity 的 defaultViewModelProviderFactory，因此各 Screen 必须显式传 factory：
 *   viewModel(factory = rememberXiaoQueXingViewModelFactory())
 */
class XiaoQueXingViewModelFactory(
    private val application: Application,
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RecordViewModel::class.java) ->
                RecordViewModel(container.recordRepository, container.mediaImporter, container.syncEngine, container.draftStore)

            modelClass.isAssignableFrom(RecordDetailViewModel::class.java) ->
                RecordDetailViewModel(container.recordRepository)

            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.recordRepository, container.plantRepository, container.tokenStore)

            modelClass.isAssignableFrom(TimelineViewModel::class.java) ->
                TimelineViewModel(container.recordRepository)

            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(
                    container.recordRepository,
                    container.plantRepository,
                    container.achievementRepository,
                    container.syncEngine,
                )

            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(container.sessionRepository, container.tokenStore, container.syncEngine)

            modelClass.isAssignableFrom(SharedSpaceViewModel::class.java) ->
                SharedSpaceViewModel(application, container.spaceRepository, container.tokenStore)

            modelClass.isAssignableFrom(AlbumViewModel::class.java) ->
                AlbumViewModel(application, container.recordRepository, container.plantRepository)

            modelClass.isAssignableFrom(ReviewViewModel::class.java) ->
                ReviewViewModel(application, container.recordRepository)

            modelClass.isAssignableFrom(ConflictViewModel::class.java) ->
                ConflictViewModel(container.syncEngine)

            modelClass.isAssignableFrom(FootprintViewModel::class.java) ->
                FootprintViewModel(container.recordRepository)

            modelClass.isAssignableFrom(PlanViewModel::class.java) ->
                PlanViewModel(container.planStore)

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    application,
                    container.settingsStore,
                    container.sessionRepository,
                    container.tokenStore,
                    container.apiService,
                    container.recordRepository,
                )

            modelClass.isAssignableFrom(ShareViewModel::class.java) ->
                ShareViewModel(
                    application,
                    container.recordRepository,
                    container.plantRepository
                )

            else ->
                throw IllegalArgumentException("未注册的 ViewModel: ${modelClass.name}")
        } as T
    }
}

/** 在 Compose 中获取与当前 Application 绑定的工厂；Activity 重建时记忆复用。 */
@Composable
fun rememberXiaoQueXingViewModelFactory(): XiaoQueXingViewModelFactory {
    val app = LocalContext.current.applicationContext as XiaoQueXingApp
    return remember(app) { XiaoQueXingViewModelFactory(app, app.container) }
}
