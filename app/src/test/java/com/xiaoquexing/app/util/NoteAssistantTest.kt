package com.xiaoquexing.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteAssistantTest {
    @Test
    fun blankText_usesMoodAndPlace() {
        val line = NoteAssistant.suggest(CaptionHint(mood = "开心", location = "公园", hour = 8))
        assertTrue(line.contains("公园"))
        assertTrue(line.contains("清晨") || line.contains("暖"))
    }

    @Test
    fun existingText_isKept() {
        val line = NoteAssistant.suggest(CaptionHint(text = "咖啡很好喝", hour = 15))
        assertTrue(line.startsWith("咖啡很好喝"))
        assertFalse(line == "咖啡很好喝")
    }
}
