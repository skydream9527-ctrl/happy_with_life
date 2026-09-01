package com.xiaoquexing.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishGuardTest {
    @Test
    fun moodRequiredFirst() {
        assertEquals(
            "请先选择一个心情～",
            PublishGuard.missingHint(null, hasContent = true),
        )
    }

    @Test
    fun contentRequiredAfterMood() {
        assertEquals(
            "请至少记录一些内容～",
            PublishGuard.missingHint("开心", hasContent = false),
        )
    }

    @Test
    fun readyWhenMoodAndText() {
        assertTrue(PublishGuard.hasContent("今天很好", 0, false, false, false, false))
        assertNull(PublishGuard.missingHint("开心", true))
    }

    @Test
    fun photoOrLocationCountsAsContent() {
        assertTrue(PublishGuard.hasContent("  ", 1, false, false, false, false))
        assertTrue(PublishGuard.hasContent("", 0, false, false, false, true))
        assertFalse(PublishGuard.hasContent("   ", 0, false, false, false, false))
    }
}
