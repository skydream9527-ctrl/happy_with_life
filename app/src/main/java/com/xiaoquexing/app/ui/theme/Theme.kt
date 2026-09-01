package com.xiaoquexing.app.ui.theme

import android.os.Build
import com.xiaoquexing.app.util.findActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = GreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F7F1),
    onSecondaryContainer = OnGreenContainer,
    tertiary = OrangeAccent,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = GreenBg,
    onSurfaceVariant = TextSecondary,
    surfaceTint = GreenPrimary,
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundLight,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    outline = Color(0xFFB0ADA6),
    outlineVariant = SeparatorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = Color(0xFF10130F),
    primaryContainer = Color(0xFF33503A),
    onPrimaryContainer = Color(0xFFDCEFDF),
    secondary = GreenLight,
    onSecondary = Color(0xFF10130F),
    secondaryContainer = Color(0xFF2A3D2E),
    onSecondaryContainer = Color(0xFFDCEFDF),
    tertiary = OrangeAccent,
    onTertiary = Color(0xFF10130F),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = GreenDarkPrimary,
    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBackground,
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF10130F),
    outline = Color(0xFF6B6862),
    outlineVariant = Color(0xFF3A3D37)
)

@Composable
fun XiaoQueXingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity() ?: return@SideEffect).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(fontScale),
        content = content
    )
}
