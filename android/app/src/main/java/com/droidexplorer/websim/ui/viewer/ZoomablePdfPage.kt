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
    val page = remember { renderer.openPage(pageIndex) }
    DisposableEffect(Unit) {
        onDispose { page.close() }
    }

    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) {
        configuration.screenWidthDp.dp.toPx()
    }

    // Create bitmap at page resolution
    val bitmap = remember {
        Bitmap.createBitmap(
            page.width,
            page.height,
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

    // 🔑 FIT-TO-WIDTH SCALE
    val fitScale = remember {
        screenWidthPx / page.width.toFloat()
    }

    ZoomableBitmap(
        bitmap = bitmap,
        initialScale = fitScale
    )
}
