@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.droidexplorer.websim.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.ZipManager
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.util.ZipUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ZipViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var extractMenuExpanded by remember { mutableStateOf(false) }
    
    // State
    var allEntries by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPath by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load ALL entries once
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching { 
                ZipManager.list(file) 
            }.fold(
                onSuccess = { 
                    allEntries = it 
                    isLoading = false
                },
                onFailure = { 
                    error = it.message 
                    isLoading = false
                }
            )
        }
    }

    // Filter entries for current view
    val currentNodes = remember(allEntries, currentPath) {
        val nodes = mutableListOf<ZipEntryNode>()
        val seenFolders = mutableSetOf<String>()
        
        // Define prefix for current level
        val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
        val prefixLen = prefix.length

        allEntries.forEach { entry ->
            if (entry.startsWith(prefix)) {
                val sub = entry.substring(prefixLen)
                if (sub.isNotEmpty()) {
                    val slashIndex = sub.indexOf('/')
                    if (slashIndex == -1) {
                        // It's a file in this folder
                        nodes.add(ZipEntryNode.File(sub, entry))
                    } else {
                        // It's a subfolder
                        val folderName = sub.substring(0, slashIndex)
                        if (folderName !in seenFolders) {
                            seenFolders.add(folderName)
                            nodes.add(ZipEntryNode.Directory(folderName, "$prefix$folderName"))
                        }
                    }
                }
            }
        }
        nodes.sortedBy { it is ZipEntryNode.File } // Folders first
    }

    // Navigation logic
    fun navigateUp() {
        if (currentPath.isEmpty()) {
            onClose()
        } else {
            val lastSlash = currentPath.lastIndexOf('/')
            currentPath = if (lastSlash == -1) "" else currentPath.substring(0, lastSlash)
        }
    }

    BackHandler {
        navigateUp()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlass(radius = 0.dp, alpha = 0.06f)
            ) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            if (currentPath.isNotEmpty()) {
                                Text(currentPath, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        Box {
                            Text(
                                text = "Extract",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable { extractMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = extractMenuExpanded,
                                onDismissRequest = { extractMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Extract here") },
                                    onClick = {
                                        extractMenuExpanded = false
                                        scope.launch {
                                            ZipUtils.extractHere(file).fold(
                                                onSuccess = {
                                                    snackbarHostState.showSnackbar("Extracted here")
                                                },
                                                onFailure = {
                                                    snackbarHostState.showSnackbar("Extract failed: ${it.message}")
                                                }
                                            )
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Extract to ${file.nameWithoutExtension}") },
                                    onClick = {
                                        extractMenuExpanded = false
                                        scope.launch {
                                            ZipUtils.extractToFolder(file).fold(
                                                onSuccess = {
                                                    snackbarHostState.showSnackbar("Extracted to folder")
                                                },
                                                onFailure = {
                                                    snackbarHostState.showSnackbar("Extract failed: ${it.message}")
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (currentNodes.isEmpty()) {
                Text(
                     text = "Empty Folder",
                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentNodes) { node ->
                        ListItem(
                            headlineContent = { Text(node.name) },
                            leadingContent = {
                                Icon(
                                    imageVector = when(node) {
                                        is ZipEntryNode.Directory -> Icons.Outlined.Folder
                                        is ZipEntryNode.File -> Icons.Outlined.Description
                                    },
                                    contentDescription = null,
                                    tint = if (node is ZipEntryNode.Directory) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                if (node is ZipEntryNode.Directory) {
                                    currentPath = node.path
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


