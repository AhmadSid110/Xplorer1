@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageViewerScreen(
    items: List<File>,
    index: Int,
    onClose: () -> Unit
) {
    // Safety check
    val validIndex = index.coerceIn(0, maxOf(0, items.size - 1))
    val pagerState = rememberPagerState(
        initialPage = validIndex,
        pageCount = { items.size }
    )
    var showControls by remember { mutableStateOf(true) }
    val pageZoomStates = remember { mutableStateMapOf<Int, ZoomState>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                // NOTE: Do NOT wrap the pager in clickable/pointerInput — that would intercept
                // swipe gestures and prevent HorizontalPager from receiving pointer events.
                // Tap detection is handled inside each page (via ZoomableBitmap.onTap).
                modifier = Modifier
                    .fillMaxSize(),
                beyondBoundsPageCount = 0,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
            ) { page ->
                ImagePageRenderer(file = items[page], onTap = { showControls = !showControls }, onRegisterZoom = { p, z -> if (z == null) pageZoomStates.remove(p) else pageZoomStates[p] = z }, pageIndex = page)
            }
        }

        // UI Overlay — controls only (do NOT cover full screen)
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
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row {
                        IconButton(onClick = { pageZoomStates[pagerState.currentPage]?.let { it.rotation = (it.rotation - 90f + 360f) % 360f } }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.rotate(270f))
                        }

                        IconButton(onClick = { pageZoomStates[pagerState.currentPage]?.let { it.rotation = (it.rotation + 90f) % 360f } }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.rotate(90f))
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePageRenderer(file: File, onTap: (() -> Unit)? = null, onRegisterZoom: ((Int, ZoomState?) -> Unit)? = null, pageIndex: Int = 0) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            // Decode with simple optimization
            // In a real app we'd downsample if too huge, but for now we decode directly
            // relying on standard Android limits and ZoomableBitmap handling
             try {
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val zoom = remember { ZoomState() }
            LaunchedEffect(zoom) { onRegisterZoom?.invoke(pageIndex, zoom) }
            DisposableEffect(pageIndex) { onDispose { onRegisterZoom?.invoke(pageIndex, null) } }
            ZoomableBitmap(bitmap = bitmap!!, zoom = zoom, onTap = onTap)
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
