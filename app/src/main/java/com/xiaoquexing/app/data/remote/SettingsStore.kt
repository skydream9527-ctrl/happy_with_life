package com.xiaoquexing.app.data.remote

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("xqx_settings", Context.MODE_PRIVATE)

    var analyticsOff: Boolean
        get() = prefs.getBoolean(ANALYTICS, false)
        set(v) { prefs.edit().putBoolean(ANALYTICS, v).apply() }

    var hidePhone: Boolean
        get() = prefs.getBoolean(HIDE_PHONE, false)
        set(v) { prefs.edit().putBoolean(HIDE_PHONE, v).apply() }

    var themeMode: String
        get() = prefs.getString(THEME, MODE_SYSTEM) ?: MODE_SYSTEM
        set(v) { prefs.edit().putString(THEME, v).apply() }

    var largeText: Boolean
        get() = prefs.getBoolean(LARGE_TEXT, false)
        set(v) { prefs.edit().putBoolean(LARGE_TEXT, v).apply() }

    val fontScale: Float get() = if (largeText) 1.3f else 1.0f

    var deleteRequestedAt: Long
        get() = prefs.getLong(DELETE_AT, 0L)
        set(v) { prefs.edit().putLong(DELETE_AT, v).apply() }

    val coolingMs: Long = COOLING_MS

    fun purgeAfter(): Long = purgeAt(deleteRequestedAt)

    fun requestDelete(now: Long = System.currentTimeMillis()) {
        deleteRequestedAt = now
    }

    fun cancelDelete() {
        deleteRequestedAt = 0
    }

    companion object {
        private const val ANALYTICS = "analytics_off"
        private const val HIDE_PHONE = "hide_phone"
        private const val DELETE_AT = "delete_requested_at"
        private const val THEME = "theme_mode"
        private const val LARGE_TEXT = "large_text"
        const val MODE_SYSTEM = "system"
        const val MODE_LIGHT = "light"
        const val MODE_DARK = "dark"
        const val COOLING_MS = 7L * 24 * 3600 * 1000
        fun purgeAt(start: Long): Long = if (start <= 0) 0 else start + COOLING_MS
    }
}
