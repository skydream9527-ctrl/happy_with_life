package com.xiaoquexing.app.ui.theme

import com.xiaoquexing.app.data.remote.SettingsStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    @Test
    fun dark_followsExplicitMode() {
        assertTrue(Appearance.dark(systemDark = false, mode = SettingsStore.MODE_DARK))
        assertFalse(Appearance.dark(systemDark = true, mode = SettingsStore.MODE_LIGHT))
        assertTrue(Appearance.dark(systemDark = true, mode = SettingsStore.MODE_SYSTEM))
    }
}
