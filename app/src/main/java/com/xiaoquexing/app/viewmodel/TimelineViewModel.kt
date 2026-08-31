package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.model.MoodTag
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val recordsByDate: Map<String, List<Record>> = emptyMap(),
    val totalCount: Int = 0,
    val visibleCount: Int = 0,
    val hasMore: Boolean = false,
    val query: TimelineQuery = TimelineQuery(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

class TimelineViewModel(private val recordRepo: RecordRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    val moodTags = MoodTag.defaults

    private var source: List<Record> = emptyList()

    init {
        viewModelScope.launch {
            recordRepo.getAllRecords().collect { records ->
                source = records
                publish()
            }
        }
    }

    fun onSearch(text: String) = updateQuery { copy(search = text, visibleLimit = TimelineQuery.PAGE_SIZE) }

    fun onMood(mood: String?) = updateQuery {
        copy(mood = if (this.mood == mood) null else mood, visibleLimit = TimelineQuery.PAGE_SIZE)
    }

    fun togglePhotos() = updateQuery { copy(photosOnly = !photosOnly, visibleLimit = TimelineQuery.PAGE_SIZE) }

    fun toggleVoice() = updateQuery { copy(voiceOnly = !voiceOnly, visibleLimit = TimelineQuery.PAGE_SIZE) }

    fun clearFilters() = updateQuery { TimelineQuery() }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore) return
        updateQuery { copy(visibleLimit = visibleLimit + TimelineQuery.PAGE_SIZE) }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            publish(refreshing = false)
        }
    }

    private fun updateQuery(block: TimelineQuery.() -> TimelineQuery) {
        _uiState.value = _uiState.value.copy(query = _uiState.value.query.block())
        publish()
    }

    private fun publish(refreshing: Boolean = _uiState.value.isRefreshing) {
        val query = _uiState.value.query
        val filtered = filterRecords(source, query)
        val page = paginate(filtered, query.visibleLimit)
        _uiState.value = _uiState.value.copy(
            recordsByDate = groupRecordsByDate(page),
            totalCount = filtered.size,
            visibleCount = page.size,
            hasMore = page.size < filtered.size,
            isLoading = false,
            isRefreshing = refreshing,
        )
    }
}
