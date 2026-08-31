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
        const val COOLING_MS = 7L * 24 * 3600 * 1000
        fun purgeAt(start: Long): Long = if (start <= 0) 0 else start + COOLING_MS
    }
}
