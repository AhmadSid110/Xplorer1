package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import android.content.res.Resources
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BITMAP_DIMENSION = 2048

@Composable
fun PdfPageRenderer(
    renderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onRegisterZoom: ((Int, ZoomState?) -> Unit)? = null,
    debugState: MutableState<PdfDebugState>? = null
) {
    // Use an LRU cache and render only when the page is visible
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // We avoid opening the PdfRenderer.Page on the main thread or in remember to prevent
    // races when the pager disposes pages during flings. Instead, render jobs are dispatched
    // to a single-threaded executor which opens/closes the page on the worker thread.
    LaunchedEffect(pageIndex) {
        // Try cache first
        // Try cache first
        PdfBitmapCache.get(pageIndex)?.let {
            bitmap = it
            debugState?.let { it.value = it.value.copy(bitmapInfo = "cached", lastEvent = "cache hit") }
            return@LaunchedEffect
        }

        // Determine target width on MAIN thread (safe) so we can report bitmap size in debug state
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels.coerceAtLeast(100)
        val targetWidth = screenWidth.coerceAtMost(MAX_BITMAP_DIMENSION)

        debugState?.let { it.value = it.value.copy(bitmapInfo = "requested=${targetWidth}x?", lastEvent = "render start (dispatch)", pageOpened = true, pageDisposed = false) }

        // Check cache one more time (in case another render produced it)
        PdfBitmapCache.get(pageIndex)?.let {
            bitmap = it
            debugState?.let { it.value = it.value.copy(bitmapInfo = "cached", lastEvent = "cache hit") }
            return@LaunchedEffect
        }

        // Dispatch render to single-threaded executor that opens/closes page on the worker
        try {
            val (workerThread, bmp) = PdfRenderExecutor.renderPage(renderer, pageIndex, targetWidth)

            // Back on Main: update debug state and UI bitmap
            debugState?.let { it.value = it.value.copy(renderThread = workerThread, lastEvent = "render done", bitmapInfo = "${bmp.width}x${bmp.height}") }
            bitmap = bmp
            debugState?.let { it.value = it.value.copy(pageDisposed = true, lastEvent = "dispose page") }
        } catch (e: Exception) {
            e.printStackTrace()
            debugState?.let { it.value = it.value.copy(lastEvent = "render error: ${e.message}") }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            val zoom = remember { ZoomState() }

            // Register zoom with parent so overlay controls (rotate) can act on current page
            LaunchedEffect(zoom) {
                onRegisterZoom?.invoke(pageIndex, zoom)
            }

            DisposableEffect(pageIndex) {
                onDispose {
                    onRegisterZoom?.invoke(pageIndex, null)
                }
            }

            ZoomableBitmap(bitmap = bitmap!!, zoom = zoom, onTap = onTap)
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
