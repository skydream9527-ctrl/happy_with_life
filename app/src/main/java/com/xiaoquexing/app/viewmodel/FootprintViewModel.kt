package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FootprintUi(
    val places: List<FootprintPlace> = emptyList(),
    val selected: String? = null,
)

class FootprintViewModel(
    private val recordRepo: RecordRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(FootprintUi())
    val uiState: StateFlow<FootprintUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            recordRepo.getAllRecords().collect { rows ->
                val places = groupFootprints(rows)
                val keep = _ui.value.selected?.takeIf { name -> places.any { it.name == name } }
                _ui.value = FootprintUi(places = places, selected = keep)
            }
        }
    }

    fun open(name: String) {
        _ui.value = _ui.value.copy(selected = name)
    }

    fun close() {
        _ui.value = _ui.value.copy(selected = null)
    }
}
