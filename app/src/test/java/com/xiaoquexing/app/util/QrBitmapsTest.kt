package com.xiaoquexing.app.util

import org.junit.Assert.assertTrue
import org.junit.Test

class QrBitmapsTest {
    @Test
    fun payload_usesRecordAnchorWhenIdPresent() {
        val link = QrBitmaps.payload(12)
        assertTrue(link.contains("record=12"))
        assertTrue(QrBitmaps.payload(0) == QrBitmaps.APP_LINK)
    }
}
