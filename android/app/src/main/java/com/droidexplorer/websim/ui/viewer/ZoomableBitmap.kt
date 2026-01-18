package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

@Composable
fun ZoomableBitmap(
    bitmap: Bitmap,
    zoom: ZoomState,
    onTap: (() -> Unit)? = null
) {
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
                            zoom.scale = 2.5f
                        }
                    }
                )
            }
            // Only run transform detection while two or more pointers are present.
            // Keyed on zoom.scale so the pointer handler is recreated when the scale changes
            // — this prevents lingering velocities from stealing pager flings after pinch.
            .pointerInput(zoom.scale) {
                while (true) {
                    val ev = awaitPointerEventScope { awaitPointerEvent() }
                    if (ev.changes.size >= 2) {
                        detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                            val gz = gestureZoom
                            val gr = gestureRotation
                            // Always apply pinch (zoom)
                            val newScale = (zoom.scale * gz).coerceIn(1f, 5f)
                            // Apply pan only if resulting scale > 1f
                            if (newScale > 1f) {
                                zoom.offset = zoom.offset + pan
                            }
                            zoom.scale = newScale
                            // Apply rotation regardless (optional)
                            zoom.rotation = (zoom.rotation + gr) % 360f
                        }
                    }
                }
            },


        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
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
