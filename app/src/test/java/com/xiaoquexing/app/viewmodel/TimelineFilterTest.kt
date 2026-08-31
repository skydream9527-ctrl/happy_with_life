package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineFilterTest {

    private val rows = listOf(
        Record(id = 1, text = "阳光很好", moodTag = "开心", photoUris = "a.jpg", createdAt = 1_000),
        Record(id = 2, text = "下雨了", moodTag = "平静", statusTags = "独处", createdAt = 2_000),
        Record(id = 3, text = "走神", moodTag = "想念", voiceUri = "v.m4a", createdAt = 3_000),
    )

    @Test
    fun search_matchesTextOrMoodOrTag() {
        assertEquals(1, filterRecords(rows, TimelineQuery(search = "阳光")).size)
        assertEquals(1, filterRecords(rows, TimelineQuery(search = "平静")).size)
        assertEquals(1, filterRecords(rows, TimelineQuery(search = "独处")).size)
    }

    @Test
    fun moodAndMediaFilters_compose() {
        val happyPhotos = filterRecords(rows, TimelineQuery(mood = "开心", photosOnly = true))
        assertEquals(1, happyPhotos.size)
        assertEquals(1L, happyPhotos.first().id)
        assertTrue(filterRecords(rows, TimelineQuery(voiceOnly = true)).all { it.voiceUri != null })
    }

    @Test
    fun paginate_thenHasRemainder() {
        val page = paginate(rows, 2)
        assertEquals(2, page.size)
        assertEquals(3, rows.size)
    }
}
