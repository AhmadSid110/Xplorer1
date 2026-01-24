package com.droidexplorer.websim.ui

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileProperties(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val created: Long,
    val isDirectory: Boolean,
    val itemCount: Int = 0,
    val hiddenCount: Int = 0,
    val readable: Boolean = false,
    val writable: Boolean = false,
    val storageType: String = "Unknown"
)

suspend fun loadProperties(file: File): FileProperties = withContext(Dispatchers.IO) {
    val isDir = file.isDirectory
    val children = file.listFiles().orEmpty()
    val totalSize = if (isDir) children.sumOf { if (it.isFile) it.length() else 0L } else file.length()
    val hiddenCount = if (isDir) children.count { it.isHidden } else 0
    val storageType = when {
        file.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath) -> "External"
        file.absolutePath.startsWith("/storage/") -> "External"
        else -> "Internal"
    }

    FileProperties(
        name = file.name,
        path = file.absolutePath,
        size = totalSize,
        lastModified = file.lastModified(),
        created = file.lastModified(),
        isDirectory = isDir,
        itemCount = if (isDir) children.size else 0,
        hiddenCount = hiddenCount,
        readable = file.canRead(),
        writable = file.canWrite(),
        storageType = storageType
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesSheet(
    file: File,
    onDismiss: () -> Unit
) {
    var props by remember(file) { mutableStateOf<FileProperties?>(null) }

    LaunchedEffect(file) {
        props = loadProperties(file)
    }

    val formatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Properties", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            val p = props ?: return@Column
            PropertyRow("Name", p.name)
            PropertyRow("Path", p.path)
            PropertyRow("Size", formatSize(p.size))
            PropertyRow("Modified", formatter.format(Date(p.lastModified)))
            PropertyRow("Created", formatter.format(Date(p.created)))
            PropertyRow("Readable", if (p.readable) "Yes" else "No")
            PropertyRow("Writable", if (p.writable) "Yes" else "No")
            PropertyRow("Storage", p.storageType)

            if (p.isDirectory) {
                PropertyRow("Items", p.itemCount.toString())
                PropertyRow("Hidden", p.hiddenCount.toString())
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = size.toDouble()
    return when {
        value >= gb -> String.format("%.2f GB", value / gb)
        value >= mb -> String.format("%.2f MB", value / mb)
        value >= kb -> String.format("%.1f KB", value / kb)
        else -> "$size B"
    }
}
