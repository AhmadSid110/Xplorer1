package com.droidexplorer.websim.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerDialog(
    context: Context,
    rootPath: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.CleaningServices,
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
                    "Clean up your storage by removing empty folders and cache files.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            isProcessing = true
                            val count = FileManager.deleteEmptyFolders(rootPath)
                            onResult("Deleted $count empty folders")
                            isProcessing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.FolderDelete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Empty Folders")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            isProcessing = true
                            val cachePath = context.cacheDir.absolutePath
                            val count = FileManager.clearCache(cachePath)
                            onResult("Cleared $count cache items")
                            isProcessing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Clear App Cache")
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
