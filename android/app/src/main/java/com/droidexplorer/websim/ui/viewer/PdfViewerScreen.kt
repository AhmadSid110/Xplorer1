@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.droidexplorer.websim.ui.viewer

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.clearAndSetSemantics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

import java.io.File

@Composable
fun PdfViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    // 1. Controller State
    var rendererWrapper by remember { mutableStateOf<RendererWrapper?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }

    // Debug state (non-interfering)
    val debugState = remember { mutableStateOf(PdfDebugState()) }

    // Initialize Renderer
    DisposableEffect(file) {
        var pfd: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            if (pfd != null) {
                pdfRenderer = PdfRenderer(pfd)
                rendererWrapper = RendererWrapper(pfd, pdfRenderer)
            }
        } catch (e: Exception) {
            error = "Failed to open PDF: ${e.message}"
        }

        onDispose {
            try {
                PdfBitmapCache.clear()
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                // Ignore closing errors
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
            // Cleanup button
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                 Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        } else if (rendererWrapper != null) {
            val renderer = rendererWrapper!!.renderer
            val pageCount = renderer.pageCount
            
            // 2. Vertical Pager
            // NOTE: Do NOT wrap the pager in clickable/pointerInput — that would intercept
            // swipe gestures and prevent VerticalPager from receiving pointer events.
            // Instead, tap detection is handled inside each page (via ZoomableBitmap.onTap).
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val pageZoomStates = remember { mutableStateMapOf<Int, ZoomState>() }
            
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize(),
                beyondBoundsPageCount = 0,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
            ) { index ->

                // 3. Render content using isolated renderer
                PdfPageRenderer(
                    renderer = renderer,
                    pageIndex = index,
                    modifier = Modifier.fillMaxSize(),
                    onTap = { showControls = !showControls },
                    onRegisterZoom = { p, z -> if (z == null) pageZoomStates.remove(p) else pageZoomStates[p] = z },
                    debugState = debugState
                )
            }

            // Vertical fast scroll thumb (PDF) — narrow, non-intercepting by default
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentPage by remember { derivedStateOf { pagerState.currentPage } }
                var sliderPos by remember { mutableStateOf(currentPage.toFloat()) }
                val scope = rememberCoroutineScope()

                // Safe scroll helper: cancels any ongoing pager scroll and then scrolls to a clamped page.
                val safeScrollToPage: (Int) -> Unit = { targetPage ->
                    scope.launch {
                        val safePage = targetPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

                        // Wait briefly if pager is mid-scroll (fling/settling) — prefer waiting to avoid IllegalState
                        var waited = 0
                        while (pagerState.isScrollInProgress && waited < 30) {
                            kotlinx.coroutines.delay(10)
                            waited++
                        }

                        // Try to scroll; if it throws (race), retry once after a short delay
                        try {
                            pagerState.scrollToPage(safePage)
                        } catch (e: Exception) {
                            Log.w("PAGER", "scrollToPage failed, retrying: ${e.message}")
                            try {
                                kotlinx.coroutines.delay(50)
                                pagerState.scrollToPage(safePage)
                            } catch (ex: Exception) {
                                Log.w("PAGER", "scrollToPage retry failed: ${ex.message}")
                            }
                        }
                    }
                }

                Slider(
                    value = sliderPos.coerceIn(0f, (pageCount - 1).toFloat()),
                    onValueChange = { v -> sliderPos = v.coerceIn(0f, (pageCount - 1).toFloat()) },
                    onValueChangeFinished = {
                        val safeTarget = sliderPos.roundToInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                        safeScrollToPage(safeTarget)
                    },
                    valueRange = 0f..(pageCount - 1).toFloat(),
                    steps = (pageCount - 1).coerceAtLeast(0),
                    modifier = Modifier
                        .fillMaxHeight()
                        .rotate(270f)
                )

                LaunchedEffect(currentPage) { sliderPos = currentPage.toFloat() }

                // TEMP: log pager changes for isolation testing
                LaunchedEffect(pagerState.currentPage) {
                    Log.d("PAGER", "Page=${pagerState.currentPage}")
                }
            }

            // 4. UI Overlay — controls only (do NOT cover full screen)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(16.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Row {
                            IconButton(onClick = {
                                pageZoomStates[pagerState.currentPage]?.let { it.rotation = (it.rotation - 90f + 360f) % 360f }
                            }) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.rotate(270f))
                            }

                            IconButton(onClick = {
                                pageZoomStates[pagerState.currentPage]?.let { it.rotation = (it.rotation + 90f) % 360f }
                            }) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.rotate(90f))
                            }

                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    }

                }
            }

            // Debug overlay (visual only, non-intercepting)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp)
            ) {
                PdfDebugOverlay(debugState.value)
            }
        } else {
             CircularProgressIndicator(
                 modifier = Modifier.align(Alignment.Center),
                 color = Color.White
             )
        }
    }
}

// Helper class to hold references
data class RendererWrapper(
    val pfd: ParcelFileDescriptor,
    val renderer: PdfRenderer
)
