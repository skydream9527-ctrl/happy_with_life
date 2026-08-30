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
    val phone: String = "",
    val code: String = "",
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

    fun onPhone(v: String) {
        _ui.value = _ui.value.copy(phone = v.filter { it.isDigit() }.take(11))
    }

    fun onCode(v: String) {
        _ui.value = _ui.value.copy(code = v.filter { it.isDigit() }.take(6))
    }

    fun sendCode() {
        val phone = _ui.value.phone
        if (phone.length != 11) {
            _ui.value = _ui.value.copy(message = "请输入 11 位手机号")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(sending = true, message = null)
            runCatching { sessions.sendCode(phone) }
                .onSuccess {
                    _ui.value = _ui.value.copy(sending = false, message = "验证码已发送（开发环境填 123456）")
                }
                .onFailure {
                    val msg = (it as? ApiException)?.err?.message ?: it.message ?: "发送失败"
                    _ui.value = _ui.value.copy(sending = false, message = msg)
                }
        }
    }

    fun verify() {
        val snapshot = _ui.value
        viewModelScope.launch {
            _ui.value = snapshot.copy(verifying = true, message = null)
            runCatching { sessions.verify(snapshot.phone, snapshot.code) }
                .onSuccess {
                    val n = runCatching { sync.pushPending() }.getOrDefault(0)
                    _ui.value = _ui.value.copy(verifying = false, lastSynced = n, message = "已登录，同步 $n 条")
                }
                .onFailure {
                    val msg = (it as? ApiException)?.err?.message ?: it.message ?: "验证失败"
                    _ui.value = _ui.value.copy(verifying = false, message = msg)
                }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, message = null)
            val n = runCatching { sync.pushPending() }.getOrElse {
                _ui.value = _ui.value.copy(syncing = false, message = it.message)
                return@launch
            }
            _ui.value = _ui.value.copy(syncing = false, lastSynced = n, message = "已同步 $n 条")
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { sessions.logout() }
            _ui.value = _ui.value.copy(message = "已退出")
        }
    }
}
