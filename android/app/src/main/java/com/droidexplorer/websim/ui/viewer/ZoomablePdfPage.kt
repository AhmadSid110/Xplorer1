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

    // Create bitmap at page resolution with size limits
    val bitmap = remember(pageIndex) {
        val width = page.width.coerceAtMost(MAX_BITMAP_SIZE)
        val height = page.height.coerceAtMost(MAX_BITMAP_SIZE)
        Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
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
        screenWidthPx / page.width.toFloat()
    }

    ZoomableBitmap(
        bitmap = bitmap,
        initialScale = fitScale
    )
}
