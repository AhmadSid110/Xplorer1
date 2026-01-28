package com.droidexplorer.websim.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalCyberAccent = compositionLocalOf { Accent }

/**
 * Cyberpunk Chamfered Corner Shape (45-degree cuts)
 * Uses a fixed pixel cut to avoid density resolution issues.
 */
fun ChamferShape(cutSize: Dp = 8.dp) = GenericShape { size, _ ->
    // use a fraction of smallest dimension if density helpers are unavailable
    val cut = (minOf(size.width, size.height) * 0.08f).coerceAtLeast(6f)
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}

/**
 * Top Chamfered Shape (for bottom sheets)
 */
fun TopChamferShape(cutSize: Dp = 12.dp) = GenericShape { size, _ ->
    val cut = (minOf(size.width, size.height) * 0.10f).coerceAtLeast(8f)
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(0f, cut)
    close()
}

fun Modifier.cyberGlow(
    color: Color,
    intensity: Float = 0.35f,
    elevation: Dp = 6.dp
): Modifier = this.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        this.color = Color.Transparent.toArgb()
        setShadowLayer(
            elevation.toPx(),
            0f,
            0f,
            color.copy(alpha = intensity).toArgb()
        )
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
