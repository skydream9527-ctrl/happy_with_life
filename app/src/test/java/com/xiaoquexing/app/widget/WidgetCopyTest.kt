package com.xiaoquexing.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCopyTest {
    @Test
    fun emptyDay_promptsWrite() {
        val snap = WidgetCopy.of(0, null, null, 0)
        assertEquals("今天还没记", snap.title)
        assertTrue(snap.body.contains("小确幸"))
        assertEquals("0 GP", snap.footer)
    }

    @Test
    fun withRecord_showsCountAndMood() {
        val snap = WidgetCopy.of(2, "开心", "咖啡很好喝", 36)
        assertEquals("今天 2 条", snap.title)
        assertTrue(snap.body.contains("开心"))
        assertEquals("36 GP", snap.footer)
    }
}
