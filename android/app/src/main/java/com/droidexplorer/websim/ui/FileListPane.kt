package com.droidexplorer.websim.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.droidexplorer.websim.data.FileClipboard
import com.droidexplorer.websim.file.FileOperator
import com.droidexplorer.websim.file.SafRequired
import com.droidexplorer.websim.file.FileManager
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType
import com.droidexplorer.websim.file.openFile
import com.droidexplorer.websim.util.ZipUtils
import com.droidexplorer.websim.ui.viewer.Viewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListPane(
    modifier: Modifier,
    paneState: PaneState,
    fileOperator: FileOperator,
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
    var viewerFile by remember { mutableStateOf<File?>(null) }
    var editorFile by remember { mutableStateOf<File?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    
    // Context menu state
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val writeProbeCache = remember { mutableStateMapOf<String, Boolean>() }
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // Load files
    val sortFiles = remember(sortType, sortOrder) {
        { files: List<File> ->
            FileManager.sortFiles(files, sortType, sortOrder)
        }
    }

    val files by produceState(
        initialValue = emptyList<File>(),
        paneState.path,
        searchQuery,
        sortType,
        sortOrder,
        refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            if (searchQuery.isBlank()) {
                FileManager.list(paneState.path, sortType, sortOrder)
            } else {
                sortFiles(
                    FileManager.searchRecursive(
                        root = File(paneState.path),
                        query = searchQuery
                    )
                )
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
            openText = { viewerFile = it },
            openImage = { imageFile = it },
            openPdf = { onOpenViewer(Viewer.Pdf(it)) },
            openOther = openOther
        )
    }

    Box(modifier = modifier) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
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
                                            val result = fileOperator.performClipboard(
                                                item,
                                                File(paneState.path)
                                            )
                                            result.fold(
                                                onSuccess = { 
                                                    snackbarMessage = "Pasted successfully"
                                                    FileClipboard.clear()
                                                    refreshTrigger++
                                                },
                                                onFailure = { 
                                                    if (!handleSaf(it)) {
                                                        snackbarMessage = "Failed to paste: ${it.message}"
                                                    }
                                                }
                                            )
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
                                    text = "Searching all subfolders…",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .padding(start = 8.dp, bottom = 4.dp)
                                        .semantics { contentDescription = "Search status: Searching all subfolders" }
                                )
                            }
                        }
                    }
                }
                
                // File list or empty state
                if (files.isEmpty()) {
                    // Empty state
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
                                text = if (searchQuery.isNotEmpty()) "No files found" else "No files here",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = files,
                            key = { it.absolutePath }
                        ) { file ->
                            val requiresSaf = remember(file.absolutePath) {
                                if (!file.isDirectory) {
                                    false
                                } else {
                                    val cached = writeProbeCache[file.absolutePath]
                                    val writable = cached ?: FileOperator.canWrite(file).also {
                                        writeProbeCache[file.absolutePath] = it
                                    }
                                    !writable
                                }
                            }
                            FileRow(
                                file = file,
                                isSelected = selectedFile?.absolutePath == file.absolutePath && showContextMenu,
                                requiresPermission = requiresSaf,
                                onClick = {
                                    onRequestFocus()
                                    if (file.isDirectory) {
                                        paneState.navigateTo(file.absolutePath)
                                        searchQuery = ""
                                    }
                                    else handleOpen(file)
                                },
                                onLongClick = {
                                    selectedFile = file
                                    showContextMenu = true
                                    onRequestFocus()
                                }
                            )
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
            file = selectedFile!!,
            onDismiss = { 
                showContextMenu = false
            },
            onOpen = { handleOpen(selectedFile!!) },
            onEdit = {
                editorFile = selectedFile
            },
            onRename = { showRenameDialog = true },
            onDelete = { showDeleteDialog = true },
            onCopy = { 
                FileClipboard.copy(selectedFile!!.absolutePath)
                snackbarMessage = "Copied to clipboard"
            },
            onMove = { 
                FileClipboard.cut(selectedFile!!.absolutePath)
                snackbarMessage = "Cut to clipboard"
            },
            onZip = {
                ZipUtils.zipFile(selectedFile!!).fold(
                    onSuccess = { 
                        snackbarMessage = "Zipped successfully"
                        refreshTrigger++
                    },
                    onFailure = { snackbarMessage = "Failed to zip: ${it.message}" }
                )
            },
            onUnzip = {
                ZipUtils.unzip(selectedFile!!).fold(
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
                        selectedFile!!
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
    
    // Text viewer
    viewerFile?.let { target ->
        TextViewerSheet(
            file = target,
            fileOperator = fileOperator,
            onDismiss = { viewerFile = null },
            onEdit = { editorFile = target },
            onSafRequired = onSafRequired
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

    imageFile?.let { target ->
        ImageViewerSheet(
            file = target,
            onDismiss = { imageFile = null }
        )
    }

    // Rename dialog
    if (showRenameDialog && selectedFile != null) {
        RenameDialog(
            file = selectedFile!!,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                fileOperator.rename(selectedFile!!, newName).fold(
                    onSuccess = { 
                        snackbarMessage = "Renamed successfully"
                        refreshTrigger++
                    },
                    onFailure = { 
                        if (!handleSaf(it)) {
                            snackbarMessage = "Failed to rename: ${it.message}"
                        }
                    }
                )
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && selectedFile != null) {
        DeleteConfirmDialog(
            file = selectedFile!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                fileOperator.delete(selectedFile!!).fold(
                    onSuccess = { 
                        snackbarMessage = "Deleted successfully"
                        refreshTrigger++
                    },
                    onFailure = { 
                        if (!handleSaf(it)) {
                            snackbarMessage = "Failed to delete: ${it.message}"
                        }
                    }
                )
            }
        )
    }
}
