package com.xiaoquexing.app.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupTest {
    @Test
    fun skip_whenSameTextAndTime() {
        val existing = setOf(LocalBackup.fingerprint("hello", 100))
        assertTrue(LocalBackup.shouldSkip(existing, BackupRecord(text = "hello", createdAt = 100)))
        assertFalse(LocalBackup.shouldSkip(existing, BackupRecord(text = "hello", createdAt = 101)))
    }
}
