package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.torbox.download.DownloadStatus
import com.droidexplorer.websim.torbox.download.TorBoxDownloadEntity

private enum class DownloadFilter(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    DOWNLOADED("Downloaded"),
    FAILED("Failed")
}

@Composable
fun TorBoxDownloadsScreen(
    downloads: List<TorBoxDownloadEntity>,
    onPause: (TorBoxDownloadEntity) -> Unit,
    onResume: (TorBoxDownloadEntity) -> Unit,
    onRemove: (TorBoxDownloadEntity) -> Unit
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filter = DownloadFilter.values()[selectedFilterIndex]

    val filtered = when (filter) {
        DownloadFilter.ACTIVE -> downloads.filter {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        DownloadFilter.PAUSED -> downloads.filter { it.status == DownloadStatus.PAUSED }
        DownloadFilter.DOWNLOADED -> downloads.filter { it.status == DownloadStatus.COMPLETED }
        DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DownloadFilter.values().forEachIndexed { index, item ->
                FilterChip(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Text(
                text = "No ${filter.label.lowercase()} downloads",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onPause = { onPause(item) },
                        onResume = { onResume(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DownloadRow(
    item: TorBoxDownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val progressText = if (item.total > 0) {
                    val pct = ((item.downloaded * 100) / item.total).toInt()
                    "$pct% • ${item.downloaded}/${item.total}"
                } else {
                    "${item.downloaded}"
                }
                val speedText = if (item.speedBytesPerSec > 0L) {
                    " • ${formatSpeed(item.speedBytesPerSec)}"
                } else {
                    ""
                }
                Text(
                    text = "${item.status} • $progressText$speedText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause) {
                            Icon(
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = "Pause"
                            )
                        }
                    }
                    DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.QUEUED -> {
                        if (!item.sourceUrl.isNullOrBlank()) {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Resume"
                                )
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onRemove) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove"
                            )
                        }
                    }
                }
                if (item.status != DownloadStatus.COMPLETED) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove"
                        )
                    }
                }
                if (item.status == DownloadStatus.FAILED) {
                    IconButton(onClick = onResume) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Retry"
                        )
                    }
                }
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 B/s"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = bytesPerSec.toDouble()
    return when {
        value >= gb -> String.format("%.2f GB/s", value / gb)
        value >= mb -> String.format("%.2f MB/s", value / mb)
        value >= kb -> String.format("%.1f KB/s", value / kb)
        else -> "${bytesPerSec} B/s"
    }
}
