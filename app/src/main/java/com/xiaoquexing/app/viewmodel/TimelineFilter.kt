package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineQuery(
    val search: String = "",
    val mood: String? = null,
    val photosOnly: Boolean = false,
    val voiceOnly: Boolean = false,
    val visibleLimit: Int = PAGE_SIZE,
) {
    val active: Boolean
        get() = search.isNotBlank() || mood != null || photosOnly || voiceOnly

    companion object {
        const val PAGE_SIZE = 30
    }
}

fun filterRecords(records: List<Record>, query: TimelineQuery): List<Record> {
    val needle = query.search.trim()
    return records.asSequence()
        .filter { row ->
            if (query.mood != null && row.moodTag != query.mood) return@filter false
            if (query.photosOnly && row.getPhotoUriList().isEmpty()) return@filter false
            if (query.voiceOnly && row.voiceUri.isNullOrBlank()) return@filter false
            if (needle.isBlank()) return@filter true
            row.text.contains(needle, ignoreCase = true) ||
                (row.moodTag?.contains(needle, ignoreCase = true) == true) ||
                row.getStatusTagList().any { it.contains(needle, ignoreCase = true) } ||
                (row.locationName?.contains(needle, ignoreCase = true) == true)
        }
        .toList()
}

fun groupRecordsByDate(records: List<Record>): Map<String, List<Record>> {
    val fmt = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
    return records.groupBy { fmt.format(Date(it.createdAt)) }
}

fun paginate(records: List<Record>, limit: Int): List<Record> =
    records.take(limit.coerceAtLeast(0))
