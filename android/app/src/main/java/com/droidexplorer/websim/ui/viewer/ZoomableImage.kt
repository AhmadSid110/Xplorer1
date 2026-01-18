package com.droidexplorer.websim.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun ZoomableImage(file: File, zoom: ZoomState, onTap: (() -> Unit)? = null) {
    val painter = rememberAsyncImagePainter(
        model = file,
        contentScale = ContentScale.Fit
    )

    // SAFE transform pattern: always detect pinch (zoom) but only pan when zoomed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Always listen for tap/double-tap
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onDoubleTap = {
                        if (zoom.scale > 1f) {
                            zoom.scale = 1f
                            zoom.offset = Offset.Zero
                            zoom.rotation = 0f
                        } else {
                            zoom.scale = 3f
                        }
                    }
                )
            }
            // Only start transform detection when two or more pointers are present.
            // Keyed on zoom.scale so the pointer handler detaches when scale changes to 1f,
            // avoiding the "lingering velocity" issue that can steal pager flings.
            .pointerInput(zoom.scale) {
                while (true) {
                    val ev = awaitPointerEventScope { awaitPointerEvent() }
                    if (ev.changes.size >= 2) {
                        detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                            val gz = gestureZoom
                            val gr = gestureRotation
                            val newScale = (zoom.scale * gz).coerceIn(1f, 5f)
                            if (newScale > 1f) zoom.offset = zoom.offset + pan
                            zoom.scale = newScale
                            zoom.rotation = (zoom.rotation + gr) % 360f
                        }
                    }
                }
            },


        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                    rotationZ = zoom.rotation
                }
        )
    }
}
