package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.theme.ChamferShape

/**
 * Cyberpunk glass surface (no gradients, no blur, chamfered corners)
 * Ensures text is never washed out by heavy overlays.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    alpha: Float = 0.20f,
    enableBlur: Boolean = false,
    blurRadius: Dp = 0.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: GenericShape = ChamferShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha)),
        content = content
    )
}
