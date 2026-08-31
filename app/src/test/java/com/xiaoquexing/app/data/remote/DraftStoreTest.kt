package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftStoreTest {
    @Test
    fun emptyDraft_isDetected() {
        assertTrue(DraftStore.isEmpty(RecordDraft()))
        assertFalse(DraftStore.isEmpty(RecordDraft(text = "hi")))
        assertFalse(DraftStore.isEmpty(RecordDraft(mood = "开心")))
    }
}
