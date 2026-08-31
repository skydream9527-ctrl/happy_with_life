package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.remote.ApiService
import com.xiaoquexing.app.data.remote.SessionRepository
import com.xiaoquexing.app.data.remote.SettingsStore
import com.xiaoquexing.app.ui.theme.Appearance
import com.xiaoquexing.app.data.remote.TokenStore
import com.xiaoquexing.app.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUi(
    val reminderOn: Boolean = false,
    val analyticsOff: Boolean = false,
    val hidePhone: Boolean = false,
    val themeMode: String = SettingsStore.MODE_SYSTEM,
    val largeText: Boolean = false,
    val loggedIn: Boolean = false,
    val deleteRequested: Boolean = false,
    val coolingOver: Boolean = false,
    val purgeLabel: String? = null,
    val message: String? = null,
)

class SettingsViewModel(
    private val app: Application,
    private val store: SettingsStore,
    private val sessions: SessionRepository,
    private val tokens: TokenStore,
    private val api: ApiService,
) : ViewModel() {

    private val _ui = MutableStateFlow(refresh())
    val uiState: StateFlow<SettingsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            tokens.session.collect { _ui.value = refresh() }
        }
    }

    fun setReminder(on: Boolean) {
        ReminderScheduler.setEnabled(app, on)
        _ui.value = refresh()
    }

    fun setAnalyticsOff(on: Boolean) {
        store.analyticsOff = on
        _ui.value = refresh()
    }

    fun setHidePhone(on: Boolean) {
        store.hidePhone = on
        _ui.value = refresh()
    }

    fun setTheme(mode: String) {
        Appearance.setMode(store, mode)
        _ui.value = refresh()
    }

    fun setLargeText(on: Boolean) {
        Appearance.setLargeText(store, on)
        _ui.value = refresh()
    }

    fun requestDelete() {
        store.requestDelete()
        _ui.value = refresh().copy(message = "已进入 7 天冷静期，到期前可取消")
    }

    fun cancelDelete() {
        store.cancelDelete()
        _ui.value = refresh().copy(message = "已取消注销申请")
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val loggedIn = tokens.current() != null
            if (loggedIn) {
                val err = runCatching { api.deleteAccount() }.exceptionOrNull()
                runCatching { sessions.logout() }
                if (err != null) {
                    _ui.value = refresh().copy(message = "已退出登录。云端删除稍后重试：${err.message}")
                    return@launch
                }
            }
            store.cancelDelete()
            _ui.value = refresh().copy(message = "账号已申请删除，本地登录已清除")
        }
    }

    private fun refresh(): SettingsUi {
        val start = store.deleteRequestedAt
        val until = store.purgeAfter()
        val now = System.currentTimeMillis()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        return SettingsUi(
            reminderOn = ReminderScheduler.isEnabled(app),
            analyticsOff = store.analyticsOff,
            hidePhone = store.hidePhone,
            themeMode = store.themeMode,
            largeText = store.largeText,
            loggedIn = tokens.current() != null,
            deleteRequested = start > 0,
            coolingOver = start > 0 && now >= until,
            purgeLabel = if (start > 0) fmt.format(Date(until)) else null,
        )
    }
}
