package com.droidexplorer.websim.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

/**
 * Displays files in a detailed list view with size and date information.
 * 
 * CRITICAL VISIBILITY RULE: This composable MUST render ALL files in the provided list.
 * DO NOT add any filtering based on extension, MIME type, category, or file support.
 * Unknown file types must be shown with default icons, NOT hidden.
 * 
 * @param files Complete list of files to display (all will be rendered)
 * @param onClick Callback when a file is clicked
 * @param onLongClick Callback when a file is long-pressed
 * @param isSelected Function to determine if a file is selected
 * @param requiresPermission Function to determine if a file requires permission
 */
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
    val haptic = LocalHapticFeedback.current

    LazyColumn {
        // Render ALL files - no filtering allowed here
        items(files, key = { it.path }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)
            val lastModified = file.lastModified()
            val formattedDate = remember(lastModified) {
                formatter.format(Date(lastModified))
            }
            val sizeBytes = file.size()
            val sizeText = remember(sizeBytes) {
                sizeBytes.sizeReadable()
            }
            
            // Animated selection background
            val surfaceColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 120),
                label = "detailsItemSelection"
            )

            Surface(
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onClick(file) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick(file)
                        }
                    ),
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .background(surfaceColor)
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
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$sizeText$SEPARATOR$formattedDate",
                            style = MaterialTheme.typography.labelSmall,
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

/**
 * Displays files in a simple list view.
 * 
 * CRITICAL VISIBILITY RULE: This composable MUST render ALL files in the provided list.
 * DO NOT add any filtering based on extension, MIME type, category, or file support.
 * Unknown file types must be shown with default icons, NOT hidden.
 * 
 * @param files Complete list of files to display (all will be rendered)
 * @param onClick Callback when a file is clicked
 * @param onLongClick Callback when a file is long-pressed
 * @param isSelected Function to determine if a file is selected
 * @param requiresPermission Function to determine if a file requires permission
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListView(
    files: List<FsNode>,
    onClick: (FsNode) -> Unit = {},
    onLongClick: (FsNode) -> Unit = {},
    isSelected: (FsNode) -> Boolean = { false },
    requiresPermission: (FsNode) -> Boolean = { false }
) {
    val haptic = LocalHapticFeedback.current
    
    LazyColumn {
        // Render ALL files - no filtering allowed here
        items(files, key = { it.path }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)
            
            // Animated selection background
            val surfaceColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 120),
                label = "listItemSelection"
            )

            if (permissionNeeded) {
                RestrictedFolderItem(
                    name = file.name,
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(file)
                    }
                )
            } else {
                Surface(
                    modifier = Modifier
                        .animateItemPlacement()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onClick(file) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick(file)
                            }
                        ),
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .background(surfaceColor)
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
                                style = MaterialTheme.typography.bodyLarge,
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

private fun Long.sizeReadable(): String {
    return when {
        this < BYTES_IN_KB -> "$this B"
        this < BYTES_IN_MB -> String.format(Locale.getDefault(), "%.1f KB", this / BYTES_IN_KB.toDouble())
        this < BYTES_IN_GB -> String.format(Locale.getDefault(), "%.1f MB", this / BYTES_IN_MB.toDouble())
        else -> String.format(Locale.getDefault(), "%.1f GB", this / BYTES_IN_GB.toDouble())
    }
}
