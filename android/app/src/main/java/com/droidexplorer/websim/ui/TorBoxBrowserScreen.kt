@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.droidexplorer.websim.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.storage.TorBoxUiStore
import com.droidexplorer.websim.torbox.TorBoxClient
import com.droidexplorer.websim.torbox.TorBoxDownloadManager
import com.droidexplorer.websim.torbox.TorBoxFileType
import com.droidexplorer.websim.torbox.TorBoxFilter
import com.droidexplorer.websim.torbox.TorBoxItem
import com.droidexplorer.websim.torbox.TorBoxSortMode
import com.droidexplorer.websim.torbox.extractVideoTag
import com.droidexplorer.websim.torbox.formatTorBoxSize
import com.droidexplorer.websim.torbox.torBoxFolderItem
import com.droidexplorer.websim.torbox.torBoxPanePath
import com.droidexplorer.websim.torbox.torBoxPathSegments
import com.droidexplorer.websim.torbox.torBoxSubPath
import com.droidexplorer.websim.ui.selection.SelectionController
import com.droidexplorer.websim.ui.viewer.Viewer
import kotlinx.coroutines.launch

@Composable
fun TorBoxBrowserScreen(
    currentPath: String,
    files: List<TorBoxItem>,
    searchQuery: String,
    torBoxClient: TorBoxClient?,
    onNavigate: (String) -> Unit,
    onOpenViewer: (Viewer) -> Unit,
    onMessage: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val selectionController = remember { SelectionController<TorBoxItem> { it.uniqueKey } }

    val uiStore = remember { TorBoxUiStore(context) }
    var sortMode by remember { mutableStateOf(uiStore.getSortMode()) }
    var sortAscending by remember { mutableStateOf(uiStore.isSortAscending()) }
    var filter by remember { mutableStateOf(uiStore.getFilter()) }
    var contextItem by remember { mutableStateOf<TorBoxItem?>(null) }

    LaunchedEffect(currentPath) {
        selectionController.clear()
    }

    LaunchedEffect(sortMode, sortAscending, filter) {
        uiStore.setSortMode(sortMode)
        uiStore.setSortAscending(sortAscending)
        uiStore.setFilter(filter)
    }

    val subPath = torBoxSubPath(currentPath)
    val currentSegments = torBoxPathSegments(subPath)

    val filteredFiles = files
        .filter { item ->
            if (item.isFolder) return@filter false
            if (searchQuery.isNotBlank() && !item.name.contains(searchQuery, true)) return@filter false
            when (filter) {
                TorBoxFilter.ALL -> true
                TorBoxFilter.VIDEO -> item.type == TorBoxFileType.VIDEO
                TorBoxFilter.AUDIO -> item.type == TorBoxFileType.AUDIO
                TorBoxFilter.IMAGE -> item.type == TorBoxFileType.IMAGE
                TorBoxFilter.ARCHIVE -> item.type == TorBoxFileType.ARCHIVE
                TorBoxFilter.DOC -> item.type == TorBoxFileType.DOC || item.type == TorBoxFileType.PDF
                TorBoxFilter.APK -> item.type == TorBoxFileType.APK
                TorBoxFilter.LARGE -> item.size >= LARGE_FILE_BYTES
                TorBoxFilter.OTHER -> item.type == TorBoxFileType.OTHER
            }
        }

    val folderNames = buildSet {
        filteredFiles.forEach { file ->
            val segments = torBoxPathSegments(file.folderPath)
            if (segments.size > currentSegments.size && segments.take(currentSegments.size) == currentSegments) {
                add(segments[currentSegments.size])
            }
        }
    }.toList().sortedBy { it.lowercase() }

    val folderItems = folderNames.map { folderName ->
        val path = (currentSegments + folderName).joinToString("/")
        torBoxFolderItem(path, folderName)
    }

    val filesInFolder = filteredFiles.filter { file ->
        torBoxPathSegments(file.folderPath) == currentSegments
    }

    val sortedFiles = sortTorBoxFiles(filesInFolder, sortMode, sortAscending)
    val grouped = sortedFiles.groupBy { it.type }.toSortedMap(compareBy { it.name })

    val rows = buildTorBoxRows(folderItems, grouped)
    val selectableFiles = rows.mapNotNull { row -> (row as? TorBoxRow.FileRow)?.file }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BreadcrumbRow(
            currentSegments = currentSegments,
            onNavigate = { segmentPath -> onNavigate(torBoxPanePath(segmentPath)) }
        )

        SortFilterRow(
            sortMode = sortMode,
            sortAscending = sortAscending,
            onSortModeChange = { sortMode = it },
            onSortOrderToggle = { sortAscending = !sortAscending }
        )

        FilterChipsRow(
            filter = filter,
            onFilterChange = { filter = it }
        )

        if (selectionController.selected().isNotEmpty()) {
            SelectionBar(
                count = selectionController.selected().size,
                onClear = { selectionController.clear() },
                onDelete = {
                    if (torBoxClient == null) {
                        onMessage("TorBox client not available")
                        return@SelectionBar
                    }
                    scope.launch {
                        val selected = selectionController.currentSelection(selectableFiles)
                        selected.forEach { item ->
                            val id = item.id ?: return@forEach
                            torBoxClient.deleteFile(id)
                        }
                        selectionController.clear()
                        onRefresh()
                        onMessage("Deleted ${selected.size} items")
                    }
                }
            )
        }

        if (rows.isEmpty()) {
            TorBoxEmptyState(hasSearchQuery = searchQuery.isNotBlank())
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(rows) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                selectRowAtOffset(offset, listState, rows, selectableFiles, selectionController)
                            },
                            onDrag = { change, _ ->
                                selectRowAtOffset(change.position, listState, rows, selectableFiles, selectionController)
                            }
                        )
                    }
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }) { _, row ->
                    when (row) {
                        is TorBoxRow.HeaderRow -> {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                            )
                        }
                        is TorBoxRow.FolderRow -> {
                            FolderRow(
                                folder = row.folder,
                                onClick = { onNavigate(torBoxPanePath(row.folder.fullPath)) }
                            )
                        }
                        is TorBoxRow.FileRow -> {
                            TorBoxFileCard(
                                file = row.file,
                                torBoxClient = torBoxClient,
                                isSelected = selectionController.isSelected(row.file),
                                onClick = {
                                    if (selectionController.selected().isNotEmpty()) {
                                        selectionController.toggle(row.file)
                                    } else {
                                        contextItem = row.file
                                    }
                                },
                                onLongClick = {
                                    selectionController.toggle(row.file)
                                },
                                onDownload = {
                                    if (torBoxClient == null) {
                                        onMessage("TorBox client not available")
                                        return@TorBoxFileCard
                                    }
                                    scope.launch {
                                        val link = torBoxClient.getShareLink(row.file.id ?: return@launch)
                                        if (link.isNullOrBlank()) {
                                            onMessage("Failed to get download link")
                                            return@launch
                                        }
                                        TorBoxDownloadManager.enqueue(
                                            context = context,
                                            fileId = row.file.id ?: return@launch,
                                            name = row.file.name,
                                            url = link
                                        )
                                    }
                                },
                                onPlay = {
                                    if (torBoxClient == null) {
                                        onMessage("TorBox client not available")
                                        return@TorBoxFileCard
                                    }
                                    scope.launch {
                                        val link = torBoxClient.getShareLink(row.file.id ?: return@launch)
                                        if (link.isNullOrBlank()) {
                                            onMessage("Failed to get stream link")
                                            return@launch
                                        }
                                        val mime = if (row.file.type == TorBoxFileType.VIDEO) "video/*" else "audio/*"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(link), mime)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                            try {
                                                context.startActivity(Intent.createChooser(intent, "Play with"))
                                            } catch (_: ActivityNotFoundException) {
                                                onMessage("No compatible player found")
                                            }
                                    }
                                },
                                onMore = { contextItem = row.file }
                            )
                        }
                    }
                }
            }
        }
    }

    contextItem?.let { file ->
        val fsNode = FsNode.TorBox(
            id = file.id ?: return@let,
            name = file.name,
            size = file.size,
            absolutePath = file.fullPath
        )
        TorBoxContextMenu(
            file = fsNode,
            torBoxClient = torBoxClient,
            onDismiss = { contextItem = null },
            onOpenViewer = onOpenViewer,
            onMessage = onMessage,
            onDeleted = onRefresh
        )
    }
}

