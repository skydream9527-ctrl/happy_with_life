package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.ui.album.AlbumExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumAggregatorTest {
    @Test
    fun buildSnapshot_groupsMoodAndGp() {
        val now = 1_700_000_000_000L
        val records = listOf(
            Record(id = 1, text = "a", moodTag = "开心", gpEarned = 10, createdAt = now, statusTags = "居家"),
            Record(id = 2, text = "b", moodTag = "开心", gpEarned = 7, createdAt = now + 1000, musicTitle = "晴天"),
        )
        val snap = buildAlbumSnapshot(AlbumWindows.ALL, "全部小确幸", records, "小确幸之树")
        assertEquals(2, snap.recordCount)
        assertEquals(17, snap.totalGp)
        assertEquals(2, snap.moods.first().second)
        assertTrue(snap.music.contains("晴天"))
        assertEquals(8, AlbumExporter.pageTexts(snap).size)
    }

    @Test
    fun window_weekExcludesOld() {
        val now = System.currentTimeMillis()
        val records = listOf(
            Record(id = 1, text = "old", createdAt = now - 20L * 24 * 3600 * 1000),
            Record(id = 2, text = "new", createdAt = now),
        )
        assertEquals(1, recordsInWindow(records, AlbumWindows.WEEK, now).size)
        assertEquals(2, recordsInWindow(records, AlbumWindows.ALL, now).size)
    }
}
