package com.droidexplorer.websim.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.ui.icon.FileIconCache

private val GridCellMinSize = 140.dp

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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridCellMinSize),
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(files, key = { it.path }) { file ->
            val selected = isSelected(file)
            val permissionNeeded = requiresPermission(file)

            Surface(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onClick(file) },
                        onLongClick = { onLongClick(file) }
                    ),
                tonalElevation = if (selected) 4.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
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
                        style = MaterialTheme.typography.bodyMedium
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

@Composable
fun FileIcon(file: FsNode, size: Dp, tint: Color? = null) {
    val extension = remember(file.path) { file.extensionKey() }
    val key = remember(extension, file.isDirectory) {
        "${extension}_${file.isDirectory}"
    }
    val imageVector = FileIconCache.get(key) {
        resolveIconVector(file)
    }
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

private fun resolveIconVector(file: FsNode) =
    if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description

private fun FsNode.extensionKey(): String {
    return when (this) {
        is FsNode.Local -> file.extension
        is FsNode.Saf -> name.substringAfterLast('.', "")
    }
}
