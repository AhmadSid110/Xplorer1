package com.droidexplorer.websim.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.lastModified
import com.droidexplorer.websim.file.size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SEPARATOR = " • "
private const val BYTES_IN_KB = 1024L
private const val BYTES_IN_MB = BYTES_IN_KB * 1024
private const val BYTES_IN_GB = BYTES_IN_MB * 1024

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileDetailsView(
    files: List<FsNode>,
    onClick: (FsNode) -> Unit = {},
    onLongClick: (FsNode) -> Unit = {},
    isSelected: (FsNode) -> Boolean = { false },
    requiresPermission: (FsNode) -> Boolean = { false }
) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    LazyColumn {
        items(files, key = { it.uniqueKey }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)

            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onClick(file) },
                        onLongClick = { onLongClick(file) }
                    ),
                tonalElevation = if (selected) 2.dp else 0.dp,
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileIcon(
                        file = file,
                        size = 32.dp,
                        tint = if (permissionNeeded) MaterialTheme.colorScheme.error else null
                    )
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${file.sizeReadable()}$SEPARATOR${formatter.format(Date(file.lastModified()))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (permissionNeeded) {
                            Text(
                                text = "Permission needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListView(
    files: List<FsNode>,
    onClick: (FsNode) -> Unit = {},
    onLongClick: (FsNode) -> Unit = {},
    isSelected: (FsNode) -> Boolean = { false },
    requiresPermission: (FsNode) -> Boolean = { false }
) {
    LazyColumn {
        items(files, key = { it.uniqueKey }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)

            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onClick(file) },
                        onLongClick = { onLongClick(file) }
                    ),
                tonalElevation = if (selected) 2.dp else 0.dp,
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileIcon(
                        file = file,
                        size = 24.dp,
                        tint = if (permissionNeeded) MaterialTheme.colorScheme.error else null
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (permissionNeeded) {
                            Text(
                                text = "Permission needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun FsNode.sizeReadable(): String {
    val bytes = size()
    return when {
        bytes < BYTES_IN_KB -> "$bytes B"
        bytes < BYTES_IN_MB -> String.format(Locale.getDefault(), "%.1f KB", bytes / BYTES_IN_KB.toDouble())
        bytes < BYTES_IN_GB -> String.format(Locale.getDefault(), "%.1f MB", bytes / BYTES_IN_MB.toDouble())
        else -> String.format(Locale.getDefault(), "%.1f GB", bytes / BYTES_IN_GB.toDouble())
    }
}
