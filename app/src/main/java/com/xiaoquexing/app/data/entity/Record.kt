package com.xiaoquexing.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String = "",
    val moodTag: String? = null,
    val statusTags: String = "",
    val photoUris: String = "",
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
    val gpEarned: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isBackdated: Boolean = false
) {
    fun getStatusTagList(): List<String> {
        return if (statusTags.isBlank()) emptyList() else statusTags.split(",")
    }

    fun getPhotoUriList(): List<String> {
        return if (photoUris.isBlank()) emptyList() else photoUris.split("|")
    }

    fun getContentCount(): Int {
        var count = 0
        if (text.isNotBlank()) count++
        if (photoUris.isNotBlank()) count += getPhotoUriList().size
        if (voiceUri != null) count++
        if (musicTitle != null) count++
        if (linkUrl != null) count++
        if (locationName != null) count++
        return count
    }
}
