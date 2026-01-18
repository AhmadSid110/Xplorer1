package com.droidexplorer.websim.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PdfDebugOverlay(state: PdfDebugState) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(8.dp)
            .widthIn(max = 320.dp)
    ) {
        Text("PDF DEBUG", color = Color.Green)
        Spacer(Modifier.height(4.dp))
        Text("Page: ${state.pageIndex} / ${state.pageCount}", color = Color.White)
        Text("Bitmap: ${state.bitmapInfo}", color = Color.White)
        Text("Opened: ${state.pageOpened}", color = Color.White)
        Text("Disposed: ${state.pageDisposed}", color = Color.White)
        Text("Open thread: ${state.openThread}", color = Color.White)
        Text("Render thread: ${state.renderThread}", color = Color.White)
        Text("Event: ${state.lastEvent}", color = Color.Yellow)
    }
}
