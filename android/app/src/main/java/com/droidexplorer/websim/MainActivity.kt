@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidexplorer.websim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.droidexplorer.websim.search.FileSearcher
import com.droidexplorer.websim.search.SearchEngine
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsScreen
import com.droidexplorer.websim.storage.DataStoreSafStore
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.storage.StorageInfoProvider
import com.droidexplorer.websim.storage.TorBoxStore
import com.droidexplorer.websim.ui.DualPaneScreen
import com.droidexplorer.websim.ui.ExplorerViewModel
import com.droidexplorer.websim.ui.ExplorerViewModelFactory
import com.droidexplorer.websim.ui.dialogs.TorBoxSetupDialog
import com.droidexplorer.websim.ui.events.UiEvent
import com.droidexplorer.websim.ui.settings.StorageScreen
import com.droidexplorer.websim.ui.theme.XplorerTheme
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.RequestSafAccess ->
                            safLauncher.launch(event.initialUri)

                        is UiEvent.RequestAllFilesAccess ->
                            requestAllFilesAccess()

                        is UiEvent.ShowTorBoxSetup ->
                            showTorBoxSetup = true
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

            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showStorage by rememberSaveable { mutableStateOf(false) }
            var showTorBoxSetup by rememberSaveable { mutableStateOf(false) }

            val storageInfoProvider = remember { StorageInfoProvider() }
            val storageInfo = remember(showStorage) {
                storageInfoProvider.internalStorage()
            }

            // Analyze storage categories when storage screen is shown
            LaunchedEffect(showStorage) {
                if (showStorage) {
                    val analyzer = com.droidexplorer.websim.storage.MediaStoreStorageAnalyzer(contentResolver)
                    val analyzerData = analyzer.analyze()
                    // Convert to UI data class and update ViewModel
                    val categoryData = com.droidexplorer.websim.ui.settings.StorageCategoryData(
                        images = analyzerData.images,
                        videos = analyzerData.videos,
                        audio = analyzerData.audio,
                        apks = analyzerData.apks,
                        archives = analyzerData.archives
                    )
                    viewModel.updateStorageData(categoryData)
                }
            }

            XplorerTheme {
                if (hasStoragePermission) {
                    if (showSettings) {

                        BackHandler {
                            if (showStorage) showStorage = false
                            else showSettings = false
                        }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(if (showStorage) "Storage" else "Settings") },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            if (showStorage) showStorage = false
                                            else showSettings = false
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        ) { padding ->

                            AnimatedContent(
                                targetState = showStorage,
                                transitionSpec = {
                                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                                },
                                label = "settings-storage"
                            ) { isStorage ->

                                if (isStorage) {
                                    StorageScreen(
                                        info = storageInfo,
                                        categoryData = storageCategoryData,
                                        modifier = Modifier.padding(padding)
                                    )
                                } else {
                                    SettingsScreen(
                                        state = settingsState,
                                        onViewModeChange = viewModel::setViewMode,
                                        onToggleHidden = viewModel::setShowHidden,
                                        onToggleSafSearch = viewModel::onToggleSafSearch,
                                        onToggleTorBox = viewModel::setTorBoxEnabled,
                                        onRequestAllFilesAccess = viewModel::requestAllFilesAccess,
                                        onOpenStorage = { showStorage = true },
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
                            onRequestSafAccess = viewModel::requestSafAccessFor
                        )
                    }
                } else {
                    PermissionScreen(
                        showRationale = showPermissionRationale,
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }
                
                // TorBox Setup Dialog
                if (showTorBoxSetup) {
                    TorBoxSetupDialog(
                        onSave = { apiKey ->
                            showTorBoxSetup = false
                            viewModel.onTorBoxSetupSave(apiKey)
                        },
                        onCancel = {
                            showTorBoxSetup = false
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

@Composable
fun PermissionScreen(
    showRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text =
                    if (showRationale)
                        "Xplorer needs storage access to browse and manage your files."
                    else
                        "Grant storage permission to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
