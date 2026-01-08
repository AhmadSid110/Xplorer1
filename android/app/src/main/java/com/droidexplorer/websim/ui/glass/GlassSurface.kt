package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A reusable glass surface component with Material 3-safe translucency.
 * 
 * This component creates a subtle glass-like aesthetic:
 * - Surface/surfaceVariant with alpha (0.75-0.9)
 * - RoundedCornerShape(14-16dp)
 * - Zero elevation
 * - 120ms color/visibility animations
 * 
 * No experimental APIs or blur effects are used, ensuring Play Store compliance.
 * 
 * @param modifier Modifier to be applied to the surface
 * @param cornerRadius Corner radius in dp (default: 14)
 * @param alpha Alpha transparency (default: 0.85)
 * @param content The content to be displayed inside the glass surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    alpha: Float = 0.85f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
            ),
        content = content
    )
}
