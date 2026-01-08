package com.droidexplorer.websim.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileOperator
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.search.SearchResult
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.storage.DataStoreSafStore
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.ui.viewer.CodeViewerScreen
import com.droidexplorer.websim.ui.viewer.PdfViewerScreen
import com.droidexplorer.websim.ui.viewer.ImageViewerScreen
import com.droidexplorer.websim.ui.viewer.TextViewerScreen
import com.droidexplorer.websim.ui.viewer.Viewer
import com.droidexplorer.websim.ui.viewer.ZipViewerScreen
import com.droidexplorer.websim.ui.glass.GlassSurface
import com.droidexplorer.websim.ui.theme.backgroundGradient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualPaneScreen(
    singlePane: Boolean = false,
    settings: SettingsState,
    searchQuery: String,
    searchResult: SearchResult?,
    permissionRefresh: Int,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onRequestSafAccess: (FsNode) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val safStore = remember { DataStoreSafStore(context) }
    val safManager = remember { SafPermissionManager(context, safStore) }
    val fileOperator = remember { FileOperator(context, safManager) }
    val defaultPath = "/storage/emulated/0"
    val leftPaneState = rememberSaveable(saver = PaneState.saver(defaultPath)) {
        PaneState.initial(defaultPath)
    }
    val rightPaneState = rememberSaveable(saver = PaneState.saver(defaultPath)) {
        PaneState.initial(defaultPath)
    }
    var activePane by remember { mutableStateOf(leftPaneState) }
    var viewer by remember { mutableStateOf<Viewer?>(null) }
    
    // Use rememberSaveable to persist pane mode across configuration changes
    var paneMode by rememberSaveable { 
        mutableStateOf(if (isTablet && !singlePane) PaneMode.DUAL else PaneMode.SINGLE) 
    }
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showCleanerDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
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

    when (val currentViewer = viewer) {
        is Viewer.Image -> {
            val next = currentViewer.items.getOrNull(currentViewer.index + 1)
            val previous = currentViewer.items.getOrNull(currentViewer.index - 1)
            ImageViewerScreen(
                file = currentViewer.file,
                onClose = { viewer = null },
                onNext = next?.let {
                    { viewer = currentViewer.copy(file = it, index = currentViewer.index + 1) }
                },
                onPrevious = previous?.let {
                    { viewer = currentViewer.copy(file = it, index = currentViewer.index - 1) }
                }
            )
        }
        is Viewer.Pdf -> PdfViewerScreen(currentViewer.file) { viewer = null }
        is Viewer.Text -> TextViewerScreen(
            file = currentViewer.file,
            onClose = { viewer = null },
            showLineNumbers = currentViewer.showLineNumbers
        )
        is Viewer.Code -> CodeViewerScreen(
            file = currentViewer.file,
            language = currentViewer.language,
            onClose = { viewer = null }
        )
        is Viewer.Zip -> ZipViewerScreen(currentViewer.file) { viewer = null }
        null -> {
            Scaffold(
                topBar = {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 0.dp,
                        enableBlur = false
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            ),
                            title = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Xplorer", style = MaterialTheme.typography.titleMedium)
                                    BreadcrumbBar(
                                        currentPath = activePane.path,
                                        onNavigateToPath = { newPath ->
                                            activePane.navigateToPath(newPath)
                                        }
                                    )
                                }
                            },
                            navigationIcon = {
                                Row {
                                    IconButton(
                                        onClick = { activePane.goBack() },
                                        enabled = activePane.canGoBack()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                    IconButton(
                                        onClick = { activePane.goForward() },
                                        enabled = activePane.canGoForward()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = "Forward"
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconToggleButton(
                                    checked = paneMode == PaneMode.DUAL,
                                    onCheckedChange = { checked ->
                                        paneMode = if (checked) PaneMode.DUAL else PaneMode.SINGLE
                                        if (!checked) {
                                            activePane = leftPaneState
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (paneMode == PaneMode.DUAL)
                                            Icons.Outlined.ViewWeek else Icons.Outlined.ViewAgenda,
                                        contentDescription = "Toggle view mode"
                                    )
                                }

                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(Icons.Outlined.Sort, contentDescription = "Sort")
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

                                IconButton(onClick = { showCleanerDialog = true }) {
                                    Icon(Icons.Outlined.CleaningServices, contentDescription = "Cleaner")
                                }

                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Settings") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                // Dynamic gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundGradient())
                ) {
                    // Animated content transition for folder navigation
                    AnimatedContent(
                        targetState = activePane.path,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "folderNavigation"
                    ) { _ ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            // Left pane (always visible)
                            FileListPane(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                paneState = leftPaneState,
                                fileOperator = fileOperator,
                                safPermissionManager = safManager,
                                settings = settings,
                                searchQuery = searchQuery,
                                searchResult = searchResult,
                                permissionRefresh = permissionRefresh,
                                onSearchQueryChange = onSearchQueryChange,
                                onSafRequired = { onRequestSafAccess(FsNode.Local(it)) },
                                onRequestSafAccess = onRequestSafAccess,
                                onRequestFocus = { activePane = leftPaneState },
                                isActive = activePane == leftPaneState,
                                sortType = sortType,
                                sortOrder = sortOrder,
                                showDivider = paneMode == PaneMode.DUAL,
                                onOpenViewer = { viewer = it }
                            )
                            
                            // Right pane (only in dual mode)
                            if (paneMode == PaneMode.DUAL) {
                                FileListPane(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    paneState = rightPaneState,
                                    fileOperator = fileOperator,
                                    safPermissionManager = safManager,
                                    settings = settings,
                                    searchQuery = searchQuery,
                                    searchResult = searchResult,
                                    permissionRefresh = permissionRefresh,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onSafRequired = { onRequestSafAccess(FsNode.Local(it)) },
                                    onRequestSafAccess = onRequestSafAccess,
                                    onRequestFocus = { activePane = rightPaneState },
                                    isActive = activePane == rightPaneState,
                                    sortType = sortType,
                                    sortOrder = sortOrder,
                                    showDivider = false,
                                    onOpenViewer = { viewer = it }
                                )
                            }
                        }
                    }
                }
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
