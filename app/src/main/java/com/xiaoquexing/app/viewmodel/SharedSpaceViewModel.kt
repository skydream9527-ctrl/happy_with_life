package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import com.xiaoquexing.app.data.remote.ApiException
import com.xiaoquexing.app.data.remote.MemberDto
import com.xiaoquexing.app.data.remote.Session
import com.xiaoquexing.app.data.remote.TokenStore
import com.xiaoquexing.app.data.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SharedSpaceUi(
    val session: Session? = null,
    val spaces: List<SpaceEntity> = emptyList(),
    val members: List<MemberDto> = emptyList(),
    val selectedServerId: String? = null,
    val inviteLink: String? = null,
    val inviteToken: String = "",
    val newName: String = "",
    val message: String? = null,
    val busy: Boolean = false,
)

class SharedSpaceViewModel(
    private val spaces: SpaceRepository,
    private val tokens: TokenStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(SharedSpaceUi())
    val uiState: StateFlow<SharedSpaceUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { tokens.session.collect { _ui.value = _ui.value.copy(session = it) } }
        viewModelScope.launch { spaces.observeSpaces().collect { _ui.value = _ui.value.copy(spaces = it) } }
        refresh()
    }

    fun onName(v: String) { _ui.value = _ui.value.copy(newName = v.take(20)) }
    fun onToken(v: String) { _ui.value = _ui.value.copy(inviteToken = v.trim()) }

    fun refresh() {
        viewModelScope.launch {
            if (_ui.value.session == null) return@launch
            _ui.value = _ui.value.copy(busy = true, message = null)
            runCatching { spaces.refreshFromServer() }
                .onSuccess { _ui.value = _ui.value.copy(busy = false) }
                .onFailure {
                    _ui.value = _ui.value.copy(busy = false, message = it.message)
                }
        }
    }

    fun create() {
        val name = _ui.value.newName.ifBlank { "我们的小确幸" }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            runCatching { spaces.createShared(name) }
                .onSuccess { _ui.value = _ui.value.copy(busy = false, newName = "", message = "已创建「${it.name}」") }
                .onFailure { _ui.value = _ui.value.copy(busy = false, message = err(it)) }
        }
    }

    fun switchTo(localId: Long) {
        viewModelScope.launch {
            spaces.switchTo(localId)
            _ui.value = _ui.value.copy(message = "已切换空间，新记录会记在这里")
        }
    }

    fun invite(serverId: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            runCatching { spaces.invite(serverId) }
                .onSuccess {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        selectedServerId = serverId,
                        inviteLink = it.link.ifBlank { it.token },
                        message = "邀请码：${it.token}",
                    )
                }
                .onFailure { _ui.value = _ui.value.copy(busy = false, message = err(it)) }
        }
    }

    fun loadMembers(serverId: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(selectedServerId = serverId)
            runCatching { spaces.members(serverId) }
                .onSuccess { _ui.value = _ui.value.copy(members = it) }
                .onFailure { _ui.value = _ui.value.copy(message = err(it)) }
        }
    }

    fun accept() {
        val token = _ui.value.inviteToken
        if (token.isBlank()) {
            _ui.value = _ui.value.copy(message = "请粘贴邀请码")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            runCatching { spaces.accept(token) }
                .onSuccess { _ui.value = _ui.value.copy(busy = false, inviteToken = "", message = "已加入「${it.name}」") }
                .onFailure { _ui.value = _ui.value.copy(busy = false, message = err(it)) }
        }
    }

    private fun err(t: Throwable) = (t as? ApiException)?.err?.message ?: t.message ?: "失败"
}
