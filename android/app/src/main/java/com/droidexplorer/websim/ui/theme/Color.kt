package com.droidexplorer.websim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val CyberBlack = Color(0xFF0B0F14)
val CyberDarkSurface = Color(0xFF111722)
val CyberElevated = Color(0xFF161D2A)

val NeonCyan = Color(0xFF00E5FF)
val NeonCyanSoft = Color(0xFF00B8D4)
val NeonMagenta = Color(0xFFFF2D95)
val NeonMagentaSoft = Color(0xFFB0005A)

val NeonPurple = Color(0xFF8A6BFF)
val NeonGreen = Color(0xFF00E676)
val NeonBlue = Color(0xFF4FC3F7)
val NeonAmber = Color(0xFFFFC400)
val NeonPink = Color(0xFFFF5CA7)

val TextPrimary = Color(0xFFE6EBF2)
val TextSecondary = Color(0xFF9AA4B2)
val TextMuted = Color(0xFF6B7280)

val SuccessGreen = Color(0xFF00E676)
val WarningAmber = Color(0xFFFFC400)
val ErrorRed = Color(0xFFFF5252)

val DividerSoft = Color.White.copy(alpha = 0.06f)

/**
 * Glass theme colors for translucent surfaces
 * 
 * These are raw color constants used for creating glass-like effects.
 * Use GlassThemeColors extension for theme-aware glass colors.
 */
object GlassColors {
    /** Light glass overlay - for use on dark backgrounds */
    val LightGlass = Color.White.copy(alpha = 0.08f)
    
    /** Dark glass overlay - for use on light backgrounds */
    val DarkGlass = Color.Black.copy(alpha = 0.12f)
    
    /** Light glass surface - for use on dark backgrounds */
    val LightGlassSurface = Color.White.copy(alpha = 0.25f)
    
    /** Dark glass surface - for use on light backgrounds */
    val DarkGlassSurface = Color.Black.copy(alpha = 0.25f)
    
    /** Light divider - for use on dark backgrounds */
    val LightDivider = Color.Black.copy(alpha = 0.15f)
    
    /** Dark divider - for use on light backgrounds */
    val DarkDivider = Color.White.copy(alpha = 0.15f)
}

/**
 * Glass theme extension for MaterialTheme
 * 
 * Provides theme-aware glass colors that automatically adapt based on
 * the current MaterialTheme color scheme. Use this for components that
 * need to adapt to the current theme.
 */
val MaterialTheme.glassColors: GlassThemeColors
    @Composable
    @ReadOnlyComposable
    get() = GlassThemeColors(
        glassSurface = colorScheme.surface.copy(alpha = 0.25f),
        glassDivider = colorScheme.outline.copy(alpha = 0.15f)
    )

/**
 * Theme-aware glass colors
 * 
 * @param glassSurface Glass surface color derived from theme's surface color
 * @param glassDivider Glass divider color derived from theme's outline color
 */
data class GlassThemeColors(
    val glassSurface: Color,
    val glassDivider: Color
)
