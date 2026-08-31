package com.xiaoquexing.app

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.xiaoquexing.app.navigation.AppNavigation
import com.xiaoquexing.app.ui.theme.Appearance
import com.xiaoquexing.app.ui.theme.XiaoQueXingTheme

class MainActivity : ComponentActivity() {
    private val openCompose = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Appearance.load((application as XiaoQueXingApp).container.settingsStore)
        consumeCompose(intent)
        setContent {
            val mode by Appearance.mode.collectAsState()
            val large by Appearance.largeText.collectAsState()
            XiaoQueXingTheme(
                darkTheme = Appearance.dark(isSystemInDarkTheme(), mode),
                fontScale = if (large) 1.3f else 1.0f,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(openCompose = openCompose.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeCompose(intent)
    }

    private fun consumeCompose(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_COMPOSE, false) == true) {
            openCompose.value = true
            intent.removeExtra(EXTRA_OPEN_COMPOSE)
        }
    }

    companion object {
        const val EXTRA_OPEN_COMPOSE = "open_compose"
    }
}
