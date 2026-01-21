package com.droidexplorer.websim.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberPulseAlpha(active: Boolean): Float {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(durationMillis = 140, easing = FastOutSlowInEasing))
            pulse.animateTo(0.6f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        } else {
            pulse.snapTo(0f)
        }
    }
    return pulse.value
}

@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.03f),
    lineHeight: Dp = 2.dp
) {
    val brush = remember(color, lineHeight) {
        Brush.verticalGradient(
            colors = listOf(
                color,
                Color.Transparent,
                Color.Transparent,
                color
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}

@Composable
fun IndeterminateArc(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Dp = 6.dp
) {
    val transition = rememberInfiniteTransition(label = "arc")
    val sweep by transition.animateFloat(
        initialValue = 40f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing)
        ),
        label = "arcSweep"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "arcRotation"
    )

    Canvas(modifier = modifier) {
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}