private fun buildTorBoxRows(
    folders: List<TorBoxItem>,
    groupedFiles: Map<TorBoxFileType, List<TorBoxItem>>
): List<TorBoxRow> {
    val rows = mutableListOf<TorBoxRow>()
    folders.forEach { rows.add(TorBoxRow.FolderRow(it)) }
    groupedFiles.forEach { (type, files) ->
        if (files.isNotEmpty()) {
            rows.add(TorBoxRow.HeaderRow(typeLabel(type)))
            files.forEach { rows.add(TorBoxRow.FileRow(it)) }
        }
    }
    return rows
}

private fun sortTorBoxFiles(
    files: List<TorBoxItem>,
    mode: TorBoxSortMode,
    asc: Boolean
): List<TorBoxItem> {
    val sorted = when (mode) {
        TorBoxSortMode.NAME -> files.sortedBy { it.name.lowercase() }
        TorBoxSortMode.SIZE -> files.sortedBy { it.size }
        TorBoxSortMode.DATE -> files.sortedBy { it.modified }
        TorBoxSortMode.TYPE -> files.sortedBy { it.type.name }
    }
    return if (asc) sorted else sorted.reversed()
}

private fun typeLabel(type: TorBoxFileType): String = when (type) {
    TorBoxFileType.VIDEO -> "Videos"
    TorBoxFileType.AUDIO -> "Audio"
    TorBoxFileType.IMAGE -> "Images"
    TorBoxFileType.ARCHIVE -> "Archives"
    TorBoxFileType.DOC -> "Documents"
    TorBoxFileType.APK -> "APKs"
    TorBoxFileType.PDF -> "PDFs"
    TorBoxFileType.OTHER -> "Other"
}

