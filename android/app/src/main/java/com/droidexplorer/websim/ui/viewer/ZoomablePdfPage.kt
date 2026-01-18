package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_BITMAP_DIMENSION = 2048

@Composable
fun ZoomablePdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // We use a key of pageIndex to force full refresh when page changes
    LaunchedEffect(renderer, pageIndex, configuration.screenWidthDp) {
        // Compute target width on main
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        var targetWidth = screenWidthPx.toInt().coerceAtMost(MAX_BITMAP_DIMENSION)
        if (targetWidth <= 0) targetWidth = 100

        // Try cache
        PdfBitmapCache.get(pageIndex)?.let {
            bitmap = it
            return@LaunchedEffect
        }

        try {
            val (workerThread, newBitmap) = PdfRenderExecutor.renderPage(renderer, pageIndex, targetWidth)
            bitmap = newBitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(pageIndex) {
        onDispose {
            // Do NOT recycle bitmaps manually; let GC handle them to avoid native crashes.
            bitmap = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            val zoom = remember { ZoomState() }
            ZoomableBitmap(
                bitmap = bitmap!!,
                zoom = zoom
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
