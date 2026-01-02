package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PdfViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val rendererState = remember { mutableStateOf<PdfRenderer?>(null) }
    val pageCount = rendererState.value?.pageCount ?: 0

    DisposableEffect(file) {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        rendererState.value = renderer

        onDispose {
            renderer.close()
            fd.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        val renderer = rendererState.value
        if (renderer != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(pageCount) { index ->
                    PdfPage(renderer, index)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRenderer,
    index: Int
) {
    val bitmap = remember(renderer, index) {
        val page = renderer.openPage(index)
        val bmp = Bitmap.createBitmap(
            page.width,
            page.height,
            Bitmap.Config.ARGB_8888
        )
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bmp
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}
