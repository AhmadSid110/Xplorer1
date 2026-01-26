package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.pdfZoomable(zoom: ZoomState): Modifier = composed {
    pointerInput(zoom.scale) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent()
                val pointers = event.changes.size

                if (pointers >= 2) {
                    val zoomChange = event.calculateZoom()
                    val pan = event.calculatePan()
                    val rotationChange = event.calculateRotation()

                    val newScale = (zoom.scale * zoomChange).coerceIn(1f, 4f)
                    zoom.scale = newScale
                    if (newScale > 1f) {
                        zoom.offset += pan
                    }
                    zoom.rotation += rotationChange

                    event.changes.forEach { it.consume() }
                }

                if (event.changes.all { !it.pressed }) {
                    break
                }
            }
        }
    }
}

@Composable
fun ZoomableBitmap(
    bitmap: Bitmap,
    zoom: ZoomState,
    onTap: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap + double-tap (does not steal pager scroll)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onDoubleTap = {
                        if (zoom.scale > 1f) {
                            zoom.reset()
                        } else {
                            zoom.scale = 2f
                        }
                    }
                )
            }
            .pdfZoomable(zoom)
            .pointerInput(zoom.scale) {
                if (zoom.scale > 1f) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        zoom.offset += dragAmount
                    }
                }
            },


        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
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
