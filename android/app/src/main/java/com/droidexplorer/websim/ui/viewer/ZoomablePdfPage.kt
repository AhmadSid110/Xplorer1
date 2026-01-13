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

@Composable
fun ZoomablePdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx().toInt()
    }

    val page = remember(renderer, pageIndex) {
        renderer.openPage(pageIndex)
    }

    DisposableEffect(Unit) {
        onDispose { page.close() }
    }

    // ✅ Calculate safe scaled height
    val aspectRatio = page.height.toFloat() / page.width.toFloat()
    val bitmapHeight = (screenWidthPx * aspectRatio).toInt()

    // ✅ SAFE bitmap (screen-sized, not page-sized)
    val bitmap = remember {
        Bitmap.createBitmap(
            screenWidthPx,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )
    }

    LaunchedEffect(Unit) {
        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
    }

    ZoomableBitmap(
        bitmap = bitmap,
        initialScale = 1f
    )
}
