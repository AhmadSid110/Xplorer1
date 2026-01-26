package com.droidexplorer.websim.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CyberGlass(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .shadow(30.dp, shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F1A24).copy(alpha = 0.85f),
                        Color(0xFF0B141D).copy(alpha = 0.92f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Color(0xFF00E5FF).copy(alpha = 0.18f),
                shape
            )
    ) {
        content()
    }
}

@Composable
fun CyberGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .padding(16.dp)
            .shadow(40.dp, shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF111C26).copy(alpha = 0.82f),
                        Color(0xFF0A131C).copy(alpha = 0.9f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Color(0xFF00E5FF).copy(alpha = 0.25f),
                shape
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 18.dp),
            content = content
        )
    }
}
