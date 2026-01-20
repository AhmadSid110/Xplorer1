package com.droidexplorer.websim.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CleanerScreen(
    context: Context,
    rootPath: String,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var largeFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    val selectedFiles = remember { mutableStateMapOf<String, Boolean>() }

    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Cleaner",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Free up space with quick cleanup and large file review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                QuickActionCard(
                    title = "Delete empty folders",
                    description = "Remove empty directories across storage",
                    icon = Icons.Filled.FolderDelete,
                    isBusy = isProcessing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isProcessing = true
                        scope.launch {
                            val count = withContext(Dispatchers.IO) {
                                FileManager.deleteEmptyFolders(rootPath)
                            }
                            onResult("Deleted $count empty folders")
                            isProcessing = false
                        }
                    }
                )
            }

            item {
                QuickActionCard(
                    title = "Clear app cache",
                    description = "Remove cached temporary files",
                    icon = Icons.Filled.Delete,
                    isBusy = isProcessing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isProcessing = true
                        scope.launch {
                            val cachePath = context.cacheDir.absolutePath
                            val count = withContext(Dispatchers.IO) {
                                FileManager.clearCache(cachePath)
                            }
                            onResult("Cleared $count cache items")
                            isProcessing = false
                        }
                    }
                )
            }

            item {
                SectionHeader(title = "Large files")
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Scan storage", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Find large files across storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isScanning = true
                                    scope.launch {
                                        val results = withContext(Dispatchers.IO) {
                                            FileManager.findLargeFiles(rootPath)
                                        }
                                        largeFiles = results
                                        selectedFiles.clear()
                                        onResult("Found ${results.size} large files")
                                        isScanning = false
                                    }
                                },
                                enabled = !isScanning && !isProcessing
                            ) {
                                Text("Scan")
                            }
                        }

                        if (isScanning) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (largeFiles.isNotEmpty()) {
                item {
                    Text(
                        text = "Review files",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(largeFiles, key = { it.absolutePath }) { file ->
                    val isChecked = selectedFiles[file.absolutePath] == true
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedFiles[file.absolutePath] = checked
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    file.parent ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatSize(file.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    val selectedCount = selectedFiles.values.count { it }
                    if (selectedCount > 0) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isProcessing = true
                                scope.launch {
                                    val toDelete = largeFiles.filter {
                                        selectedFiles[it.absolutePath] == true
                                    }
                                    val deletedCount = withContext(Dispatchers.IO) {
                                        toDelete.count { it.delete() }
                                    }
                                    largeFiles = largeFiles - toDelete.toSet()
                                    selectedFiles.clear()
                                    onResult("Deleted $deletedCount large files")
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Text("Delete selected ($selectedCount)")
                        }
                    }
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onClick, enabled = !isBusy) {
                Text("Run")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CleaningServices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(title, style = MaterialTheme.typography.titleSmall)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
