package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordMapperTest {
    @Test
    fun recordWrite_keepsMoodAndSpace() {
        val write = RecordWrite(
            spaceId = "spc_1",
            moodTag = "开心",
            contentText = "阳光",
            occurredDate = "2026-08-30",
        )
        assertEquals("spc_1", write.spaceId)
        assertEquals("开心", write.moodTag)
        assertEquals("阳光", write.contentText)
        assertEquals("2026-08-30", write.occurredDate)
        assertTrue(write.media.isEmpty())
    }

    @Test
    fun mutationResult_readsAuthoritativeGp() {
        val result = MutationResult(
            mutationId = "m1",
            status = "APPLIED",
            serverId = "rec_1",
            version = 1,
            authoritative = Authoritative(gpFinal = 17, gpCapped = false, plantStage = "SPROUT"),
        )
        assertEquals("APPLIED", result.status)
        assertEquals(17, result.authoritative?.gpFinal)
        assertEquals("rec_1", result.serverId)
    }
}
