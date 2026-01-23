package com.droidexplorer.websim.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidexplorer.websim.file.*
import com.droidexplorer.websim.search.SearchResult
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.storage.DataStoreSafStore
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.ui.viewer.*
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.theme.backgroundGradient
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.effects.ScanlineOverlay
import com.droidexplorer.websim.ui.theme.cyberGlow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualPaneScreen(
    singlePane: Boolean = false,
    settings: SettingsState,
    searchQuery: String,
    searchResult: SearchResult?,
    permissionRefresh: Int,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCleaner: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRequestSafAccess: (FsNode) -> Unit,
    onViewModeChange: (com.droidexplorer.websim.settings.ViewMode) -> Unit,
    torBoxClient: com.droidexplorer.websim.torbox.TorBoxClient? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
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
    var viewMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val accent = LocalCyberAccent.current
    val haptics = LocalHapticFeedback.current
    var selectedNode by remember { mutableStateOf<FsNode?>(null) }
    var clearSelectionSignal by remember { mutableStateOf(0) }
    var renameSelectionSignal by remember { mutableStateOf(0) }
    var deleteSelectionSignal by remember { mutableStateOf(0) }

    val drawerBlur by animateDpAsState(
        targetValue = if (drawerState.isOpen) 6.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "drawerBlur"
    )

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /* ───────────────────── VIEWERS ───────────────────── */

    when (val v = viewer) {

        is Viewer.Image -> {
            ImageViewerScreen(
                items = v.items,
                index = v.index,
                onClose = { viewer = null }
            )
        }

        is Viewer.Pdf ->
            PdfViewerScreen(v.file) { viewer = null }

        is Viewer.Text ->
            TextViewerScreen(v.file, onClose = { viewer = null })

        is Viewer.Code ->
            CodeViewerScreen(v.file, v.language, onClose = { viewer = null })

        is Viewer.Zip ->
            ZipViewerScreen(v.file) { viewer = null }

        null -> {

            /* ───────────────────── MAIN UI ───────────────────── */

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    SideDrawerContent(
                        activeDestination = DrawerDestination.FILES,
                        onFiles = {
                            drawerScope.launch { drawerState.close() }
                            activePane.navigateToPath(defaultPath)
                        },
                        onCleaner = {
                            drawerScope.launch { drawerState.close() }
                            onOpenCleaner()
                        },
                        onTorBox = {
                            drawerScope.launch { drawerState.close() }
                            activePane.navigateToPath("torbox:")
                        },
                        onPermissions = {
                            drawerScope.launch { drawerState.close() }
                            onRequestAllFilesAccess()
                        },
                        onSettings = {
                            drawerScope.launch { drawerState.close() }
                            onOpenSettings()
                        }
                    )
                },
                gesturesEnabled = true
            ) {
                Scaffold(
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                title = {
                                    Column {
                                        Text("XPLORER", letterSpacing = 0.5.sp)
                                        BreadcrumbBar(
                                            currentPath = activePane.path,
                                            onNavigateToPath = { activePane.navigateToPath(it) }
                                        )
                                        if (selectedNode != null) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(top = 6.dp)
                                                    .border(
                                                        1.dp,
                                                        accent.copy(alpha = 0.4f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .cyberGlow(accent, intensity = 0.25f)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Selected: ${selectedNode?.name}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Clear",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accent,
                                                    modifier = Modifier.clickable {
                                                        clearSelectionSignal++
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                navigationIcon = {
                                    Row {
                                        IconButton(
                                            onClick = { drawerScope.launch { drawerState.open() } }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Menu,
                                                contentDescription = "Open menu",
                                                tint = accent
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    Box {
                                        IconButton(onClick = { viewMenuExpanded = true }) {
                                            Icon(
                                                Icons.Filled.MoreVert,
                                                contentDescription = "More",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = viewMenuExpanded,
                                            onDismissRequest = { viewMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Sort") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Sort,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    sortMenuExpanded = true
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Home") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Home,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    activePane.navigateToPath(defaultPath)
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Back") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.AutoMirrored.Outlined.ArrowBack,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                enabled = activePane.canGoBack(),
                                                onClick = {
                                                    activePane.goBack()
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Forward") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.AutoMirrored.Outlined.ArrowForward,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                enabled = activePane.canGoForward(),
                                                onClick = {
                                                    activePane.goForward()
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("List view") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.List,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    onViewModeChange(com.droidexplorer.websim.settings.ViewMode.LIST)
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Grid view") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.GridView,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    onViewModeChange(com.droidexplorer.websim.settings.ViewMode.GRID)
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Details view") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.ViewList,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    onViewModeChange(com.droidexplorer.websim.settings.ViewMode.DETAILS)
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (paneMode == PaneMode.DUAL) "Single pane" else "Split pane") },
                                                leadingIcon = {
                                                    Icon(
                                                        if (paneMode == PaneMode.DUAL) Icons.Outlined.ViewAgenda else Icons.Outlined.ViewWeek,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    paneMode =
                                                        if (paneMode == PaneMode.DUAL) PaneMode.SINGLE else PaneMode.DUAL
                                                    if (paneMode == PaneMode.SINGLE) activePane = leftPaneState
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Cleaner") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.CleaningServices,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    onOpenCleaner()
                                                    viewMenuExpanded = false
                                                }
                                            )
                                            if (selectedNode != null) {
                                                DropdownMenuItem(
                                                    text = { Text("Rename selected") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Edit,
                                                            contentDescription = null,
                                                            tint = accent
                                                        )
                                                    },
                                                    onClick = {
                                                        renameSelectionSignal++
                                                        viewMenuExpanded = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete selected") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Delete,
                                                            contentDescription = null,
                                                            tint = accent
                                                        )
                                                    },
                                                    onClick = {
                                                        deleteSelectionSignal++
                                                        viewMenuExpanded = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Clear selection") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Close,
                                                            contentDescription = null,
                                                            tint = accent
                                                        )
                                                    },
                                                    onClick = {
                                                        clearSelectionSignal++
                                                        viewMenuExpanded = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Settings") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Settings,
                                                        contentDescription = null,
                                                        tint = accent
                                                    )
                                                },
                                                onClick = {
                                                    onOpenSettings()
                                                    viewMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                    SortMenu(
                                        currentSortType = sortType,
                                        currentSortOrder = sortOrder,
                                        onSortChange = { newType, newOrder ->
                                            sortType = newType
                                            sortOrder = newOrder
                                        },
                                        expanded = sortMenuExpanded,
                                        onDismiss = { sortMenuExpanded = false }
                                    )
                                }
                            )
                            Divider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(backgroundGradient())
                            .blur(drawerBlur)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
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
                                onSelectionChange = { selectedNode = it },
                                clearSelectionSignal = clearSelectionSignal,
                                renameSelectionSignal = renameSelectionSignal,
                                deleteSelectionSignal = deleteSelectionSignal,
                                onRequestFocus = { activePane = leftPaneState },
                                isActive = activePane == leftPaneState,
                                sortType = sortType,
                                sortOrder = sortOrder,
                                showDivider = paneMode == PaneMode.DUAL,
                                onOpenViewer = { if (viewer == null) viewer = it },
                                torBoxClient = torBoxClient
                            )

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
                                    onSelectionChange = { selectedNode = it },
                                    clearSelectionSignal = clearSelectionSignal,
                                    renameSelectionSignal = renameSelectionSignal,
                                    deleteSelectionSignal = deleteSelectionSignal,
                                    onRequestFocus = { activePane = rightPaneState },
                                    isActive = activePane == rightPaneState,
                                    sortType = sortType,
                                    sortOrder = sortOrder,
                                    showDivider = false,
                                    onOpenViewer = { if (viewer == null) viewer = it },
                                    torBoxClient = torBoxClient
                                )
                            }
                        }
                        ScanlineOverlay()
                    }
                }
            }
        }
    }
}