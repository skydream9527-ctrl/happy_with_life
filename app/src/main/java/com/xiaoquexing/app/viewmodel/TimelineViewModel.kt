package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.data.entity.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val recordsByDate: Map<String, List<Record>> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoQueXingApp
    private val recordRepo = app.recordRepository

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            recordRepo.getAllRecords().collect { records ->
                val grouped = records.groupBy { record ->
                    java.text.SimpleDateFormat("yyyy年M月d日 EEEE", java.util.Locale.CHINESE)
                        .format(java.util.Date(record.createdAt))
                }
                _uiState.value = _uiState.value.copy(
                    recordsByDate = grouped,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        // Data is updated via Room Flow, just simulate refresh
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
}
