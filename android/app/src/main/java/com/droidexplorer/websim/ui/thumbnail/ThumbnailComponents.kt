package com.droidexplorer.websim.ui.thumbnail

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.isImage
import com.droidexplorer.websim.file.isVideo
import java.io.File

@Composable
fun FileThumbnail(
    file: File,
    size: Dp,
    showVideoDuration: Boolean = true
) {
    val sizePx = with(LocalDensity.current) { size.toPx().toInt().coerceAtLeast(64) }

    val bitmap by produceState<Bitmap?>(null, file.path, sizePx) {
        value = when {
            file.isImage() -> loadImageThumbnail(file, sizePx)
            file.isVideo() -> loadVideoThumbnail(file, sizePx)
            else -> null
        }
    }

    val duration by produceState<String?>(null, file.path) {
        value = if (showVideoDuration && file.isVideo()) loadVideoDurationLabel(file) else null
    }

    if (bitmap != null) {
        Box {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
            if (!duration.isNullOrBlank()) {
                Text(
                    text = duration!!,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
