package com.droidexplorer.websim.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.droidexplorer.websim.util.ZipUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContextMenu(
    file: File,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onZip: () -> Unit,
    onUnzip: () -> Unit,
    onShare: (Context) -> Unit
) {
    val isZipFile = ZipUtils.isZipFile(file)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (!file.isDirectory) {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    text = "Open",
                    onClick = { onDismiss() }
                )
            }
            
            ContextMenuItem(
                icon = Icons.Filled.ContentCopy,
                text = "Copy",
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            
            ContextMenuItem(
                icon = Icons.Filled.ContentCut,
                text = "Move",
                onClick = {
                    onMove()
                    onDismiss()
                }
            )
            
            ContextMenuItem(
                icon = Icons.Filled.DriveFileRenameOutline,
                text = "Rename",
                onClick = {
                    onRename()
                    onDismiss()
                }
            )
            
            if (isZipFile) {
                ContextMenuItem(
                    icon = Icons.Filled.FolderZip,
                    text = "Unzip",
                    onClick = {
                        onUnzip()
                        onDismiss()
                    }
                )
            } else {
                ContextMenuItem(
                    icon = Icons.Filled.FolderZip,
                    text = "Zip",
                    onClick = {
                        onZip()
                        onDismiss()
                    }
                )
            }
            
            if (!file.isDirectory) {
                ContextMenuItem(
                    icon = Icons.Filled.Share,
                    text = "Share",
                    onClick = {
                        // onShare will be called from the composable context
                        onDismiss()
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ContextMenuItem(
                icon = Icons.Filled.Delete,
                text = "Delete",
                onClick = {
                    onDelete()
                    onDismiss()
                },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
