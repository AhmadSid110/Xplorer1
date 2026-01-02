package com.droidexplorer.websim.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.droidexplorer.websim.data.FileClipboard
import com.droidexplorer.websim.file.FileManager
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType
import com.droidexplorer.websim.util.ZipUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListPane(
    modifier: Modifier,
    startPath: String,
    showDivider: Boolean = false
) {
    val context = LocalContext.current
    val navigator = remember { PaneNavigator(startPath) }
    var path by rememberSaveable { mutableStateOf(startPath) }
    var sortType by rememberSaveable { mutableStateOf(SortType.NAME) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.ASC) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Context menu state
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // Load files
    val files = remember(path, sortType, sortOrder, searchQuery, refreshTrigger) {
        isLoading = true
        val result = if (searchQuery.isNotEmpty()) {
            FileManager.search(path, searchQuery)
        } else {
            FileManager.list(path, sortType, sortOrder)
        }
        isLoading = false
        result
    }

    // Show snackbar when message is set
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    BackHandler(enabled = navigator.canGoBack()) {
        navigator.goBack()
        path = navigator.currentPath
    }

    Box(modifier = modifier) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                // Top bar with navigation and search
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        // Navigation row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    navigator.goBack()
                                    path = navigator.currentPath
                                },
                                enabled = navigator.canGoBack()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (navigator.canGoBack()) 
                                        MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    navigator.goForward()
                                    path = navigator.currentPath
                                },
                                enabled = navigator.canGoForward()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (navigator.canGoForward()) 
                                        MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Search toggle
                            IconButton(onClick = { isSearching = !isSearching }) {
                                Icon(
                                    if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
                            
                            // Paste button
                            if (FileClipboard.hasItem()) {
                                IconButton(
                                    onClick = {
                                        FileClipboard.item?.let { item ->
                                            val result = FileManager.paste(item, path)
                                            result.fold(
                                                onSuccess = { 
                                                    snackbarMessage = "Pasted successfully"
                                                    FileClipboard.clear()
                                                    refreshTrigger++
                                                },
                                                onFailure = { snackbarMessage = "Failed to paste: ${it.message}" }
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
                        }
                        
                        // Breadcrumb navigation
                        BreadcrumbBar(
                            currentPath = path,
                            onNavigateToPath = { newPath ->
                                navigator.navigateToPath(newPath)
                                path = navigator.currentPath
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                
                // Loading indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // File list or empty state
                if (files.isEmpty() && !isLoading) {
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
                            FileRow(
                                file = file,
                                isSelected = selectedFile?.absolutePath == file.absolutePath && showContextMenu,
                                onClick = {
                                    if (file.isDirectory) {
                                        navigator.navigateTo(file.absolutePath)
                                        path = navigator.currentPath
                                    }
                                },
                                onLongClick = {
                                    selectedFile = file
                                    showContextMenu = true
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
    
    // Rename dialog
    if (showRenameDialog && selectedFile != null) {
        RenameDialog(
            file = selectedFile!!,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                FileManager.rename(selectedFile!!, newName).fold(
                    onSuccess = { 
                        snackbarMessage = "Renamed successfully"
                        refreshTrigger++
                    },
                    onFailure = { snackbarMessage = "Failed to rename: ${it.message}" }
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
                FileManager.delete(selectedFile!!).fold(
                    onSuccess = { 
                        snackbarMessage = "Deleted successfully"
                        refreshTrigger++
                    },
                    onFailure = { snackbarMessage = "Failed to delete: ${it.message}" }
                )
            }
        )
    }
}
