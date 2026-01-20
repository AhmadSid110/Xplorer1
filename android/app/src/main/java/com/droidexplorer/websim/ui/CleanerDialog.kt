package com.droidexplorer.websim.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileManager
import com.droidexplorer.websim.ui.glass.neonGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerDialog(
    context: Context,
    rootPath: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var largeFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    val selectedFiles = remember { mutableStateMapOf<String, Boolean>() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = Modifier.neonGlass(),
        containerColor = Color.Transparent,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CleaningServices,
                contentDescription = "Cleaner",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Cleaner")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Clean up your storage by removing empty folders, clearing cache, and scanning for large files.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isProcessing || isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    OutlinedButton(
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.FolderDelete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Empty Folders")
                    }
                    
                    OutlinedButton(
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Clear App Cache")
                    }

                    OutlinedButton(
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Scan for Large Files")
                    }

                    if (largeFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Large files",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(largeFiles, key = { it.absolutePath }) { file ->
                                val isChecked = selectedFiles[file.absolutePath] == true
                                    Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedFiles[file.absolutePath] = checked
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                file.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                file.parent ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            formatSize(file.length()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        val selectedCount = selectedFiles.values.count { it }
                        if (selectedCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isProcessing = true
                                    scope.launch {
                                        val toDelete = largeFiles.filter { selectedFiles[it.absolutePath] == true }
                                        val deletedCount = withContext(Dispatchers.IO) {
                                            toDelete.count { it.delete() }
                                        }
                                        largeFiles = largeFiles - toDelete.toSet()
                                        selectedFiles.clear()
                                        onResult("Deleted $deletedCount large files")
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete Selected ($selectedCount)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Close")
            }
        }
    )
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
