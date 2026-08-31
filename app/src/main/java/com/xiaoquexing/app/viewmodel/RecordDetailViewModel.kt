package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordDetailUi(
    val record: Record? = null,
    val gone: Boolean = false,
    val message: String? = null,
)

class RecordDetailViewModel(
    private val recordRepo: RecordRepository,
) : ViewModel() {
    private var recordId: Long = 0
    private val _ui = MutableStateFlow(RecordDetailUi())
    val uiState: StateFlow<RecordDetailUi> = _ui.asStateFlow()

    fun load(id: Long) {
        if (id == recordId && _ui.value.record != null) return
        recordId = id
        viewModelScope.launch {
            val row = recordRepo.getRecordById(id)
            _ui.value = if (row == null) RecordDetailUi(gone = true) else RecordDetailUi(record = row)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { recordRepo.softDelete(recordId) }
                .onSuccess {
                    _ui.value = RecordDetailUi(gone = true, message = "已删除")
                    onDone()
                }
                .onFailure { _ui.value = _ui.value.copy(message = it.message) }
        }
    }
}
