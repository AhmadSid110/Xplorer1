package com.droidexplorer.websim.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPurple,
    tertiary = NeonPink,
    background = DarkBase,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceAlt,
    outline = DividerSoft
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3F5EE6),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E2FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001A42),
    secondary = androidx.compose.ui.graphics.Color(0xFF555F71),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E2F9),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF121C2B),
    tertiary = androidx.compose.ui.graphics.Color(0xFF705574),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surface = androidx.compose.ui.graphics.Color(0xFFFBFDF8),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C19),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE3E4E8),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF44474E),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = androidx.compose.ui.graphics.Color(0xFFFBFDF8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C19)
)

@Composable
fun XplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val Typography = Typography()

/**
 * Dynamic gradient background brush that adapts to dark/light theme
 */
@Composable
fun backgroundGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.linearGradient(
            listOf(
                Color(0xFF121212),
                Color(0xFF1F1F1F)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFFBFDF8),
                Color(0xFFE8EAE6)
            )
        )
    }
}
