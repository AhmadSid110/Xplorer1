package com.droidexplorer.websim.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.storage.StorageInfo
import com.droidexplorer.websim.ui.formatFileSize

@Composable
fun StorageScreen(info: StorageInfo) {
    val progress = if (info.total > 0) {
        (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Storage", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Used: ${info.usedReadable()}")
        Text("Free: ${info.freeReadable()}")
        Text("Total: ${info.totalReadable()}")
    }
}

private fun StorageInfo.usedReadable() = formatFileSize(used)
private fun StorageInfo.freeReadable() = formatFileSize(free)
private fun StorageInfo.totalReadable() = formatFileSize(total)
