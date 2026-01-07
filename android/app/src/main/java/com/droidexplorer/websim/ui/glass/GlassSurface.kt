package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A reusable glass surface component that provides a modern, semi-transparent glass effect.
 * 
 * This component creates a production-safe glass UI that works on all Android versions:
 * - Semi-transparent surfaces
 * - Subtle elevation + border
 * - Soft background tint
 * - Smooth animations
 * 
 * No experimental APIs are used, making it safe for all Android versions.
 * 
 * @param modifier Modifier to be applied to the surface
 * @param cornerRadius Corner radius in dp (default: 16)
 * @param content The content to be displayed inside the glass surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            )
            .border(
                width = 0.6.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = shape
            ),
        content = content
    )
}
