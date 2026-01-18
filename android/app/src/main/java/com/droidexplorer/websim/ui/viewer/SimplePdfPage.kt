package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SimplePdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx().toInt()
    }

    // Render using single-threaded executor to avoid concurrent PdfRenderer access
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, screenWidthPx) {
        // try cache first
        PdfBitmapCache.get(pageIndex)?.let {
            bitmapState.value = it
            return@LaunchedEffect
        }

        try {
            val (workerThread, bmp) = PdfRenderExecutor.renderPage(renderer, pageIndex, screenWidthPx)
            bitmapState.value = bmp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val bitmap = bitmapState.value

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    } else {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