@Composable
private fun BreadcrumbRow(
    currentSegments: List<String>,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TorBox",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onNavigate("") }
        )
        currentSegments.forEachIndexed { index, segment ->
            Text(text = ">", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = segment,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable {
                    val path = currentSegments.take(index + 1).joinToString("/")
                    onNavigate(path)
                }
            )
        }
    }
}

@Composable
private fun SortFilterRow(
    sortMode: TorBoxSortMode,
    sortAscending: Boolean,
    onSortModeChange: (TorBoxSortMode) -> Unit,
    onSortOrderToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = sortMode == TorBoxSortMode.NAME,
            onClick = { onSortModeChange(TorBoxSortMode.NAME) },
            label = { Text("Name") }
        )
        FilterChip(
            selected = sortMode == TorBoxSortMode.SIZE,
            onClick = { onSortModeChange(TorBoxSortMode.SIZE) },
            label = { Text("Size") }
        )
        FilterChip(
            selected = sortMode == TorBoxSortMode.DATE,
            onClick = { onSortModeChange(TorBoxSortMode.DATE) },
            label = { Text("Date") }
        )
        FilterChip(
            selected = sortMode == TorBoxSortMode.TYPE,
            onClick = { onSortModeChange(TorBoxSortMode.TYPE) },
            label = { Text("Type") }
        )
        IconButton(onClick = onSortOrderToggle) {
            Icon(
                imageVector = Icons.Outlined.ViewList,
                contentDescription = "Sort order",
                tint = if (sortAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    filter: TorBoxFilter,
    onFilterChange: (TorBoxFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TorBoxFilter.values().forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = { onFilterChange(item) },
                label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$count selected", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClear)
                )
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: TorBoxItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TorBoxFileCard(
    file: TorBoxItem,
    torBoxClient: TorBoxClient?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    val sizeColor = sizeColor(file.size)
    val tag = extractVideoTag(file.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TorBoxTypeBadge(file = file, torBoxClient = torBoxClient)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val info = buildString {
                    append(formatTorBoxSize(file.size))
                    if (!tag.isNullOrBlank()) append(" • $tag")
                }
                Text(
                    text = info,
                    style = MaterialTheme.typography.labelSmall,
                    color = sizeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (file.type == TorBoxFileType.VIDEO || file.type == TorBoxFileType.AUDIO) {
                    IconButton(onClick = onPlay) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play")
                    }
                }
                IconButton(onClick = onDownload) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = "Download")
                }
                IconButton(onClick = onMore) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More")
                }
            }
        }
    }
}

