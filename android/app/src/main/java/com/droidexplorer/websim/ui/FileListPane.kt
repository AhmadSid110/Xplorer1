@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.droidexplorer.websim.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.droidexplorer.websim.search.SearchResult
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.settings.ViewMode
import com.droidexplorer.websim.service.FileOperationService
import com.droidexplorer.websim.util.ZipUtils
import com.droidexplorer.websim.ui.viewer.Viewer
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.selection.SelectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Minimum cell size for grid view mode. */
private val GridCellMinSize = 140.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListPane(
    modifier: Modifier,
    paneState: PaneState,
    currentPath: String,
    fileOperator: FileOperator,
    safPermissionManager: SafPermissionManager,
    settings: SettingsState,
    searchQuery: String,
    searchResult: SearchResult?,
    permissionRefresh: Int,
    onSearchQueryChange: (String) -> Unit,
    onSafRequired: (File) -> Unit,
    onRequestSafAccess: (FsNode) -> Unit,
    onSelectionChange: (FsNode?) -> Unit,
    clearSelectionSignal: Int,
    renameSelectionSignal: Int,
    deleteSelectionSignal: Int,
    onRequestFocus: () -> Unit,
    isActive: Boolean,
    sortType: SortType,
    sortOrder: SortOrder,
    showDivider: Boolean = false,
    onOpenViewer: (Viewer) -> Unit = {},
    torBoxClient: com.droidexplorer.websim.torbox.TorBoxClient? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var isSearching by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var editorFile by remember { mutableStateOf<File?>(null) }
    var restrictedTarget by remember { mutableStateOf<FsNode?>(null) }
    var showExplain by remember { mutableStateOf(false) }
    val operationProgress by FileOperationService.observe().collectAsState(initial = null)


    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
        }
    }

    // Context menu state
    var selectedFile by remember { mutableStateOf<FsNode?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectionController = remember { SelectionController<FsNode> { it.uniqueKey } }
    val writeProbeCache = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(permissionRefresh) {
        writeProbeCache.clear()
        if (permissionRefresh > 0) {
            refreshTrigger++
        }
    }

    LaunchedEffect(clearSelectionSignal) {
        selectedFile = null
        showContextMenu = false
        selectionController.clear()
        onSelectionChange(null)
    }

    LaunchedEffect(renameSelectionSignal) {
        if (selectedFile != null) {
            showRenameDialog = true
        }
    }

    LaunchedEffect(deleteSelectionSignal) {
        if (selectedFile != null) {
            showDeleteDialog = true
        }
    }

    LaunchedEffect(selectedFile, showContextMenu) {
        onSelectionChange(selectedFile?.takeIf { showContextMenu })
    }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // ─────────────────────────────────────────────
    // Load files (FINAL – TorBox + Local SAFE)
    // ─────────────────────────────────────────────

    val files by produceState<List<FsNode>>(
        initialValue = emptyList(),
        currentPath,
        permissionRefresh,
        refreshTrigger
    ) {
        value = if (currentPath == "torbox:") {
            torBoxClient?.listFiles()?.map {
                FsNode.TorBox(
                    id = it.id,
                    name = it.name,
                    size = it.size,
                    absolutePath = it.absolutePath
                )
            } ?: emptyList()
        } else {
            FileManager.list(
                currentPath,
                sortType,
                sortOrder,
                settings.showHiddenFiles,
                safPermissionManager,
                context
            )
        }
    }

    val isSearchActive = searchQuery.isNotBlank()
    val activeFiles = if (isSearchActive) {
        FileManager.sortFiles(searchResult?.matches ?: emptyList(), sortType, sortOrder)
    } else {
        files
    }
    val searchLoading = isSearchActive && searchResult == null

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
            restrictedTarget = FsNode.Local(e.directory)
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
        val ext = file.extension.lowercase()
        if (ext == "apk") {
            openApk(context, file)
            return
        }
        if (ext == "zip") {
            onOpenViewer(Viewer.Zip(file))
            return
        }
        if (ext in codeExtensions) {
            onOpenViewer(Viewer.Code(file, langFor(ext)))
            return
        }
        openFile(
            file = file,
            openText = { onOpenViewer(Viewer.Text(it)) },
            openImage = { target ->
                val images = activeFiles
                    .filter { !it.isDirectory }
                    .map { it.asFile() }
                    .filter { it.isImage() }
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

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    tonalElevation = 0.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSearching = !isSearching
                            }) {
                                Icon(
                                    if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

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

                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { onSearchQueryChange(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                placeholder = { Text("Search files...") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSearchQueryChange("")
                                        }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                if (searchQuery.isNotBlank()) {
                    SearchStatusBanner(searchResult?.skippedRoots ?: emptyList())
                }

                fun requiresPermission(node: FsNode): Boolean {
                    if (node is FsNode.TorBox) return false
                    val file = node.asFile()
                    if (!file.isDirectory) return false
                    if (file.isDirectory && safPermissionManager.isPersisted(file)) return false
                    if (file.isDirectory && safPermissionManager.isRevoked(file)) return true
                    return when {
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

                val handleRestricted: (FsNode) -> Unit = { node ->
                    restrictedTarget = node
                    onRequestFocus()
                }
                val handleItemClick: (FsNode) -> Unit = { node ->
                    onRequestFocus()
                    if (requiresPermission(node)) {
                        handleRestricted(node)
                    } else {
                        when (node) {
                            is FsNode.TorBox -> {
                                selectedFile = node
                                selectionController.clear()
                                selectionController.select(node)
                                showContextMenu = true
                            }
                            else -> {
                                if (node.isDirectory) {
                                    paneState.navigateTo(node.path)
                                    onSearchQueryChange("")
                                } else {
                                    handleOpen(node.asFile())
                                }
                            }
                        }
                    }
                }
                val handleItemLongClick: (FsNode) -> Unit = { node ->
                    onRequestFocus()
                    if (requiresPermission(node)) {
                        handleRestricted(node)
                        showContextMenu = false
                    } else {
                        when (node) {
                            is FsNode.TorBox -> {
                                selectedFile = node
                                selectionController.clear()
                                selectionController.select(node)
                                showContextMenu = true
                            }
                            else -> {
                                selectedFile = node
                                selectionController.clear()
                                selectionController.select(node)
                                showContextMenu = true
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = paneState.path,
                    transitionSpec = {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    },
                    label = "directoryTransition"
                ) { _ ->
                    when (settings.defaultViewMode) {
                        ViewMode.LIST -> {
                            if (searchLoading) {
                                SearchLoadingState()
                            } else if (activeFiles.isEmpty()) {
                                EmptyState(searchQuery.isNotEmpty())
                            } else {
                                FileListView(
                                    files = activeFiles,
                                    onClick = handleItemClick,
                                    onLongClick = handleItemLongClick,
                                    isSelected = { selectionController.isSelected(it) && showContextMenu },
                                    requiresPermission = { requiresPermission(it) }
                                )
                            }
                        }
                        ViewMode.GRID -> {
                            if (searchLoading) {
                                SearchLoadingState()
                            } else if (activeFiles.isEmpty()) {
                                EmptyState(searchQuery.isNotEmpty())
                            } else {
                                FileGridView(
                                    files = activeFiles,
                                    onClick = handleItemClick,
                                    onLongClick = handleItemLongClick,
                                    isSelected = { selectionController.isSelected(it) && showContextMenu },
                                    requiresPermission = { requiresPermission(it) }
                                )
                            }
                        }
                        ViewMode.DETAILS -> {
                            if (searchLoading) {
                                SearchLoadingState()
                            } else if (activeFiles.isEmpty()) {
                                EmptyState(searchQuery.isNotEmpty())
                            } else {
                                FileDetailsView(
                                    files = activeFiles,
                                    onClick = handleItemClick,
                                    onLongClick = handleItemLongClick,
                                    isSelected = { selectionController.isSelected(it) && showContextMenu },
                                    requiresPermission = { requiresPermission(it) }
                                )
                            }
                        }
                    }
                }
            }

            if (showDivider) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Debug overlay removed
    }

    restrictedTarget?.let { target ->
        RestrictedFolderMenu(
            folderName = target.name,
            onGrantAccess = { onRequestSafAccess(target) },
            onExplain = { showExplain = true },
            onDismiss = { restrictedTarget = null }
        )
    }

    if (showExplain) {
        PermissionExplanationDialog { showExplain = false }
    }

    if (showContextMenu && selectedFile != null) {
        when (val file = selectedFile!!) {
            is FsNode.TorBox -> {
                TorBoxContextMenu(
                    file = file,
                    torBoxClient = torBoxClient,
                    onDismiss = { showContextMenu = false },
                    onOpenViewer = onOpenViewer,
                    onMessage = { message ->
                        snackbarMessage = message
                    },
                    onDeleted = {
                        refreshTrigger++
                    }
                )
            }
            else -> {
                FileContextMenu(
                    file = file.asFile(),
                    onDismiss = { 
                        showContextMenu = false
                    },
                    onOpen = { handleOpen(file.asFile()) },
                    onEdit = {
                        editorFile = file.asFile()
                    },
                    onRename = { showRenameDialog = true },
                    onDelete = { showDeleteDialog = true },
                    onCopy = { 
                        FileClipboard.copy(file.path)
                        snackbarMessage = "Copied to clipboard"
                    },
                    onMove = { 
                        FileClipboard.cut(file.path)
                        snackbarMessage = "Cut to clipboard"
                    },
                    onZip = {
                        ZipUtils.zipFile(file.asFile()).fold(
                            onSuccess = { 
                                snackbarMessage = "Zipped successfully"
                                refreshTrigger++
                            },
                            onFailure = { snackbarMessage = "Failed to zip: ${it.message}" }
                        )
                    },
                    onUnzip = {
                        ZipUtils.unzip(file.asFile()).fold(
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
                                file.asFile()
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
        }
    }

    editorFile?.let { target ->
        TextEditorSheet(
            file = target,
            fileOperator = fileOperator,
            onDismiss = { editorFile = null },
            onSaved = { refreshTrigger++ },
            onSafRequired = onSafRequired
        )
    }

    if (showRenameDialog && selectedFile != null && selectedFile !is FsNode.TorBox) {
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

    if (showDeleteDialog && selectedFile != null && selectedFile !is FsNode.TorBox) {
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
fun PermissionExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.neonGlass(),
        containerColor = Color.Transparent,
        title = { Text("Why is this restricted?") },
        text = {
            Text("Android restricts some folders for privacy. Grant access to manage their contents.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasSearchQuery) "No results found" else "This folder is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasSearchQuery) "Try another keyword" else "No files to display",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SearchLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Searching storage…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val codeExtensions = setOf(
    "py", "kt", "java", "json", "xml", "js", "ts", "c", "cpp", "h", "sh"
)

private fun langFor(ext: String): String = when (ext.lowercase()) {
    "py" -> "python"
    "kt" -> "kotlin"
    "java" -> "java"
    "json" -> "json"
    "xml" -> "xml"
    "js" -> "javascript"
    "ts" -> "typescript"
    "c" -> "c"
    "cpp" -> "cpp"
    "h" -> "cpp"
    "sh" -> "bash"
    else -> "plaintext"
}

private fun openApk(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No installer found", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Permission denied opening APK", Toast.LENGTH_SHORT).show()
    } catch (_: IllegalArgumentException) {
        Toast.makeText(context, "Unable to open APK", Toast.LENGTH_SHORT).show()
    }
}
