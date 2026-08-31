package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import com.xiaoquexing.app.data.remote.ApiException
import com.xiaoquexing.app.data.remote.MemberDto
import com.xiaoquexing.app.data.remote.Session
import com.xiaoquexing.app.data.remote.TokenStore
import com.xiaoquexing.app.data.remote.Anniversary
import com.xiaoquexing.app.data.remote.AnniversaryStore
import com.xiaoquexing.app.data.repository.SpaceRepository
import com.xiaoquexing.app.util.ReminderScheduler
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
    val anniversaries: List<Anniversary> = emptyList(),
    val annTitle: String = "",
    val annMonth: String = "",
    val annDay: String = "",
)

class SharedSpaceViewModel(
    private val app: Application,
    private val spaces: SpaceRepository,
    private val tokens: TokenStore,
) : ViewModel() {
    private val annStore = AnniversaryStore(app)

    private val _ui = MutableStateFlow(SharedSpaceUi())
    val uiState: StateFlow<SharedSpaceUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { tokens.session.collect { _ui.value = _ui.value.copy(session = it) } }
        viewModelScope.launch { spaces.observeSpaces().collect { _ui.value = _ui.value.copy(spaces = it) } }
        _ui.value = _ui.value.copy(anniversaries = annStore.list())
        refresh()
    }

    fun onName(v: String) { _ui.value = _ui.value.copy(newName = v.take(20)) }
    fun onAnnTitle(v: String) { _ui.value = _ui.value.copy(annTitle = v.take(20)) }
    fun onAnnMonth(v: String) { _ui.value = _ui.value.copy(annMonth = v.filter { it.isDigit() }.take(2)) }
    fun onAnnDay(v: String) { _ui.value = _ui.value.copy(annDay = v.filter { it.isDigit() }.take(2)) }

    fun addAnniversary() {
        val month = _ui.value.annMonth.toIntOrNull() ?: 0
        val day = _ui.value.annDay.toIntOrNull() ?: 0
        val item = annStore.add(_ui.value.annTitle, month, day)
        if (item == null) {
            _ui.value = _ui.value.copy(message = "请填写名称和有效月日，例如 5 / 20")
            return
        }
        ReminderScheduler.ensure(app)
        _ui.value = _ui.value.copy(
            anniversaries = annStore.list(),
            annTitle = "",
            annMonth = "",
            annDay = "",
            message = "已添加 ${item.month}月${item.day}日「${item.title}」，当天 21 点会提醒",
        )
    }

    fun removeAnniversary(id: Long) {
        annStore.remove(id)
        ReminderScheduler.ensure(app)
        _ui.value = _ui.value.copy(anniversaries = annStore.list())
    }
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