@Composable
private fun TorBoxTypeBadge(
    file: TorBoxItem,
    torBoxClient: TorBoxClient?
) {
    val painter = rememberImagePainter(file, torBoxClient)
    if (painter != null) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(46.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon(file.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun rememberImagePainter(
    file: TorBoxItem,
    torBoxClient: TorBoxClient?
): androidx.compose.ui.graphics.painter.Painter? {
    if (file.type != TorBoxFileType.IMAGE || torBoxClient == null || file.id.isNullOrBlank()) return null
    var link by remember(file.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.id) {
        link = torBoxClient.getShareLink(file.id)
    }

    return link?.let { rememberAsyncImagePainter(model = it) }
}

private fun typeIcon(type: TorBoxFileType) = when (type) {
    TorBoxFileType.VIDEO -> Icons.Outlined.VideoFile
    TorBoxFileType.AUDIO -> Icons.Outlined.AudioFile
    TorBoxFileType.IMAGE -> Icons.Outlined.Image
    TorBoxFileType.ARCHIVE -> Icons.Outlined.Archive
    TorBoxFileType.DOC, TorBoxFileType.PDF -> Icons.Outlined.Description
    TorBoxFileType.APK -> Icons.Outlined.Android
    TorBoxFileType.OTHER -> Icons.Outlined.Description
}

private fun sizeColor(bytes: Long): Color {
    return when {
        bytes >= HUGE_FILE_BYTES -> Color(0xFF00B0FF)
        bytes >= LARGE_FILE_BYTES -> Color(0xFF2EC4FF)
        else -> Color.Gray
    }
}

private fun selectRowAtOffset(
    offset: Offset,
    listState: LazyListState,
    rows: List<TorBoxRow>,
    selectableFiles: List<TorBoxItem>,
    selectionController: SelectionController<TorBoxItem>
) {
    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        val top = info.offset
        val bottom = info.offset + info.size
        offset.y.toInt() in top..bottom
    } ?: return
    val row = rows.getOrNull(itemInfo.index) ?: return
    if (row is TorBoxRow.FileRow) {
        selectionController.selectRange(selectableFiles, row.file)
    }
}

private sealed class TorBoxRow(val key: String) {
    class HeaderRow(val title: String) : TorBoxRow("header:$title")
    class FolderRow(val folder: TorBoxItem) : TorBoxRow(folder.uniqueKey)
    class FileRow(val file: TorBoxItem) : TorBoxRow(file.uniqueKey)
}

@Composable
private fun TorBoxEmptyState(hasSearchQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (hasSearchQuery) "No results found" else "This folder is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasSearchQuery) "Try another keyword" else "No files to display",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private const val LARGE_FILE_BYTES = 500L * 1024L * 1024L
private const val HUGE_FILE_BYTES = 2L * 1024L * 1024L * 1024L
