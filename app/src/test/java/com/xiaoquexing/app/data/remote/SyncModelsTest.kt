package com.xiaoquexing.app.data.remote

import com.xiaoquexing.app.data.media.PhotoUploader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModelsTest {
    @Test
    fun mediaRef_defaultsToPhoto() {
        val ref = MediaRef(mediaId = "m_1")
        assertEquals("PHOTO", ref.type)
        val write = RecordWrite(spaceId = "s", moodTag = "平静", media = listOf(ref))
        assertEquals(1, write.media.size)
        assertEquals("m_1", write.media.first().mediaId)
    }

    @Test
    fun photoUploader_capsAtFiveMegabytes() {
        assertEquals(5 * 1024 * 1024, PhotoUploader.MAX_BYTES)
        assertEquals(1280, PhotoUploader.MAX_EDGE)
    }

    @Test
    fun syncReport_defaultsClean() {
        val report = SyncReport()
        assertEquals(0, report.pushed)
        assertEquals(0, report.conflicts)
        assertTrue(report.error == null)
    }
}
