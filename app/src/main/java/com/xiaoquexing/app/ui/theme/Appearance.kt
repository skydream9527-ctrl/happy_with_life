package com.xiaoquexing.app.ui.theme

import com.xiaoquexing.app.data.remote.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Appearance {
    private val _mode = MutableStateFlow(SettingsStore.MODE_SYSTEM)
    private val _largeText = MutableStateFlow(false)
    val mode: StateFlow<String> = _mode
    val largeText: StateFlow<Boolean> = _largeText

    fun load(store: SettingsStore) {
        _mode.value = store.themeMode
        _largeText.value = store.largeText
    }

    fun setMode(store: SettingsStore, value: String) {
        store.themeMode = value
        _mode.value = value
    }

    fun setLargeText(store: SettingsStore, value: Boolean) {
        store.largeText = value
        _largeText.value = value
    }

    fun dark(systemDark: Boolean, mode: String = _mode.value): Boolean = when (mode) {
        SettingsStore.MODE_DARK -> true
        SettingsStore.MODE_LIGHT -> false
        else -> systemDark
    }
}
