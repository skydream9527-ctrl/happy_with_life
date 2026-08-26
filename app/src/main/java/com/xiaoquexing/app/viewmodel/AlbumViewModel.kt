package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.data.entity.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Album(
    val id: Long,
    val title: String,
    val dateRange: String,
    val recordCount: Int,
    val coverRecord: Record? = null
)

data class AlbumUiState(
    val albums: List<Album> = emptyList(),
    val showCreateDialog: Boolean = false
)

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoQueXingApp

    private val _uiState = MutableStateFlow(AlbumUiState(
        albums = listOf(
            Album(
                id = 1,
                title = "我的第一本周记",
                dateRange = "最近7天",
                recordCount = 5
            ),
            Album(
                id = 2,
                title = "2024年1月精选",
                dateRange = "2024.1.1 - 2024.1.31",
                recordCount = 12
            )
        )
    ))
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createAlbum(title: String) {
        // Demo: just add a new album
        val newAlbum = Album(
            id = System.currentTimeMillis(),
            title = title,
            dateRange = "今天",
            recordCount = 0
        )
        _uiState.value = _uiState.value.copy(
            albums = _uiState.value.albums + newAlbum,
            showCreateDialog = false
        )
    }
}
