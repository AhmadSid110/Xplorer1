package com.droidexplorer.websim.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import kotlinx.coroutines.launch

/**
 * Context menu for TorBox remote files.
 * Fetches download link on-demand and copies to clipboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorBoxContextMenu(
    file: FsNode.TorBox,
    torBoxClient: com.droidexplorer.websim.torbox.TorBoxClient?,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
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
            
            Text(
                text = "Remote file (read-only)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                TorBoxContextMenuItem(
                    icon = Icons.Filled.ContentCopy,
                    text = "Copy download link",
                    onClick = {
                        if (torBoxClient == null) {
                            onError("TorBox client not available")
                            return@TorBoxContextMenuItem
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val link = torBoxClient.getShareLink(file.id)
                                if (link != null) {
                                    onCopyLink(link)
                                } else {
                                    onError("Failed to get download link")
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TorBoxContextMenuItem(
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
