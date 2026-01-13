
package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PdfViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    val parcelFile = remember {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    val renderer = remember {
        PdfRenderer(parcelFile)
    }

    var pageIndex by remember { mutableStateOf(0) }
    val pageCount = renderer.pageCount

    DisposableEffect(Unit) {
        onDispose {
            renderer.close()
            parcelFile.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.material3.Text(
                text = "${pageIndex + 1} / $pageCount",
                color = Color.White
            )

            androidx.compose.material3.Text(
                text = "Close",
                color = Color.White,
                modifier = Modifier.clickable { onClose() }
            )
        }

        PdfPage(
            renderer = renderer,
            pageIndex = pageIndex,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.material3.Text(
                text = "◀ Prev",
                color = if (pageIndex > 0) Color.White else Color.Gray,
                modifier = Modifier.clickable(enabled = pageIndex > 0) {
                    pageIndex--
                }
            )

            androidx.compose.material3.Text(
                text = "Next ▶",
                color = if (pageIndex < pageCount - 1) Color.White else Color.Gray,
                modifier = Modifier.clickable(enabled = pageIndex < pageCount - 1) {
                    pageIndex++
                }
            )
        }
    }
}

@Composable
fun PdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val page = remember(pageIndex) {
        renderer.openPage(pageIndex)
    }

    DisposableEffect(pageIndex) {
        onDispose {
            page.close()
        }
    }

    val bitmap = remember(pageIndex) {
        Bitmap.createBitmap(
            page.width,
            page.height,
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

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )
}
