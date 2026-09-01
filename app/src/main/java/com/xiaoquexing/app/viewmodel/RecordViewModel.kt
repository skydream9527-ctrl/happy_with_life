package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.media.MediaImporter
import com.xiaoquexing.app.data.model.MoodTag
import com.xiaoquexing.app.data.model.StatusTag
import com.xiaoquexing.app.data.remote.DraftStore
import com.xiaoquexing.app.data.remote.RecordDraft
import com.xiaoquexing.app.data.remote.SyncEngine
import com.xiaoquexing.app.data.repository.RecordRepository
import com.xiaoquexing.app.util.DateKeys
import com.xiaoquexing.app.util.GPCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** 发生时间；0 = 现在。补记入口由 UI（M1-01）写入，365 天窗口由仓储层校验 */
    val occurredAt: Long = 0L,
    val isRecording: Boolean = false,
    val isPublishing: Boolean = false,
    val showGpAnimation: Boolean = false,
    val earnedGp: Int = 0,
    val estimatedGp: Int = 0,
    val todayGp: Int = 0,
    val currentStreak: Int = 0,
    val errorMessage: String? = null,
    val editingId: Long = 0L,
    val isDeleting: Boolean = false,
    val mediaRetryId: Long = 0L,
    val mediaHint: String? = null,
    val draftRestored: Boolean = false,
)

