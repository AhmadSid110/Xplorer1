package com.droidexplorer.websim.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonCyanSoft,
    tertiary = NeonCyanSoft,
    background = CyberBlack,
    surface = CyberDarkSurface,
    surfaceVariant = CyberElevated,
    outline = DividerSoft,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    secondary = NeonCyanSoft,
    tertiary = NeonCyanSoft,
    background = CyberBlack,
    surface = CyberDarkSurface,
    surfaceVariant = CyberElevated,
    outline = DividerSoft,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary
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
    
    CompositionLocalProvider(LocalCyberAccent provides NeonCyan) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
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
                CyberBlack,
                CyberDarkSurface
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                CyberBlack,
                CyberDarkSurface
            )
        )
    }
}
