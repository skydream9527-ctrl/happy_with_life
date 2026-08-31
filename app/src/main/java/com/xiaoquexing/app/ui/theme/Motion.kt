package com.xiaoquexing.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object Motion {
    const val FAST = 180
    const val NORMAL = 280
    const val SLOW = 420

    @Composable
    fun reduce(): Boolean {
        val resolver = LocalContext.current.contentResolver
        val scale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return scale == 0f
    }

    fun fadeMs(reduce: Boolean, ms: Int = NORMAL) = tween<Float>(if (reduce) 0 else ms)
}

class GuideStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("xqx_guide", android.content.Context.MODE_PRIVATE)
    var seen: Boolean
        get() = prefs.getBoolean("seen", false)
        set(value) { prefs.edit().putBoolean("seen", value).apply() }
}
