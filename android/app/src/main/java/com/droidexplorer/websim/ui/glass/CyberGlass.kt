package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.theme.BorderGrey
import com.droidexplorer.websim.ui.theme.ChamferShape
import com.droidexplorer.websim.ui.theme.CyberDarkSurface

@Composable
fun CyberGlass(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = ChamferShape(12.dp)

    Box(
        modifier = modifier
            .background(CyberDarkSurface.copy(alpha = 0.35f), shape)
            .border(1.dp, BorderGrey, shape)
    ) {
        content()
    }
}

@Composable
fun CyberGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = ChamferShape(14.dp)

    Box(
        modifier = modifier
            .padding(12.dp)
            .background(CyberDarkSurface.copy(alpha = 0.40f), shape)
            .border(1.dp, BorderGrey, shape)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp),
            content = content
        )
    }
}
