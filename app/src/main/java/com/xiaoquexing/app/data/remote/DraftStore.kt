package com.xiaoquexing.app.data.remote

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecordDraft(
    val text: String = "",
    val mood: String? = null,
    val statusTags: List<String> = emptyList(),
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
    val occurredAt: Long = 0,
)

class DraftStore(context: Context) {
    private val prefs = context.getSharedPreferences("xqx_draft", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): RecordDraft? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString(RecordDraft.serializer(), raw) }.getOrNull()
    }

    fun save(draft: RecordDraft) {
        if (isEmpty(draft)) {
            clear()
            return
        }
        prefs.edit().putString(KEY, json.encodeToString(RecordDraft.serializer(), draft)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "record_draft"
        fun isEmpty(draft: RecordDraft): Boolean =
            draft.text.isBlank() &&
                draft.mood == null &&
                draft.statusTags.isEmpty() &&
                draft.photoUris.isEmpty() &&
                draft.voiceUri == null &&
                draft.musicTitle == null &&
                draft.linkUrl == null &&
                draft.locationName == null
    }
}
