package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.model.MoodTag
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AlbumSnapshot(
    val id: Long,
    val title: String,
    val dateRange: String,
    val recordCount: Int,
    val totalGp: Int,
    val plantLabel: String,
    val days: List<Pair<String, Int>>,
    val moods: List<Pair<String, Int>>,
    val tags: List<Pair<String, Int>>,
    val locations: List<Pair<String, Int>>,
    val music: List<String>,
    val links: List<String>,
)

object AlbumWindows {
    const val WEEK = 7L
    const val MONTH = 30L
    const val ALL = 0L
}

fun recordsInWindow(records: List<Record>, windowId: Long, now: Long = System.currentTimeMillis()): List<Record> {
    return when (windowId) {
        AlbumWindows.WEEK -> {
            val start = startOfDay(now) - 6L * 24 * 3600 * 1000
            records.filter { it.createdAt >= start }
        }
        AlbumWindows.MONTH -> {
            val cal = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.DAY_OF_MONTH, 1) }
            val start = startOfDay(cal.timeInMillis)
            records.filter { it.createdAt >= start }
        }
        else -> records
    }
}

fun buildAlbumSnapshot(
    id: Long,
    title: String,
    records: List<Record>,
    plantName: String,
): AlbumSnapshot {
    val ordered = records.sortedBy { it.createdAt }
    val gp = ordered.sumOf { it.gpEarned }
    val stage = PlantStage.fromGp(gp)
    val dayFmt = SimpleDateFormat("M月d日", Locale.CHINESE)
    val days = ordered.groupBy { dayFmt.format(Date(it.createdAt)) }
        .map { (day, rows) -> day to rows.sumOf { it.gpEarned } }
    val moods = ordered.mapNotNull { it.moodTag }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { (name, count) ->
            val emoji = MoodTag.fromName(name)?.emoji.orEmpty()
            val label = if (emoji.isBlank()) name else "$emoji $name"
            label to count
        }
    val tags = ordered.flatMap { it.getStatusTagList() }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }
    val locations = ordered.mapNotNull { it.locationName }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }
    val music = ordered.mapNotNull { rec ->
        rec.musicTitle?.let { title ->
            if (rec.musicArtist.isNullOrBlank()) title else "$title - ${rec.musicArtist}"
        }
    }.distinct()
    val links = ordered.mapNotNull { it.linkUrl }.filter { it.isNotBlank() }.distinct()
    val range = when {
        ordered.isEmpty() -> "暂无记录"
        ordered.size == 1 -> dayFmt.format(Date(ordered.first().createdAt))
        else -> "${dayFmt.format(Date(ordered.first().createdAt))} - ${dayFmt.format(Date(ordered.last().createdAt))}"
    }
    return AlbumSnapshot(
        id = id,
        title = title,
        dateRange = range,
        recordCount = ordered.size,
        totalGp = gp,
        plantLabel = "$plantName · ${stage.displayName}",
        days = days,
        moods = moods,
        tags = tags,
        locations = locations,
        music = music,
        links = links,
    )
}

private fun startOfDay(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
