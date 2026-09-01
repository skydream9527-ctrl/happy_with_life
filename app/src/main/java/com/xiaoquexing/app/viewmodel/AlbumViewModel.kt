package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import com.xiaoquexing.app.ui.album.AlbumExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class Album(
    val id: Long,
    val title: String,
    val dateRange: String,
    val recordCount: Int,
    val coverRecord: Record? = null,
)

data class AlbumUiState(
    val albums: List<Album> = emptyList(),
    val snapshot: AlbumSnapshot? = null,
    val showCreateDialog: Boolean = false,
    val exporting: Boolean = false,
    val message: String? = null,
)

class AlbumViewModel(
    private val app: Application,
    private val recordRepo: RecordRepository,
    private val plantRepo: PlantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    private var source: List<Record> = emptyList()
    private var plantName: String = "小确幸之树"

    init {
        viewModelScope.launch {
            combine(recordRepo.getAllRecords(), plantRepo.getActivePlant()) { records, plant ->
                records to (plant?.plantType?.displayName ?: "小确幸之树")
            }.collect { (records, name) ->
                source = records
                plantName = name
                _uiState.value = _uiState.value.copy(albums = virtualAlbums(records))
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createAlbum(title: String) {
        val snap = buildAlbumSnapshot(System.currentTimeMillis(), title.ifBlank { "我的画册" }, source, plantName)
        _uiState.value = _uiState.value.copy(
            albums = _uiState.value.albums + Album(snap.id, snap.title, snap.dateRange, snap.recordCount),
            showCreateDialog = false,
        )
    }

    fun open(albumId: Long) {
        val named = _uiState.value.albums.find { it.id == albumId }
        val title = named?.title ?: titleOf(albumId)
        val windowed = recordsInWindow(source, if (albumId == AlbumWindows.WEEK || albumId == AlbumWindows.MONTH) albumId else AlbumWindows.ALL)
        _uiState.value = _uiState.value.copy(snapshot = buildAlbumSnapshot(albumId, title, windowed, plantName))
    }

    fun exportImage() {
        val snap = _uiState.value.snapshot ?: run {
            _uiState.value = _uiState.value.copy(message = "画册还没准备好")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true, message = null)
            val ok = runCatching { AlbumExporter.exportLongImage(app, snap) }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                exporting = false,
                message = if (ok) "长图已保存到相册" else "导出长图失败",
            )
        }
    }

    fun exportPdf() {
        val snap = _uiState.value.snapshot ?: run {
            _uiState.value = _uiState.value.copy(message = "画册还没准备好")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true, message = null)
            val ok = runCatching { AlbumExporter.exportPdf(app, snap) }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                exporting = false,
                message = if (ok) "PDF 已保存到下载目录" else "导出 PDF 失败",
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun virtualAlbums(records: List<Record>): List<Album> {
        val week = recordsInWindow(records, AlbumWindows.WEEK)
        val month = recordsInWindow(records, AlbumWindows.MONTH)
        return listOf(
            buildAlbumSnapshot(AlbumWindows.WEEK, "最近一周", week, plantName),
            buildAlbumSnapshot(AlbumWindows.MONTH, "本月", month, plantName),
            buildAlbumSnapshot(AlbumWindows.ALL, "全部小确幸", records, plantName),
        ).map { Album(it.id, it.title, it.dateRange, it.recordCount) }
    }

    private fun titleOf(id: Long) = when (id) {
        AlbumWindows.WEEK -> "最近一周"
        AlbumWindows.MONTH -> "本月"
        else -> "全部小确幸"
    }
}
