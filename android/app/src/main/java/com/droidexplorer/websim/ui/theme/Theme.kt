package com.droidexplorer.websim.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeFlavor {
    CLASSIC,
    CYBER
}

val LocalThemeFlavor = compositionLocalOf { ThemeFlavor.CLASSIC }

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Accent,
    tertiary = Accent,
    background = CyberBlack,
    surface = CyberDarkSurface,
    surfaceVariant = CyberElevated,
    outline = DividerSoft,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorRed,
    onError = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = Accent,
    tertiary = Accent,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceAlt,
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

    CompositionLocalProvider(
        LocalThemeFlavor provides ThemeFlavor.CLASSIC,
        LocalCyberAccent provides Accent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val Typography = Typography()

/**
 * Background brush adapts to theme flavor.
 */
@Composable
fun backgroundGradient(): Brush {
    return if (LocalThemeFlavor.current == ThemeFlavor.CYBER) {
        SolidColor(MaterialTheme.colorScheme.background)
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
