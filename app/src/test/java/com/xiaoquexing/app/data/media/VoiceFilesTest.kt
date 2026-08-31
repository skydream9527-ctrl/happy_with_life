package com.xiaoquexing.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VoiceFilesTest {
    @Test
    fun reject_missingAndTinyFiles() {
        assertFalse(VoiceFiles.accept(null, 1_000).ok)
        val empty = File.createTempFile("voice", ".m4a")
        empty.writeBytes(ByteArray(10))
        val rejected = VoiceFiles.accept(empty.absolutePath, 2_000)
        assertFalse(rejected.ok)
        assertFalse(empty.exists())
    }

    @Test
    fun accept_validFile_capsDuration() {
        val file = File.createTempFile("voice", ".m4a")
        file.writeBytes(ByteArray(1024))
        val ok = VoiceFiles.accept(file.absolutePath, 90_000)
        assertTrue(ok.ok)
        assertEquals(VoiceFiles.MAX_DURATION_MS, ok.durationMs)
        file.delete()
    }

    @Test
    fun remaining_neverNegative() {
        assertEquals(0, VoiceFiles.remainingMs(80_000))
        assertEquals(10_000, VoiceFiles.remainingMs(50_000))
    }
}
