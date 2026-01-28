package com.droidexplorer.websim.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Cyberpunk/Tesla "Night Drive" palette
private val CyberNightBlack = Color(0xFF000000)
private val CyberNightGunmetal = Color(0xFF0D0D0D)
private val CyberNightElevated = Color(0xFF1A1A1A)
private val CyberNightAccent = Color(0xFF00F3FF)
private val CyberNightBorder = Color(0xFF333333)
private val CyberNightTextPrimary = Color(0xFFF1F5F9)
private val CyberNightTextSecondary = Color(0xFFF1F5F9).copy(alpha = 0.70f)
private val CyberNightError = Color(0xFFE82127)

private val CyberColorScheme = darkColorScheme(
    primary = CyberNightAccent,
    secondary = CyberNightAccent,
    tertiary = CyberNightAccent,
    background = CyberNightBlack,
    surface = CyberNightGunmetal,
    surfaceVariant = CyberNightElevated,
    outline = CyberNightBorder,
    onBackground = CyberNightTextPrimary,
    onSurface = CyberNightTextPrimary,
    onSurfaceVariant = CyberNightTextSecondary,
    error = CyberNightError,
    onError = CyberNightTextPrimary
)

private val CyberTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.25.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.25.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 0.25.sp)
)

private val CyberShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
)

@Composable
fun XplorerCyberTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CyberColorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalThemeFlavor provides ThemeFlavor.CYBER,
        LocalCyberAccent provides CyberNightAccent
    ) {
        MaterialTheme(
            colorScheme = CyberColorScheme,
            typography = CyberTypography,
            shapes = CyberShapes,
            content = content
        )
    }
}
