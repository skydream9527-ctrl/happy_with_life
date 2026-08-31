package com.xiaoquexing.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictiveBackLogicTest {
    @Test
    fun progress_isClamped() {
        fun clamp(v: Float) = v.coerceIn(0f, 1f)
        assertEquals(0f, clamp(-0.2f))
        assertEquals(1f, clamp(1.4f))
        assertEquals(0.3f, clamp(0.3f))
    }
}
