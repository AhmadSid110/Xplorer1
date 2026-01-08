package com.droidexplorer.websim.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.ui.icons.fileIconFor

private val GridCellMinSize = 140.dp

/**
 * Displays files in a grid layout.
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
 * @param modifier Modifier for the grid
 * @param contentPadding Padding around the grid content
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridView(
    files: List<FsNode>,
    onClick: (FsNode) -> Unit = {},
    onLongClick: (FsNode) -> Unit = {},
    isSelected: (FsNode) -> Boolean = { false },
    requiresPermission: (FsNode) -> Boolean = { false },
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 4.dp),
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
) {
    val haptic = LocalHapticFeedback.current
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridCellMinSize),
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        // Render ALL files - no filtering allowed here
        items(files, key = { it.path }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)
            
            // Animated selection background
            val surfaceColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                },
                animationSpec = tween(durationMillis = 120),
                label = "gridItemSelection"
            )

            Surface(
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(8.dp)
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
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .background(surfaceColor)
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileIcon(
                        file = file,
                        size = 48.dp,
                        tint = if (permissionNeeded) MaterialTheme.colorScheme.error else null
                    )
                    Text(
                        text = file.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
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

/**
 * Displays an icon for a file or directory.
 * 
 * CRITICAL: Icon resolution failure must NOT hide the file.
 * If icon cannot be determined, a default icon (Description) is used.
 * This ensures ALL files remain visible even with unknown extensions.
 * 
 * @param file The file/directory to display an icon for
 * @param size Size of the icon
 * @param tint Optional color tint for the icon
 */
@Composable
fun FileIcon(file: FsNode, size: Dp, tint: Color? = null) {
    val imageVector = resolveIconVector(file)
    val iconTint = tint ?: if (file.isDirectory) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        imageVector = imageVector,
        contentDescription = if (file.isDirectory) "Folder" else "File",
        tint = iconTint,
        modifier = Modifier
            .padding(4.dp)
            .size(size)
    )
}

/**
 * Resolves the icon for a file or directory.
 * 
 * GUARANTEED FALLBACK: Always returns a valid icon (never null).
 * - Directories: Folder icon
 * - Files: Description icon (default for all file types)
 * 
 * This ensures unknown file types are still visible with a generic icon.
 */
@Composable
private fun resolveIconVector(file: FsNode) =
    if (file.isDirectory) Icons.Filled.Folder else fileIconFor(file)
