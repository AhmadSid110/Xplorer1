package com.droidexplorer.websim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFB388FF)
val NeonPink = Color(0xFFFF4D9D)

val DarkBase = Color(0xFF0B0F14)
val DarkSurface = Color(0xFF121823)
val DarkSurfaceAlt = Color(0xFF1A2233)

val DividerSoft = Color.White.copy(alpha = 0.08f)

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
