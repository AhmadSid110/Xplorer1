package com.droidexplorer.websim.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalCyberAccent = compositionLocalOf { Accent }

fun Modifier.cyberGlow(
    color: Color,
    intensity: Float = 0.4f
): Modifier = this.shadow(
    elevation = 12.dp,
    shape = RoundedCornerShape(16.dp),
    ambientColor = color.copy(alpha = intensity),
    spotColor = color.copy(alpha = intensity)
)
