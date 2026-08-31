package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.remote.ApiException
import com.xiaoquexing.app.data.remote.Session
import com.xiaoquexing.app.data.remote.SessionRepository
import com.xiaoquexing.app.data.remote.SyncEngine
import com.xiaoquexing.app.data.remote.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val session: Session? = null,
    val account: String = "",
    val password: String = "",
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val syncing: Boolean = false,
    val message: String? = null,
    val lastSynced: Int = 0,
)

class AuthViewModel(
    private val sessions: SessionRepository,
    private val tokens: TokenStore,
    private val sync: SyncEngine,
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            tokens.session.collect { s ->
                _ui.value = _ui.value.copy(session = s)
            }
        }
    }

    fun onAccount(v: String) {
        _ui.value = _ui.value.copy(account = v.trim().take(32))
    }

    fun onPassword(v: String) {
        _ui.value = _ui.value.copy(password = v.take(72))
    }

    fun register() = authenticate(register = true)
    fun login() = authenticate(register = false)

    private fun authenticate(register: Boolean) {
        val snapshot = _ui.value
        if (snapshot.account.length < 3) {
            _ui.value = snapshot.copy(message = "账号至少 3 位字母数字或下划线")
            return
        }
        if (snapshot.password.length < 6) {
            _ui.value = snapshot.copy(message = "密码至少 6 位")
            return
        }
        viewModelScope.launch {
            _ui.value = snapshot.copy(verifying = true, sending = register, message = null)
            val call = if (register) {
                { sessions.register(snapshot.account, snapshot.password) }
            } else {
                { sessions.login(snapshot.account, snapshot.password) }
            }
            runCatching { call() }
                .onSuccess {
                    val report = runCatching { sync.syncAll() }.getOrDefault(com.xiaoquexing.app.data.remote.SyncReport())
                    _ui.value = _ui.value.copy(
                        verifying = false,
                        sending = false,
                        lastSynced = report.pushed,
                        message = report.error ?: "已登录，上传 ${report.pushed} 条" + if (report.conflicts > 0) "，${report.conflicts} 条冲突" else "",
                    )
                }
                .onFailure {
                    val msg = (it as? ApiException)?.err?.message ?: it.message ?: "失败"
                    _ui.value = _ui.value.copy(verifying = false, sending = false, message = msg)
                }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, message = null)
            val report = runCatching { sync.syncAll() }.getOrElse {
                _ui.value = _ui.value.copy(syncing = false, message = it.message)
                return@launch
            }
            _ui.value = _ui.value.copy(
                syncing = false,
                lastSynced = report.pushed,
                message = report.error ?: "已同步 ${report.pushed} 条 / 拉取 ${report.pulled} 条",
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { sessions.logout() }
            _ui.value = _ui.value.copy(message = "已退出")
        }
    }
}
