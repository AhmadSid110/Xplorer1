package com.droidexplorer.websim.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.glass.CyberGlassPanel
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.util.ZipUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContextMenu(
    file: File,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onZip: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractToFolder: () -> Unit,
    onProperties: () -> Unit,
    onShare: (Context) -> Unit
) {
    val context = LocalContext.current
    val isZipFile = ZipUtils.isZipFile(file)
    
    val menuText = Color.White.copy(alpha = 0.92f)
    val accent = Color(0xFF00E5FF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        CyberGlassPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = menuText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1
            )
            
            Divider(color = DividerSoft, modifier = Modifier.padding(vertical = 8.dp))
            
            if (!file.isDirectory && onOpen != null) {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    text = "Open",
                    textColor = menuText,
                    tint = accent,
                    onClick = { 
                        onOpen()
                        onDismiss() 
                    }
                )
            }

            if (!file.isDirectory && onEdit != null && file.isTextLike()) {
                ContextMenuItem(
                    icon = Icons.Outlined.Edit,
                    text = "Edit",
                    textColor = menuText,
                    tint = accent,
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
            }
            
            ContextMenuItem(
                icon = Icons.Outlined.ContentCopy,
                text = "Copy",
                textColor = menuText,
                tint = accent,
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            
            ContextMenuItem(
                icon = Icons.Outlined.ContentCut,
                text = "Move",
                textColor = menuText,
                tint = accent,
                onClick = {
                    onMove()
                    onDismiss()
                }
            )
            
            ContextMenuItem(
                icon = Icons.Outlined.DriveFileRenameOutline,
                text = "Rename",
                textColor = menuText,
                tint = accent,
                onClick = {
                    onRename()
                    onDismiss()
                }
            )
            
            if (isZipFile) {
                ContextMenuItem(
                    icon = Icons.Outlined.Archive,
                    text = "Extract here",
                    textColor = menuText,
                    tint = accent,
                    onClick = {
                        onExtractHere()
                        onDismiss()
                    }
                )
                ContextMenuItem(
                    icon = Icons.Outlined.Archive,
                    text = "Extract to ${file.nameWithoutExtension}",
                    textColor = menuText,
                    tint = accent,
                    onClick = {
                        onExtractToFolder()
                        onDismiss()
                    }
                )
            } else {
                ContextMenuItem(
                    icon = Icons.Outlined.Archive,
                    text = "Zip",
                    textColor = menuText,
                    tint = accent,
                    onClick = {
                        onZip()
                        onDismiss()
                    }
                )
            }

            ContextMenuItem(
                icon = Icons.Outlined.Description,
                text = "Properties",
                textColor = menuText,
                tint = accent,
                onClick = {
                    onProperties()
                    onDismiss()
                }
            )
            
            if (!file.isDirectory) {
                ContextMenuItem(
                    icon = Icons.Outlined.Share,
                    text = "Share",
                    textColor = menuText,
                    tint = accent,
                    onClick = {
                        onShare(context)
                        onDismiss()
                    }
                )
            }
            
            Divider(color = DividerSoft, modifier = Modifier.padding(vertical = 8.dp))
            
            ContextMenuItem(
                icon = Icons.Outlined.Delete,
                text = "Delete",
                onClick = {
                    onDelete()
                    onDismiss()
                },
                tint = Color(0xFFFF4D6D),
                textColor = Color(0xFFFF4D6D)
            )
        }
    }
}

private fun File.isTextLike(): Boolean {
    val ext = extension.lowercase()
    return ext in setOf("txt", "md", "json", "xml", "csv", "log")
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = tint
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(52.dp)
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
            color = textColor
        )
    }
}
