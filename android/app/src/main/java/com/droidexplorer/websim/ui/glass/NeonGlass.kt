package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.theme.BorderGrey
import com.droidexplorer.websim.ui.theme.ChamferShape
import com.droidexplorer.websim.ui.theme.CyberDarkSurface

fun Modifier.neonGlass(
    radius: Dp = 10.dp,
    alpha: Float = 0.28f
) = this
    .background(
        CyberDarkSurface.copy(alpha = alpha),
        ChamferShape(radius)
    )
    .border(
        1.dp,
        BorderGrey.copy(alpha = 0.6f),
        ChamferShape(radius)
    )
