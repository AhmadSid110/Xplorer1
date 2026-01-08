package com.droidexplorer.websim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Glass theme colors for translucent surfaces
 */
object GlassColors {
    val LightGlass = Color.White.copy(alpha = 0.08f)
    val DarkGlass = Color.Black.copy(alpha = 0.12f)
    
    val LightGlassSurface = Color.White.copy(alpha = 0.25f)
    val DarkGlassSurface = Color.Black.copy(alpha = 0.25f)
    
    val LightDivider = Color.Black.copy(alpha = 0.15f)
    val DarkDivider = Color.White.copy(alpha = 0.15f)
}

/**
 * Glass theme extension for MaterialTheme
 */
val MaterialTheme.glassColors: GlassThemeColors
    @Composable
    @ReadOnlyComposable
    get() = GlassThemeColors(
        glassSurface = colorScheme.surface.copy(alpha = 0.25f),
        glassDivider = colorScheme.outline.copy(alpha = 0.15f)
    )

data class GlassThemeColors(
    val glassSurface: Color,
    val glassDivider: Color
)
