package com.droidexplorer.websim.ui

import androidx.compose.foundation.clickable
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.torbox.TorBoxDownloadManager
import com.droidexplorer.websim.torbox.TorBoxFileType
import com.droidexplorer.websim.torbox.TorBoxTempDownloader
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.viewer.Viewer
import com.droidexplorer.websim.util.ClipboardUtil
import androidx.compose.ui.platform.LocalContext
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
    onOpenViewer: (Viewer) -> Unit,
    onMessage: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val fileType = remember(file.name) { TorBoxFileType.fromFileName(file.name) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .neonGlass()
                .padding(bottom = 24.dp)
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
            
            Divider(color = DividerSoft, modifier = Modifier.padding(vertical = 8.dp))
            
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
                if (fileType == TorBoxFileType.VIDEO || fileType == TorBoxFileType.AUDIO) {
                    TorBoxContextMenuItem(
                        icon = Icons.Outlined.PlayArrow,
                        text = "Play",
                        onClick = {
                            if (torBoxClient == null) {
                                onMessage("TorBox client not available")
                                return@TorBoxContextMenuItem
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val link = torBoxClient.getShareLink(file.id)
                                    if (link.isNullOrBlank() || !link.startsWith("http")) {
                                        onMessage("Failed to get stream link")
                                        return@launch
                                    }
                                    val mime = if (fileType == TorBoxFileType.VIDEO) "video/*" else "audio/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(link), mime)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Play with"))
                                        onDismiss()
                                    } catch (_: ActivityNotFoundException) {
                                        onMessage("No compatible player found")
                                    }
                                } catch (e: Exception) {
                                    onMessage("Error: ${e.message ?: "Unknown error"}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }

                if (fileType == TorBoxFileType.IMAGE || fileType == TorBoxFileType.PDF) {
                    TorBoxContextMenuItem(
                        icon = Icons.Outlined.Visibility,
                        text = "View",
                        onClick = {
                            if (torBoxClient == null) {
                                onMessage("TorBox client not available")
                                return@TorBoxContextMenuItem
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val link = torBoxClient.getShareLink(file.id)
                                    if (link.isNullOrBlank() || !link.startsWith("http")) {
                                        onMessage("Failed to get download link")
                                        return@launch
                                    }
                                    val tempFile = TorBoxTempDownloader.downloadToCache(context, link, file.name)
                                    when (fileType) {
                                        TorBoxFileType.IMAGE ->
                                            onOpenViewer(Viewer.Image(tempFile, listOf(tempFile), 0))
                                        TorBoxFileType.PDF ->
                                            onOpenViewer(Viewer.Pdf(tempFile))
                                        else -> Unit
                                    }
                                    onDismiss()
                                } catch (e: Exception) {
                                    onMessage("Unable to open: ${e.message ?: "Unknown error"}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }

                TorBoxContextMenuItem(
                    icon = Icons.Outlined.Download,
                    text = "Download",
                    onClick = {
                        if (torBoxClient == null) {
                            onMessage("TorBox client not available")
                            return@TorBoxContextMenuItem
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val link = torBoxClient.getShareLink(file.id)
                                if (link.isNullOrBlank() || !link.startsWith("http")) {
                                    onMessage("Failed to get download link")
                                    return@launch
                                }
                                TorBoxDownloadManager.enqueue(
                                    context = context,
                                    fileId = file.id,
                                    name = file.name,
                                    url = link
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                onMessage("Error: ${e.message ?: "Unknown error"}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                TorBoxContextMenuItem(
                    icon = Icons.Outlined.Delete,
                    text = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        if (torBoxClient == null) {
                            onMessage("TorBox client not available")
                            return@TorBoxContextMenuItem
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val ok = torBoxClient.deleteFile(file.id)
                                if (ok) {
                                    onMessage("Deleted")
                                    onDeleted()
                                } else {
                                    onMessage("Failed to delete")
                                }
                                onDismiss()
                            } catch (e: Exception) {
                                onMessage("Error: ${e.message ?: "Unknown error"}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                TorBoxContextMenuItem(
                    icon = Icons.Outlined.ContentCopy,
                    text = "Copy download link",
                    onClick = {
                        if (torBoxClient == null) {
                            onMessage("TorBox client not available")
                            return@TorBoxContextMenuItem
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val link = torBoxClient.getShareLink(file.id)
                                if (!link.isNullOrBlank() && link.startsWith("http")) {
                                    val copied = ClipboardUtil.copyText(context, "TorBox link", link)
                                    onMessage(if (copied) "Link copied to clipboard" else "Failed to access clipboard")
                                } else {
                                    onMessage("Failed to get download link")
                                }
                            } catch (e: Exception) {
                                onMessage("Error: ${e.message ?: "Unknown error"}")
                            } finally {
                                isLoading = false
                                onDismiss()
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
