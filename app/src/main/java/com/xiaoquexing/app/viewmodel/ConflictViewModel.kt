package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.db.entity.RecordEntity
import com.xiaoquexing.app.data.remote.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConflictUi(
    val items: List<RecordEntity> = emptyList(),
    val message: String? = null,
    val busyId: Long? = null,
)

class ConflictViewModel(private val sync: SyncEngine) : ViewModel() {
    private val _ui = MutableStateFlow(ConflictUi())
    val uiState: StateFlow<ConflictUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            sync.observeConflicts().collect { rows ->
                _ui.value = _ui.value.copy(items = rows)
            }
        }
    }

    fun keepLocal(id: Long) = resolve(id) { sync.keepLocal(id) }
    fun keepCloud(id: Long) = resolve(id) { sync.keepCloud(id) }

    private fun resolve(id: Long, block: suspend () -> com.xiaoquexing.app.data.remote.SyncReport) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busyId = id, message = null)
            val report = runCatching { block() }.getOrElse {
                _ui.value = _ui.value.copy(busyId = null, message = it.message)
                return@launch
            }
            _ui.value = _ui.value.copy(
                busyId = null,
                message = report.error ?: if (report.pushed > 0) "已保留本地并上传" else "已采用云端版本",
            )
        }
    }
}
