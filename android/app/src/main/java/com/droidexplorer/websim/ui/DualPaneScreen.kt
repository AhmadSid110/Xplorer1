package com.droidexplorer.websim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.droidexplorer.websim.ui.viewer.*
import com.droidexplorer.websim.ui.glass.GlassSurface
import com.droidexplorer.websim.ui.theme.backgroundGradient

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
    onRequestSafAccess: (FsNode) -> Unit,
    torBoxClient: com.droidexplorer.websim.torbox.TorBoxClient? = null
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

    var paneMode by rememberSaveable {
        mutableStateOf(if (isTablet && !singlePane) PaneMode.DUAL else PaneMode.SINGLE)
    }

    var sortType by rememberSaveable { mutableStateOf(SortType.NAME) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.ASC) }

    when (val v = viewer) {

        /* ✅ IMAGE VIEWER — CORRECT SIGNATURE */
        is Viewer.Image -> {
            val next = v.items.getOrNull(v.index + 1)
            val previous = v.items.getOrNull(v.index - 1)

            ImageViewerScreen(
                file = v.file,
                onClose = { viewer = null },
                onNext = next?.let {
                    { viewer = v.copy(file = it, index = v.index + 1) }
                },
                onPrevious = previous?.let {
                    { viewer = v.copy(file = it, index = v.index - 1) }
                }
            )
        }

        is Viewer.Pdf -> PdfViewerScreen(v.file) { viewer = null }
        is Viewer.Text -> TextViewerScreen(v.file, onClose = { viewer = null })
        is Viewer.Code -> CodeViewerScreen(v.file, v.language) { viewer = null }
        is Viewer.Zip -> ZipViewerScreen(v.file) { viewer = null }

        null -> {
            Scaffold(
                topBar = {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 0.dp
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            ),
                            title = {
                                Column {
                                    Text("Xplorer")
                                    BreadcrumbBar(
                                        currentPath = activePane.path,
                                        onNavigateToPath = { activePane.navigateToPath(it) }
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
                                            Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                    IconButton(
                                        onClick = { activePane.goForward() },
                                        enabled = activePane.canGoForward()
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = "Forward"
                                        )
                                    }
                                    if (settings.torBoxEnabled) {
                                        IconButton(
                                            onClick = { activePane.navigateToPath("torbox:") }
                                        ) {
                                            Icon(
                                                Icons.Filled.Cloud,
                                                contentDescription = "TorBox",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(backgroundGradient())
                        .padding(padding)
                ) {
                    Row(Modifier.fillMaxSize()) {

                        /* LEFT PANE */
                        FileListPane(
                            modifier = Modifier.weight(1f),
                            paneState = leftPaneState,
                            currentPath = leftPaneState.path,
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
                            onOpenViewer = { viewer = it },
                            torBoxClient = torBoxClient
                        )

                        /* RIGHT PANE */
                        if (paneMode == PaneMode.DUAL) {
                            FileListPane(
                                modifier = Modifier.weight(1f),
                                paneState = rightPaneState,
                                currentPath = rightPaneState.path,
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
                                onOpenViewer = { viewer = it },
                                torBoxClient = torBoxClient
                            )
                        }
                    }
                }
            }
        }
    }
}