class RecordViewModel(
    private val recordRepo: RecordRepository,
    private val mediaImporter: MediaImporter,
    private val syncEngine: SyncEngine? = null,
    private val drafts: DraftStore? = null,
) : ViewModel() {

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
        restoreDraftIfNeeded()
    }

    private fun restoreDraftIfNeeded() {
        if (_uiState.value.editingId > 0) return
        val draft = drafts?.load() ?: return
        if (DraftStore.isEmpty(draft)) return
        _uiState.value = applyDraft(_uiState.value, draft).copy(draftRestored = true)
        recalculateEstimatedGp()
    }

    fun discardDraft() {
        drafts?.clear()
        if (_uiState.value.editingId > 0) return
        _uiState.value = RecordUiState(
            todayGp = _uiState.value.todayGp,
            currentStreak = _uiState.value.currentStreak,
        )
        recalculateEstimatedGp()
    }

    private fun persistDraft() {
        if (_uiState.value.editingId > 0) return
        drafts?.save(snapshotDraft(_uiState.value))
    }

    suspend fun refreshStreak() {
        runCatching {
            val s = recordRepo.calculateStreakDays()
            _uiState.value = _uiState.value.copy(currentStreak = s)
            recalculateEstimatedGp()
        }
    }

    fun applyAssistant() {
        val state = _uiState.value
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val line = com.xiaoquexing.app.util.NoteAssistant.suggest(
            com.xiaoquexing.app.util.CaptionHint(
                mood = state.selectedMood,
                text = state.text,
                location = state.locationName,
                hour = hour,
            )
        )
        updateText(line)
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(text = text)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun selectMood(mood: String?) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun toggleStatusTag(tag: String) {
        val current = _uiState.value.selectedStatusTags.toMutableSet()
        if (current.contains(tag)) current.remove(tag) else current.add(tag)
        _uiState.value = _uiState.value.copy(selectedStatusTags = current)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun addPhoto(uri: String) = addPhotos(listOf(uri))

    fun addPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        val current = _uiState.value.photoUris
        val space = 9 - current.size
        if (space <= 0) return
        val incoming = uris.take(space)
        val merged = (current + incoming).distinct()
        _uiState.value = _uiState.value.copy(photoUris = merged, mediaHint = "正在校正方向并压缩…")
        recalculateEstimatedGp()
        viewModelScope.launch {
            val normalized = incoming.map { src -> mediaImporter.persistPreview(src) ?: src }
            val withoutRaw = (_uiState.value.photoUris - incoming.toSet()) + normalized
            _uiState.value = _uiState.value.copy(
                photoUris = withoutRaw.distinct().take(9),
                mediaHint = "照片已压缩并校正方向",
            )
            persistDraft()
        }
    }

    fun removePhoto(index: Int) {
        _uiState.value = _uiState.value.copy(
            photoUris = _uiState.value.photoUris.toMutableList().apply { removeAt(index) }
        )
        recalculateEstimatedGp()
        persistDraft()
    }

    fun setVoiceRecording(recording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = recording)
    }

    fun setVoice(uri: String, durationMs: Long) {
        val accepted = com.xiaoquexing.app.data.media.VoiceFiles.accept(uri, durationMs)
        if (!accepted.ok) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                errorMessage = accepted.reason ?: "录音失败",
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            voiceUri = accepted.path,
            voiceDuration = accepted.durationMs,
            isRecording = false,
        )
        recalculateEstimatedGp()
        persistDraft()
    }

    fun removeVoice() {
        _uiState.value = _uiState.value.copy(voiceUri = null, voiceDuration = 0)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun setMusic(title: String, artist: String, uri: String? = null) {
        _uiState.value = _uiState.value.copy(musicTitle = title, musicArtist = artist, musicUri = uri)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun removeMusic() {
        _uiState.value = _uiState.value.copy(musicTitle = null, musicArtist = null, musicUri = null)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun setLink(url: String, title: String? = null, summary: String? = null) {
        _uiState.value = _uiState.value.copy(linkUrl = url, linkTitle = title, linkSummary = summary)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun removeLink() {
        _uiState.value = _uiState.value.copy(linkUrl = null, linkTitle = null, linkSummary = null)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun setLocation(name: String, lat: Double? = null, lng: Double? = null) {
        _uiState.value = _uiState.value.copy(locationName = name, locationLat = lat, locationLng = lng)
        recalculateEstimatedGp()
        persistDraft()
    }

    fun removeLocation() {
        _uiState.value = _uiState.value.copy(locationName = null, locationLat = null, locationLng = null)
        recalculateEstimatedGp()
        persistDraft()
    }

    /** 补记入口（Z1-04）：传入发生时间；窗口与未来时间由仓储层强制校验。 */
    fun setOccurredAt(timestampMs: Long) {
        _uiState.value = _uiState.value.copy(occurredAt = timestampMs)
        recalculateEstimatedGp()
    }

    fun clearOccurredAt() {
        _uiState.value = _uiState.value.copy(occurredAt = 0L)
        recalculateEstimatedGp()
    }

    fun load(recordId: Long) {
        if (recordId <= 0) return
        viewModelScope.launch {
            val row = recordRepo.getRecordById(recordId) ?: run {
                _uiState.value = _uiState.value.copy(errorMessage = "记录不存在或已删除")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                editingId = recordId,
                text = row.text,
                selectedMood = row.moodTag,
                selectedStatusTags = row.getStatusTagList().toSet(),
                photoUris = row.getPhotoUriList(),
                voiceUri = row.voiceUri,
                voiceDuration = row.voiceDuration,
                musicTitle = row.musicTitle,
                musicArtist = row.musicArtist,
                musicUri = row.musicUri,
                linkUrl = row.linkUrl,
                linkTitle = row.linkTitle,
                linkSummary = row.linkSummary,
                locationName = row.locationName,
                locationLat = row.locationLat,
                locationLng = row.locationLng,
                occurredAt = row.createdAt,
            )
            recalculateEstimatedGp()
        }
    }

    private fun estimateIsBackdated(): Boolean {
        val occurred = _uiState.value.occurredAt
        return occurred > 0 && DateKeys.epochDay(occurred) != DateKeys.epochDay(System.currentTimeMillis())
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
            isBackdated = estimateIsBackdated(),
            remainingQuota = (GPCalculator.DAILY_GP_LIMIT - state.todayGp).coerceAtLeast(0)
        )
        _uiState.value = _uiState.value.copy(estimatedGp = breakdown.finalGp)
    }

    fun publish(onSuccess: () -> Unit) {
        val state = _uiState.value
        val content = com.xiaoquexing.app.util.PublishGuard.hasContent(
            text = state.text,
            photoCount = state.photoUris.size,
            hasVoice = state.voiceUri != null,
            hasMusic = state.musicTitle != null,
            hasLink = state.linkUrl != null,
            hasLocation = state.locationName != null,
        )
        com.xiaoquexing.app.util.PublishGuard.missingHint(state.selectedMood, content)?.let { hint ->
            _uiState.value = state.copy(errorMessage = hint)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true)

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
                // 领域 Record 的 createdAt 即发生时间语义；仓储层映射为 occurredAt
                createdAt = state.occurredAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                isBackdated = false
            )

            // 记录、媒体、标签、当日额度、空间 GP、植物阶段、成就、Outbox 在同一事务内
            // 原子写入（Z1-02）；GP 由事务内按数据库当前额度计算，避免过期读数
            val result = runCatching {
                if (state.editingId > 0) recordRepo.editRecord(state.editingId, record)
                else recordRepo.publish(record)
            }.getOrElse { e ->
                _uiState.value = _uiState.value.copy(
                    isPublishing = false,
                    errorMessage = "保存失败，请重试（${e.message}）"
                )
                return@launch
            }

            // content:// 照片复制到私有目录（K5）：事务成功后的独立步骤，失败置 MISSING 可重试
            viewModelScope.launch {
                val copied = runCatching { mediaImporter.importPending(result.recordId) }.getOrDefault(0)
                val failed = state.photoUris.isNotEmpty() && copied < state.photoUris.size
                if (failed) {
                    _uiState.value = _uiState.value.copy(
                        mediaRetryId = result.recordId,
                        mediaHint = "有照片导入失败，可重试",
                    )
                }
                runCatching { syncEngine?.syncAll(retries = 2) }
            }

            drafts?.clear()
            _uiState.value = _uiState.value.copy(
                currentStreak = result.streakDays,
                isPublishing = false,
                showGpAnimation = true,
                earnedGp = result.earnedGp,
                draftRestored = false,
            )

            kotlinx.coroutines.delay(900)
            onSuccess()
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun retryFailedPhotos() {
        val id = _uiState.value.mediaRetryId
        if (id <= 0) return
        viewModelScope.launch {
            val copied = runCatching { mediaImporter.retryMissing(id) }.getOrDefault(0)
            _uiState.value = _uiState.value.copy(
                mediaRetryId = if (copied > 0) 0 else id,
                mediaHint = if (copied > 0) "照片已重新导入" else "仍失败，请检查相册权限后再试",
            )
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _uiState.value.editingId
        if (id <= 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            runCatching { recordRepo.softDelete(id) }
                .onSuccess {
                    runCatching { syncEngine?.syncAll(retries = 1) }
                    onDone()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = it.message ?: "删除失败",
                    )
                }
        }
    }

    private fun snapshotDraft(state: RecordUiState) = RecordDraft(
        text = state.text,
        mood = state.selectedMood,
        statusTags = state.selectedStatusTags.toList(),
        photoUris = state.photoUris,
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
        occurredAt = state.occurredAt,
    )

    private fun applyDraft(state: RecordUiState, draft: RecordDraft) = state.copy(
        text = draft.text,
        selectedMood = draft.mood,
        selectedStatusTags = draft.statusTags.toSet(),
        photoUris = draft.photoUris,
        voiceUri = draft.voiceUri,
        voiceDuration = draft.voiceDuration,
        musicTitle = draft.musicTitle,
        musicArtist = draft.musicArtist,
        musicUri = draft.musicUri,
        linkUrl = draft.linkUrl,
        linkTitle = draft.linkTitle,
        linkSummary = draft.linkSummary,
        locationName = draft.locationName,
        locationLat = draft.locationLat,
        locationLng = draft.locationLng,
        occurredAt = draft.occurredAt,
    )
}
