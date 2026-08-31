package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {
    @Test
    fun cooling_isSevenDays() {
        assertEquals(7L * 24 * 3600 * 1000, SettingsStore.COOLING_MS)
        assertEquals(0, SettingsStore.purgeAt(0))
        assertEquals(1_000 + SettingsStore.COOLING_MS, SettingsStore.purgeAt(1_000))
    }

    @Test
    fun themeModes_areStable() {
        assertEquals("system", SettingsStore.MODE_SYSTEM)
        assertEquals("light", SettingsStore.MODE_LIGHT)
        assertEquals("dark", SettingsStore.MODE_DARK)
    }
}
