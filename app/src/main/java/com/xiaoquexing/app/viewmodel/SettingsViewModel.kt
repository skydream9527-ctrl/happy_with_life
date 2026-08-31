package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.xiaoquexing.app.data.backup.LocalBackup
import com.xiaoquexing.app.data.remote.ApiService
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
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
    private val records: RecordRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(refresh(loggedIn = false))
    val uiState: StateFlow<SettingsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            tokens.session.collect { session ->
                _ui.value = refresh(loggedIn = session != null)
            }
        }
    }

    fun exportBackup(dest: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val rows = records.getAllRecords().first()
                    val tmp = File(app.cacheDir, "xqx-backup.zip")
                    LocalBackup.pack(app, rows, tmp)
                    app.contentResolver.openOutputStream(dest)?.use { out ->
                        tmp.inputStream().use { it.copyTo(out) }
                    } ?: error("无法写入所选位置")
                    rows.size
                }
            }.onSuccess { n ->
                _ui.value = _ui.value.copy(message = "已导出 $n 条")
            }.onFailure {
                _ui.value = _ui.value.copy(message = it.message ?: "导出失败")
            }
        }
    }

    fun importBackup(src: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val existing = records.getAllRecords().first()
                        .map { LocalBackup.fingerprint(it.text, it.createdAt) }
                        .toSet()
                    val (envelope, dir) = LocalBackup.unpack(app, src)
                    var added = 0
                    var skipped = 0
                    envelope.records.forEach { row ->
                        if (LocalBackup.shouldSkip(existing, row)) {
                            skipped++
                        } else {
                            records.publish(LocalBackup.toDraft(row, dir))
                            added++
                        }
                    }
                    added to skipped
                }
            }.onSuccess { (added, skipped) ->
                _ui.value = _ui.value.copy(message = "恢复 $added 条，跳过 $skipped 条重复")
            }.onFailure {
                _ui.value = _ui.value.copy(message = it.message ?: "恢复失败")
            }
        }
    }

    fun setReminder(on: Boolean) {
        ReminderScheduler.setEnabled(app, on)
        _ui.value = refresh(_ui.value.loggedIn)
    }

    fun setAnalyticsOff(on: Boolean) {
        store.analyticsOff = on
        _ui.value = refresh(_ui.value.loggedIn)
    }

    fun setHidePhone(on: Boolean) {
        store.hidePhone = on
        _ui.value = refresh(_ui.value.loggedIn)
    }

    fun setTheme(mode: String) {
        Appearance.setMode(store, mode)
        _ui.value = refresh(_ui.value.loggedIn)
    }

    fun setLargeText(on: Boolean) {
        Appearance.setLargeText(store, on)
        _ui.value = refresh(_ui.value.loggedIn)
    }

    fun requestDelete() {
        store.requestDelete()
        _ui.value = refresh(_ui.value.loggedIn).copy(message = "已进入 7 天冷静期，到期前可取消")
    }

    fun cancelDelete() {
        store.cancelDelete()
        _ui.value = refresh(_ui.value.loggedIn).copy(message = "已取消注销申请")
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val loggedIn = tokens.current() != null
            if (loggedIn) {
                val err = runCatching { api.deleteAccount() }.exceptionOrNull()
                runCatching { sessions.logout() }
                if (err != null) {
                    _ui.value = refresh(_ui.value.loggedIn).copy(message = "已退出登录。云端删除稍后重试：${err.message}")
                    return@launch
                }
            }
            store.cancelDelete()
            _ui.value = refresh(_ui.value.loggedIn).copy(message = "账号已申请删除，本地登录已清除")
        }
    }

    private fun refresh(loggedIn: Boolean = _ui.value.loggedIn): SettingsUi {
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
            loggedIn = loggedIn,
            deleteRequested = start > 0,
            coolingOver = start > 0 && now >= until,
            purgeLabel = if (start > 0) fmt.format(Date(until)) else null,
        )
    }
}
