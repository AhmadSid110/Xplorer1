package com.droidexplorer.websim.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A reusable glass surface component with Material 3-safe translucency.
 * 
 * This component creates a subtle glass-like aesthetic:
 * - Surface/surfaceVariant with alpha (0.75-0.9)
 * - RoundedCornerShape(14-16dp)
 * - Optional blur effect (Android 12+)
 * - Subtle shadow elevation
 * - 120ms color/visibility animations
 * 
 * Blur effects are only applied on Android 12+ for graceful degradation.
 * 
 * @param modifier Modifier to be applied to the surface
 * @param cornerRadius Corner radius in dp (default: 14)
 * @param alpha Alpha transparency (default: 0.85)
 * @param enableBlur Enable blur effect on supported devices (default: true)
 * @param blurRadius Blur radius in dp (default: 12)
 * @param elevation Shadow elevation in dp (default: 2)
 * @param content The content to be displayed inside the glass surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    alpha: Float = 0.85f,
    enableBlur: Boolean = true,
    blurRadius: Dp = 12.dp,
    elevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    // Apply blur only on Android 12+ for graceful degradation
    val blurModifier = if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .then(blurModifier)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
            ),
        content = content
    )
}
