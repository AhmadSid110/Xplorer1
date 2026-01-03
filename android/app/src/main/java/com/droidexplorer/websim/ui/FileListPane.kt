@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.droidexplorer.websim.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.droidexplorer.websim.data.ClipboardOperation
import com.droidexplorer.websim.data.FileClipboard
import com.droidexplorer.websim.core.ops.FileOperation
import com.droidexplorer.websim.core.ops.NodeRef
import com.droidexplorer.websim.core.ops.OperationProgress
import com.droidexplorer.websim.core.ops.OperationResult
import com.droidexplorer.websim.file.FileOperator
import com.droidexplorer.websim.file.SafRequired
import com.droidexplorer.websim.file.FileManager
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType
import com.droidexplorer.websim.file.openFile
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.asFile
import com.droidexplorer.websim.file.isImage
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.settings.ViewMode
import com.droidexplorer.websim.service.FileOperationService
import com.droidexplorer.websim.util.ZipUtils
import com.droidexplorer.websim.ui.viewer.Viewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Minimum cell size for grid view mode. */
private val GridCellMinSize = 140.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListPane(
    modifier: Modifier,
    paneState: PaneState,
    fileOperator: FileOperator,
    safPermissionManager: SafPermissionManager,
    settings: SettingsState,
    onSafRequired: (File) -> Unit,
    onRequestFocus: () -> Unit,
    isActive: Boolean,
    sortType: SortType,
    sortOrder: SortOrder,
    showDivider: Boolean = false,
    onOpenViewer: (Viewer) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var editorFile by remember { mutableStateOf<File?>(null) }
    val operationProgress by FileOperationService.observe().collectAsState(initial = null)
    
    // Context menu state
    var selectedFile by remember { mutableStateOf<FsNode?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val writeProbeCache = remember { mutableStateMapOf<String, Boolean>() }
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // Load files
    val sortFiles = remember(sortType, sortOrder) {
        { files: List<FsNode> ->
            FileManager.sortFiles(files, sortType, sortOrder)
        }
    }

    val files by produceState(
        initialValue = emptyList<FsNode>(),
        paneState.path,
        searchQuery,
        sortType,
        sortOrder,
        settings.showHiddenFiles,
        settings.searchIncludeSaf,
        refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            if (searchQuery.isBlank()) {
                FileManager.list(
                    paneState.path,
                    sortType,
                    sortOrder,
                    settings.showHiddenFiles,
                    safPermissionManager,
                    context
                )
            } else {
                val results = FileManager.search(
                    path = paneState.path,
                    query = searchQuery,
                    showHidden = settings.showHiddenFiles,
                    safPermissionManager = if (settings.searchIncludeSaf) safPermissionManager else null,
                    context = if (settings.searchIncludeSaf) context else null
                )
                sortFiles(results)
            }
        }
    }

    // Show snackbar when message is set
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(operationProgress) {
        val progress = operationProgress ?: return@LaunchedEffect
        when (progress) {
            is OperationProgress.Started -> progress.label?.let { snackbarMessage = it }
            is OperationProgress.Running -> progress.label?.let { snackbarMessage = it }
            is OperationProgress.Completed -> {
                when (val result = progress.result) {
                    is OperationResult.Success -> snackbarMessage = result.message ?: "Operation completed"
                    is OperationResult.Failure -> snackbarMessage = result.message
                    OperationResult.Cancelled -> snackbarMessage = "Operation cancelled"
                }
                refreshTrigger++
            }
        }
    }

    BackHandler(enabled = paneState.canGoBack() && isActive) {
        paneState.goBack()
    }

    val permissionMessage = "Android requires permission to write here.\nSelect this folder once."
    
    fun handleSaf(e: Throwable): Boolean {
        return if (e is SafRequired) {
            onSafRequired(e.directory)
            snackbarMessage = permissionMessage
            true
        } else false
    }

    val openOther: (File) -> Unit = { file ->
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: ActivityNotFoundException) {
            snackbarMessage = "No compatible app found"
        } catch (_: Exception) {
            snackbarMessage = "Unable to open file"
        }
    }

    fun handleOpen(file: File) {
        onRequestFocus()
        if (file.extension.equals("zip", ignoreCase = true)) {
            onOpenViewer(Viewer.Zip(file))
            return
        }
        openFile(
            file = file,
            openText = { onOpenViewer(Viewer.Text(it)) },
            openImage = { target ->
                val images = files.filter { !it.isDirectory && it.asFile().isImage() }.map { it.asFile() }
                val index = images.indexOfFirst { it.absolutePath == target.absolutePath }
                    .takeIf { it >= 0 } ?: 0
                onOpenViewer(Viewer.Image(target, images, index))
            },
            openPdf = { onOpenViewer(Viewer.Pdf(it)) },
            openOther = openOther
        )
    }

    Box(modifier = modifier) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                ScrollableTabRow(
                    selectedTabIndex = paneState.activeTabIndex,
                    edgePadding = 12.dp,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = paneState.tabs
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == paneState.activeTabIndex,
                            onClick = {
                                paneState.selectTab(index)
                                onRequestFocus()
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(File(tab.path).name.ifBlank { tab.path })
                                    if (tabs.size > 1) {
                                        IconButton(
                                            onClick = { paneState.closeTab(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Close tab",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Tab(
                        selected = false,
                        onClick = {
                            paneState.addTab()
                            onRequestFocus()
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "New tab",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("New")
                            }
                        }
                    )
                }

                // Top bar with search only (breadcrumbs moved to scaffold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search toggle
                            IconButton(onClick = { isSearching = !isSearching }) {
                                Icon(
                                    if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Paste button
                            if (FileClipboard.hasItem()) {
                                IconButton(
                                    onClick = {
                                        FileClipboard.item?.let { item ->
                                            val destination = NodeRef.from(File(paneState.path))
                                            val operation = when (item.operation) {
                                                ClipboardOperation.COPY -> FileOperation.Copy(
                                                    NodeRef.from(File(item.sourcePath)),
                                                    destination
                                                )

                                                ClipboardOperation.MOVE -> FileOperation.Move(
                                                    NodeRef.from(File(item.sourcePath)),
                                                    destination
                                                )
                                            }
                                            FileOperationService.enqueue(context, operation)
                                            FileClipboard.clear()
                                            snackbarMessage = "Operation started"
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        }
                        
                        // Search bar (conditional)
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                placeholder = { Text("Search files...") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "Searching…",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .padding(start = 8.dp, bottom = 4.dp)
                                        .semantics { contentDescription = "Search status: Searching" }
                                )
                            }
                        }
                    }
                }

                fun requiresPermission(node: FsNode): Boolean {
                    return when {
                        node is FsNode.Saf -> true
                        node is FsNode.Local && node.isDirectory -> {
                            val cached = writeProbeCache[node.path]
                            val writable = cached ?: FileOperator.canWrite(node.asFile()).also {
                                writeProbeCache[node.path] = it
                            }
                            !writable
                        }
                        else -> false
                    }
                }

                @Composable
                fun FileListContent(showDetails: Boolean) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = files,
                            key = { it.uniqueKey }
                        ) { node ->
                            val requiresSaf = remember(node.uniqueKey) {
                                requiresPermission(node)
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                FileRow(
                                    file = node,
                                    isSelected = selectedFile?.uniqueKey == node.uniqueKey && showContextMenu,
                                    requiresPermission = requiresSaf,
                                    onClick = {
                                        onRequestFocus()
                                        if (node.isDirectory) {
                                            paneState.navigateTo(node.path)
                                            searchQuery = ""
                                        }
                                        else handleOpen(node.asFile())
                                    },
                                    onLongClick = {
                                        selectedFile = node
                                        showContextMenu = true
                                        onRequestFocus()
                                    }
                                )
                                if (showDetails) {
                                    Text(
                                        text = node.path,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 8.dp, bottom = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // File list or empty state
                when (settings.defaultViewMode) {
                    ViewMode.GRID -> {
                        if (files.isEmpty()) {
                            EmptyState(searchQuery.isNotEmpty())
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(GridCellMinSize),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                items(
                                    items = files,
                                    key = { it.uniqueKey }
                                ) { node ->
                                    val requiresSaf = remember(node.uniqueKey) {
                                        requiresPermission(node)
                                    }
                                    FileGridItem(
                                        file = node,
                                        isSelected = selectedFile?.uniqueKey == node.uniqueKey && showContextMenu,
                                        requiresPermission = requiresSaf,
                                        onClick = {
                                            onRequestFocus()
                                            if (node.isDirectory) {
                                                paneState.navigateTo(node.path)
                                                searchQuery = ""
                                            }
                                            else handleOpen(node.asFile())
                                        },
                                        onLongClick = {
                                            selectedFile = node
                                            showContextMenu = true
                                            onRequestFocus()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    ViewMode.LIST -> {
                        if (files.isEmpty()) {
                            EmptyState(searchQuery.isNotEmpty())
                        } else {
                            FileListContent(showDetails = false)
                        }
                    }
                    ViewMode.DETAILS -> {
                        if (files.isEmpty()) {
                            EmptyState(searchQuery.isNotEmpty())
                        } else {
                            FileListContent(showDetails = true)
                        }
                    }
                }
            }
            
            // Vertical divider for dual pane
            if (showDivider) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        
        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Context menu bottom sheet
    if (showContextMenu && selectedFile != null) {
        FileContextMenu(
            file = selectedFile!!.asFile(),
            onDismiss = { 
                showContextMenu = false
            },
            onOpen = { handleOpen(selectedFile!!.asFile()) },
            onEdit = {
                editorFile = selectedFile!!.asFile()
            },
            onRename = { showRenameDialog = true },
            onDelete = { showDeleteDialog = true },
            onCopy = { 
                FileClipboard.copy(selectedFile!!.path)
                snackbarMessage = "Copied to clipboard"
            },
            onMove = { 
                FileClipboard.cut(selectedFile!!.path)
                snackbarMessage = "Cut to clipboard"
            },
            onZip = {
                ZipUtils.zipFile(selectedFile!!.asFile()).fold(
                    onSuccess = { 
                        snackbarMessage = "Zipped successfully"
                        refreshTrigger++
                    },
                    onFailure = { snackbarMessage = "Failed to zip: ${it.message}" }
                )
            },
            onUnzip = {
                ZipUtils.unzip(selectedFile!!.asFile()).fold(
                    onSuccess = { 
                        snackbarMessage = "Unzipped successfully"
                        refreshTrigger++
                    },
                    onFailure = { snackbarMessage = "Failed to unzip: ${it.message}" }
                )
            },
            onShare = { ctx ->
                try {
                    val uri = FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.provider",
                        selectedFile!!.asFile()
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "Share"))
                } catch (e: Exception) {
                    snackbarMessage = "Failed to share: ${e.message}"
                }
            }
        )
    }
    
    // Text editor
    editorFile?.let { target ->
        TextEditorSheet(
            file = target,
            fileOperator = fileOperator,
            onDismiss = { editorFile = null },
            onSaved = { refreshTrigger++ },
            onSafRequired = onSafRequired
        )
    }

    // Rename dialog
    if (showRenameDialog && selectedFile != null) {
        RenameDialog(
            file = selectedFile!!.asFile(),
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                FileOperationService.enqueue(
                    context,
                    FileOperation.Rename(NodeRef.from(selectedFile!!.asFile()), newName)
                )
                snackbarMessage = "Renaming..."
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && selectedFile != null) {
        DeleteConfirmDialog(
            file = selectedFile!!.asFile(),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                FileOperationService.enqueue(
                    context,
                    FileOperation.Delete(NodeRef.from(selectedFile!!.asFile()))
                )
                snackbarMessage = "Deleting..."
            }
        )
    }
}

@Composable
private fun EmptyState(hasSearchQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasSearchQuery) "No files found" else "No files here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun FileGridItem(
    file: FsNode,
    isSelected: Boolean,
    requiresPermission: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null,
                tint = if (requiresPermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                text = file.name.ifBlank { file.path },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (requiresPermission) {
                Text(
                    text = "Permission needed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
