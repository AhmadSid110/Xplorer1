package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SimplePdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx().toInt()
    }

    val page = remember(pageIndex) {
        renderer.openPage(pageIndex)
    }

    DisposableEffect(Unit) {
        onDispose { page.close() }
    }

    val aspectRatio = page.height.toFloat() / page.width.toFloat()
    val heightPx = (screenWidthPx * aspectRatio).toInt()

    val bitmap = remember {
        Bitmap.createBitmap(
            screenWidthPx,
            heightPx,
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

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
