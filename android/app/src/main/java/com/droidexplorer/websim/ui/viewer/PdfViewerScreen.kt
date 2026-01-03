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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
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

private const val PDF_SCALE_FACTOR = 0.75f

@Composable
fun PdfViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val rendererState = remember { mutableStateOf<PdfRenderer?>(null) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val pageCount = rendererState.value?.pageCount ?: 0

    DisposableEffect(file) {
        var fd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            rendererState.value = renderer
            errorState.value = null
        } catch (e: Exception) {
            errorState.value = e.message ?: "Unable to open PDF"
            rendererState.value = null
            renderer?.close()
            fd?.close()
        }

        onDispose {
            rendererState.value?.close()
            fd?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val renderer = rendererState.value
        val error = errorState.value
        when {
            renderer != null -> {
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

            error != null -> {
                Text(
                    text = error,
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                )
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
        val width = (page.width * PDF_SCALE_FACTOR).toInt().coerceAtLeast(1)
        val height = (page.height * PDF_SCALE_FACTOR).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(
            width,
            height,
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
