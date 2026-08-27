package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.model.MoodTag
import com.xiaoquexing.app.data.model.StatusTag
import com.xiaoquexing.app.util.AchievementTrigger
import com.xiaoquexing.app.util.GPCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RecordUiState(
    val text: String = "",
    val selectedMood: String? = null,
    val selectedStatusTags: Set<String> = emptySet(),
    val photoUris: List<String> = emptyList(),
    val voiceUri: String? = null,
    val voiceDuration: Long = 0,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicUri: String? = null,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkSummary: String? = null,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val isRecording: Boolean = false,
    val isPublishing: Boolean = false,
    val showGpAnimation: Boolean = false,
    val earnedGp: Int = 0,
    val estimatedGp: Int = 0,
    val todayGp: Int = 0,
    val currentStreak: Int = 0,
    val errorMessage: String? = null
)

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoQueXingApp
    private val recordRepo = app.recordRepository
    private val plantRepo = app.plantRepository
    private val achievementRepo = app.achievementRepository
    private val trigger = AchievementTrigger(recordRepo, plantRepo, achievementRepo)

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    val moodTags = MoodTag.defaults
    val statusTags = StatusTag.defaults

    init {
        viewModelScope.launch {
            recordRepo.getTodayGp().collect { todayGp ->
                _uiState.value = _uiState.value.copy(todayGp = todayGp)
                recalculateEstimatedGp()
            }
        }
        viewModelScope.launch {
            // 初始加载一次 streak
            refreshStreak()
        }
    }

    suspend fun refreshStreak() {
        runCatching {
            val s = recordRepo.calculateStreakDays()
            _uiState.value = _uiState.value.copy(currentStreak = s)
            recalculateEstimatedGp()
        }
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(text = text)
        recalculateEstimatedGp()
    }

    fun selectMood(mood: String?) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
        recalculateEstimatedGp()
    }

    fun toggleStatusTag(tag: String) {
        val current = _uiState.value.selectedStatusTags.toMutableSet()
        if (current.contains(tag)) current.remove(tag) else current.add(tag)
        _uiState.value = _uiState.value.copy(selectedStatusTags = current)
        recalculateEstimatedGp()
    }

    fun addPhoto(uri: String) {
        if (_uiState.value.photoUris.size < 9) {
            _uiState.value = _uiState.value.copy(photoUris = _uiState.value.photoUris + uri)
            recalculateEstimatedGp()
        }
    }

    fun addPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        val current = _uiState.value.photoUris
        val space = 9 - current.size
        if (space <= 0) return
        val merged = (current + uris.take(space)).distinct()
        _uiState.value = _uiState.value.copy(photoUris = merged)
        recalculateEstimatedGp()
    }

    fun removePhoto(index: Int) {
        _uiState.value = _uiState.value.copy(
            photoUris = _uiState.value.photoUris.toMutableList().apply { removeAt(index) }
        )
        recalculateEstimatedGp()
    }

    fun setVoiceRecording(recording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = recording)
    }

    fun setVoice(uri: String, durationMs: Long) {
        _uiState.value = _uiState.value.copy(
            voiceUri = uri, voiceDuration = durationMs, isRecording = false
        )
        recalculateEstimatedGp()
    }

    fun removeVoice() {
        _uiState.value = _uiState.value.copy(voiceUri = null, voiceDuration = 0)
        recalculateEstimatedGp()
    }

    fun setMusic(title: String, artist: String, uri: String? = null) {
        _uiState.value = _uiState.value.copy(musicTitle = title, musicArtist = artist, musicUri = uri)
        recalculateEstimatedGp()
    }

    fun removeMusic() {
        _uiState.value = _uiState.value.copy(musicTitle = null, musicArtist = null, musicUri = null)
        recalculateEstimatedGp()
    }

    fun setLink(url: String, title: String? = null, summary: String? = null) {
        _uiState.value = _uiState.value.copy(linkUrl = url, linkTitle = title, linkSummary = summary)
        recalculateEstimatedGp()
    }

    fun removeLink() {
        _uiState.value = _uiState.value.copy(linkUrl = null, linkTitle = null, linkSummary = null)
        recalculateEstimatedGp()
    }

    fun setLocation(name: String, lat: Double? = null, lng: Double? = null) {
        _uiState.value = _uiState.value.copy(locationName = name, locationLat = lat, locationLng = lng)
        recalculateEstimatedGp()
    }

    fun removeLocation() {
        _uiState.value = _uiState.value.copy(locationName = null, locationLat = null, locationLng = null)
        recalculateEstimatedGp()
    }

    private fun recalculateEstimatedGp() {
        val state = _uiState.value
        val breakdown = GPCalculator.calculate(
            textLength = state.text.length,
            photoCount = state.photoUris.size,
            hasVoice = state.voiceUri != null,
            hasMusic = state.musicTitle != null,
            hasLink = state.linkUrl != null,
            hasLocation = state.locationName != null,
            hasMood = state.selectedMood != null,
            statusTagCount = state.selectedStatusTags.size,
            streakDays = state.currentStreak.coerceAtLeast(1),
            isBackdated = false,
            todayGpSoFar = state.todayGp
        )
        _uiState.value = _uiState.value.copy(estimatedGp = breakdown.finalGp)
    }

    fun publish(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.text.isBlank() && state.photoUris.isEmpty() && state.voiceUri == null
            && state.musicTitle == null && state.linkUrl == null && state.locationName == null
        ) {
            _uiState.value = _uiState.value.copy(errorMessage = "请至少记录一些内容～")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true)

            // 重新算一次 streak —— 可能用户在 RecordScreen 停留期间又过了零点
            val streak = runCatching { recordRepo.calculateStreakDays() }.getOrDefault(state.currentStreak)

            val breakdown = GPCalculator.calculate(
                textLength = state.text.length,
                photoCount = state.photoUris.size,
                hasVoice = state.voiceUri != null,
                hasMusic = state.musicTitle != null,
                hasLink = state.linkUrl != null,
                hasLocation = state.locationName != null,
                hasMood = state.selectedMood != null,
                statusTagCount = state.selectedStatusTags.size,
                streakDays = streak.coerceAtLeast(1),
                isBackdated = false,
                todayGpSoFar = state.todayGp
            )

            val record = Record(
                text = state.text,
                moodTag = state.selectedMood,
                statusTags = state.selectedStatusTags.joinToString(","),
                photoUris = state.photoUris.joinToString("|"),
                voiceUri = state.voiceUri,
                voiceDuration = state.voiceDuration,
                musicTitle = state.musicTitle,
                musicArtist = state.musicArtist,
                musicUri = state.musicUri,
                linkUrl = state.linkUrl,
                linkTitle = state.linkTitle,
                linkSummary = state.linkSummary,
                locationName = state.locationName,
                locationLat = state.locationLat,
                locationLng = state.locationLng,
                gpEarned = breakdown.finalGp,
                createdAt = System.currentTimeMillis(),
                isBackdated = false
            )

            recordRepo.insert(record)

            if (breakdown.finalGp > 0) {
                plantRepo.addGpToActive(breakdown.finalGp)
            }

            // 触发成就 + 植物解锁（best-effort）
            trigger.onRecordPublished()

            // 重新拉 streak 给用户看（连续 +1 的体感）
            val newStreak = runCatching { recordRepo.calculateStreakDays() }.getOrDefault(streak)
            _uiState.value = _uiState.value.copy(
                currentStreak = newStreak,
                isPublishing = false,
                showGpAnimation = true,
                earnedGp = breakdown.finalGp
            )

            kotlinx.coroutines.delay(1500)
            onSuccess()
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
