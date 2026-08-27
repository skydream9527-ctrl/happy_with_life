package com.xiaoquexing.app.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.model.ShareCardData
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import com.xiaoquexing.app.util.PhotoSaver
import com.xiaoquexing.app.util.ShareCardRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ShareSaveState {
    object Idle : ShareSaveState()
    object Saving : ShareSaveState()
    data class Success(val path: String) : ShareSaveState()
    object Failed : ShareSaveState()
}

data class ShareUiState(
    val cardData: ShareCardData? = null,
    val saveState: ShareSaveState = ShareSaveState.Idle
)

class ShareViewModel(
    private val appContext: Context,
    private val recordRepo: RecordRepository,
    private val plantRepo: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)

    fun loadDemo() {
        // 临时 Demo 数据：等后端接入后改为 recordRepo.getRecordById(recordId)
        _uiState.value = _uiState.value.copy(
            cardData = ShareCardData(
                recordText = "今天的咖啡特别好喝，阳光洒在桌面上，是一个安静又美好的下午 ☕️",
                moodEmoji = "😊",
                dateStr = dateFormat.format(Date()),
                plantType = PlantType.TREE,
                plantStage = 4,
                photoUris = emptyList(),
                musicTitle = "Morning Breeze",
                musicArtist = "治愈电台",
                totalGp = 42,
                footerText = "用小确幸记录生活"
            )
        )
    }

    fun loadByRecordId(recordId: Long) {
        viewModelScope.launch {
            val record = recordRepo.getRecordById(recordId)
            if (record == null) {
                loadDemo()
                return@launch
            }
            val activePlant = withContext(Dispatchers.IO) {
                plantRepo.getActivePlant().firstOrNull()
            }
            val totalGp = withContext(Dispatchers.IO) {
                recordRepo.getTotalGp().firstOrNull() ?: 0
            }
            _uiState.value = _uiState.value.copy(
                cardData = ShareCardData(
                    recordText = record.text,
                    moodEmoji = record.moodTag?.let { emojiForMood(it) } ?: "🌱",
                    dateStr = dateFormat.format(Date(record.createdAt)),
                    plantType = activePlant?.plantType ?: PlantType.TREE,
                    plantStage = 4,
                    photoUris = record.getPhotoUriList(),
                    musicTitle = record.musicTitle,
                    musicArtist = record.musicArtist,
                    totalGp = totalGp,
                    footerText = "用小确幸记录生活"
                )
            )
        }
    }

    fun saveToGallery() {
        val data = _uiState.value.cardData ?: return
        _uiState.value = _uiState.value.copy(saveState = ShareSaveState.Saving)
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                ShareCardRenderer.render(data, width = 1080)
            }
            val title = "XiaoQueXing_${System.currentTimeMillis()}"
            val result = PhotoSaver.saveShareCard(appContext, bitmap, title)
            _uiState.value = when (result) {
                is PhotoSaver.SaveResult.Success -> _uiState.value.copy(
                    saveState = ShareSaveState.Success(result.displayPath)
                )
                is PhotoSaver.SaveResult.Failed -> _uiState.value.copy(
                    saveState = ShareSaveState.Failed
                )
            }
        }
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveState = ShareSaveState.Idle)
    }

    private fun emojiForMood(name: String): String = when (name) {
        "开心" -> "😊"
        "平静" -> "😌"
        "感动" -> "🥹"
        "兴奋" -> "🤩"
        "治愈" -> "🌸"
        "满足" -> "🥰"
        "惊喜" -> "🎁"
        "感恩" -> "🙏"
        "放松" -> "🍃"
        else -> "🌱"
    }
}
