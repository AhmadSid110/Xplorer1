package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neonGlass(
    radius: Dp = 16.dp,
    alpha: Float = 0.08f
) = this
    .background(
        Color.White.copy(alpha = alpha),
        RoundedCornerShape(radius)
    )
    .border(
        1.dp,
        Color.White.copy(alpha = 0.12f),
        RoundedCornerShape(radius)
    )
