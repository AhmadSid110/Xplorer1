package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val MAX_BITMAP_SIZE = 4096

@Composable
fun ZoomablePdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    val page = remember(pageIndex) { renderer.openPage(pageIndex) }
    DisposableEffect(pageIndex) {
        onDispose { page.close() }
    }

    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) {
        configuration.screenWidthDp.dp.toPx()
    }

    // Create bitmap at page resolution with size limits and aspect ratio preserved
    val bitmap = remember(pageIndex) {
        val pageWidth = page.width
        val pageHeight = page.height
        
        // Calculate scale factor to fit within MAX_BITMAP_SIZE while preserving aspect ratio
        val scaleFactor = if (pageWidth > MAX_BITMAP_SIZE || pageHeight > MAX_BITMAP_SIZE) {
            minOf(
                MAX_BITMAP_SIZE.toFloat() / pageWidth,
                MAX_BITMAP_SIZE.toFloat() / pageHeight
            )
        } else {
            1f
        }
        
        val width = (pageWidth * scaleFactor).toInt().coerceAtLeast(1)
        val height = (pageHeight * scaleFactor).toInt().coerceAtLeast(1)
        
        Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
    }
    
    DisposableEffect(pageIndex) {
        onDispose { 
            bitmap.recycle()
        }
    }

    LaunchedEffect(pageIndex) {
        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
    }

    // 🔑 FIT-TO-WIDTH SCALE
    val fitScale = remember(pageIndex) {
        screenWidthPx / bitmap.width.toFloat()
    }

    ZoomableBitmap(
        bitmap = bitmap,
        initialScale = fitScale
    )
}
