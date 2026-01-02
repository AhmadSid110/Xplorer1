package com.droidexplorer.websim.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualPaneScreen(singlePane: Boolean = false) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    // Use rememberSaveable to persist pane mode across configuration changes
    var paneMode by rememberSaveable { 
        mutableStateOf(if (isTablet && !singlePane) PaneMode.DUAL else PaneMode.SINGLE) 
    }
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showCleanerDialog by remember { mutableStateOf(false) }
    var sortType by rememberSaveable { mutableStateOf(SortType.NAME) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.ASC) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var cleanerResult by remember { mutableStateOf<String?>(null) }
    
    // Show cleaner result
    LaunchedEffect(cleanerResult) {
        cleanerResult?.let {
            snackbarHostState.showSnackbar(it)
            cleanerResult = null
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Xplorer")
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (paneMode == PaneMode.DUAL) "DUAL" else "SINGLE",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Toggle pane mode
                    IconButton(onClick = {
                        paneMode = if (paneMode == PaneMode.DUAL) PaneMode.SINGLE else PaneMode.DUAL
                    }) {
                        Icon(
                            imageVector = if (paneMode == PaneMode.DUAL) 
                                Icons.Filled.Splitscreen 
                                else Icons.Filled.ViewAgenda,
                            contentDescription = "Toggle pane mode"
                        )
                    }
                    
                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        SortMenu(
                            currentSortType = sortType,
                            currentSortOrder = sortOrder,
                            onSortChange = { type, order ->
                                sortType = type
                                sortOrder = order
                            },
                            expanded = showSortMenu,
                            onDismiss = { showSortMenu = false }
                        )
                    }
                    
                    // Cleaner
                    IconButton(onClick = { showCleanerDialog = true }) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = "Cleaner")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left pane (always visible)
            FileListPane(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
                startPath = "/storage/emulated/0",
                showDivider = paneMode == PaneMode.DUAL
            )
            
            // Right pane (only in dual mode)
            if (paneMode == PaneMode.DUAL) {
                FileListPane(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    startPath = "/storage/emulated/0",
                    showDivider = false
                )
            }
        }
    }
    
    // Cleaner dialog
    if (showCleanerDialog) {
        CleanerDialog(
            context = context,
            rootPath = "/storage/emulated/0",
            onDismiss = { showCleanerDialog = false },
            onResult = { cleanerResult = it }
        )
    }
}
