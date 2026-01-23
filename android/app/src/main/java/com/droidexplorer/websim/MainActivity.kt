@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidexplorer.websim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.droidexplorer.websim.search.FileSearcher
import com.droidexplorer.websim.search.SearchEngine
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsScreen
import com.droidexplorer.websim.storage.*
import com.droidexplorer.websim.torbox.TorBoxClient
import com.droidexplorer.websim.ui.*
import com.droidexplorer.websim.ui.dialogs.TorBoxSetupDialog
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.events.UiEvent
import com.droidexplorer.websim.ui.settings.CleanerScreen
import com.droidexplorer.websim.ui.settings.StorageScreen
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.XplorerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val safStore by lazy { DataStoreSafStore(applicationContext) }
    private val safPermissionManager by lazy { SafPermissionManager(applicationContext, safStore) }
    private val searchEngine by lazy { SearchEngine(FileSearcher(applicationContext)) }
    private val torBoxStore by lazy { TorBoxStore(applicationContext) }

    private val viewModel: ExplorerViewModel by viewModels {
        ExplorerViewModelFactory(
            settingsRepository,
            safPermissionManager,
            searchEngine,
            contentResolver,
            torBoxStore
        )
    }

    // Activity-owned UI state
    private val showTorBoxSetupFlow = MutableStateFlow(false)

    private var hasStoragePermission by mutableStateOf(false)
    private var showPermissionRationale by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasStoragePermission = permissions.all { it.value }
            if (!hasStoragePermission) showPermissionRationale = true
        }

    private val safLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.onSafPermissionGranted(uri)
            } else {
                viewModel.onSafPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        // Lifecycle-safe UI events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.RequestSafAccess ->
                            safLauncher.launch(event.initialUri)

                        is UiEvent.RequestAllFilesAccess ->
                            requestAllFilesAccess()

                        is UiEvent.ShowTorBoxSetup ->
                            showTorBoxSetupFlow.value = true
                    }
                }
            }
        }

        setContent {
            val settingsState by viewModel.settings.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val searchResult by viewModel.searchResults.collectAsState()
            val permissionRefresh by viewModel.permissionRefresh.collectAsState()
            val storageCategoryData by viewModel.storageCategoryData.collectAsState()
            val showTorBoxSetup by showTorBoxSetupFlow.collectAsState()
            val context = LocalContext.current

            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showStorage by rememberSaveable { mutableStateOf(false) }
            var showCleaner by rememberSaveable { mutableStateOf(false) }

            val storageInfoProvider = remember { StorageInfoProvider() }
            val storageInfo = remember(showStorage) {
                storageInfoProvider.internalStorage()
            }

            // Storage analyzer (Play Store safe)
            LaunchedEffect(showStorage) {
                if (showStorage) {
                    val analyzer = MediaStoreStorageAnalyzer(contentResolver)
                    val data = analyzer.analyze()
                    viewModel.updateStorageData(
                        com.droidexplorer.websim.ui.settings.StorageCategoryData(
                            images = data.images,
                            videos = data.videos,
                            audio = data.audio,
                            apks = data.apks,
                            archives = data.archives
                        )
                    )
                }
            }

            /**
             * ✅ CRITICAL FIX
             * TorBoxClient MUST be recreated when TorBox is enabled
             */
            val torBoxClient: TorBoxClient? =
                remember(settingsState.torBoxEnabled) {
                    if (settingsState.torBoxEnabled) {
                        torBoxStore.getApiKey()?.let { TorBoxClient(it) }
                    } else null
                }

            val useDarkTheme = when (settingsState.themeMode) {
                com.droidexplorer.websim.settings.ThemeMode.DARK -> true
                com.droidexplorer.websim.settings.ThemeMode.LIGHT -> false
                com.droidexplorer.websim.settings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            XplorerTheme(darkTheme = useDarkTheme) {
                if (hasStoragePermission) {
                    if (showSettings) {

                        BackHandler {
                            when {
                                showCleaner -> showCleaner = false
                                showStorage -> showStorage = false
                                else -> showSettings = false
                            }
                        }

                        Scaffold(
                            topBar = {
                                val accent = LocalCyberAccent.current
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
                                            Text(
                                                when {
                                                    showCleaner -> "Cleaner"
                                                    showStorage -> "Storage"
                                                    else -> "Settings"
                                                }
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = {
                                                when {
                                                    showCleaner -> showCleaner = false
                                                    showStorage -> showStorage = false
                                                    else -> showSettings = false
                                                }
                                            }) {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = accent
                                                )
                                            }
                                        }
                                    )
                                    Divider(
                                        color = accent.copy(alpha = 0.3f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        ) { padding ->
                            AnimatedContent(
                                targetState = when {
                                    showCleaner -> "cleaner"
                                    showStorage -> "storage"
                                    else -> "settings"
                                },
                                transitionSpec = {
                                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                                },
                                label = "settings-pages"
                            ) { destination ->
                                when (destination) {
                                    "storage" ->
                                        StorageScreen(
                                            info = storageInfo,
                                            categoryData = storageCategoryData,
                                            modifier = Modifier.padding(padding)
                                        )

                                    "cleaner" ->
                                        CleanerScreen(
                                            data = storageCategoryData ?: com.droidexplorer.websim.ui.settings.StorageCategoryData(),
                                            onClose = { showCleaner = false },
                                            onOpenCategory = { },
                                            modifier = Modifier.padding(padding)
                                        )

                                    else ->
                                        SettingsScreen(
                                            state = settingsState,
                                            onViewModeChange = viewModel::setViewMode,
                                            onThemeModeChange = viewModel::setThemeMode,
                                            onToggleHidden = viewModel::setShowHidden,
                                            onToggleSafSearch = viewModel::onToggleSafSearch,
                                            onToggleTorBox = viewModel::setTorBoxEnabled,
                                            onRequestAllFilesAccess = viewModel::requestAllFilesAccess,
                                            onOpenStorage = { showStorage = true },
                                            onOpenCleaner = { showCleaner = true },
                                            modifier = Modifier.padding(padding)
                                        )
                                }
                            }
                        }
                    } else {
                        DualPaneScreen(
                            singlePane = false,
                            settings = settingsState,
                            searchQuery = searchQuery,
                            searchResult = searchResult,
                            permissionRefresh = permissionRefresh,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onOpenSettings = { showSettings = true },
                            onOpenCleaner = {
                                showSettings = true
                                showCleaner = true
                            },
                            onRequestAllFilesAccess = viewModel::requestAllFilesAccess,
                            onRequestSafAccess = viewModel::requestSafAccessFor,
                            onViewModeChange = viewModel::setViewMode,
                            torBoxClient = torBoxClient
                        )
                    }
                } else {
                    PermissionScreen(
                        showRationale = showPermissionRationale,
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }

                if (showTorBoxSetup) {
                    TorBoxSetupDialog(
                        onSave = { apiKey ->
                            showTorBoxSetupFlow.value = false
                            viewModel.onTorBoxSetupSave(apiKey)
                        },
                        onCancel = {
                            showTorBoxSetupFlow.value = false
                            viewModel.onTorBoxSetupCancel()
                        }
                    )
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAllFilesAccessChanged()
    }

    private fun checkAndRequestPermissions() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

        hasStoragePermission = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasStoragePermission) {
            permissionLauncher.launch(permissions)
        }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }
}