package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.theme.CyberDarkSurface
import com.droidexplorer.websim.ui.theme.DividerSoft

fun Modifier.neonGlass(
    radius: Dp = 16.dp,
    alpha: Float = 0.08f
) = this
    .background(
        CyberDarkSurface.copy(alpha = alpha),
        RoundedCornerShape(radius)
    )
    .border(
        1.dp,
        DividerSoft,
        RoundedCornerShape(radius)
    )
