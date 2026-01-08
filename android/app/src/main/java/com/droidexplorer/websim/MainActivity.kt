@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidexplorer.websim

import kotlinx.coroutines.launch
import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsScreen
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.ui.DualPaneScreen
import com.droidexplorer.websim.ui.ExplorerViewModel
import com.droidexplorer.websim.ui.ExplorerViewModelFactory
import com.droidexplorer.websim.ui.events.UiEvent
import com.droidexplorer.websim.ui.settings.StorageScreen
import com.droidexplorer.websim.ui.theme.XplorerTheme
import com.droidexplorer.websim.search.FileSearcher
import com.droidexplorer.websim.search.SearchEngine
import com.droidexplorer.websim.storage.DataStoreSafStore
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.storage.StorageInfoProvider

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val safStore by lazy { DataStoreSafStore(applicationContext) }
    private val safPermissionManager by lazy { SafPermissionManager(applicationContext, safStore) }
    private val searchEngine by lazy { SearchEngine(FileSearcher(applicationContext)) }
    private val viewModel: ExplorerViewModel by viewModels {
        ExplorerViewModelFactory(settingsRepository, safPermissionManager, searchEngine)
    }

    private var hasStoragePermission by mutableStateOf(false)
    private var showPermissionRationale by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.all { it.value }
        if (!hasStoragePermission) {
            showPermissionRationale = true
        }
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
                        is UiEvent.RequestSafAccess -> {
                            safLauncher.launch(event.initialUri)
                        }
                        is UiEvent.RequestAllFilesAccess -> {
                            requestAllFilesAccess()
                        }
                    }
                }
            }
        }
        
        setContent {
            val settingsState by viewModel.settings.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val searchResult by viewModel.searchResults.collectAsState()
            val permissionRefresh by viewModel.permissionRefresh.collectAsState()
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showStorage by rememberSaveable { mutableStateOf(false) }
            val storageInfoProvider = remember { StorageInfoProvider() }
            val storageInfo = remember(showStorage) {
                storageInfoProvider.internalStorage()
            }

            XplorerTheme {
                if (hasStoragePermission) {
                    if (showSettings) {
                        BackHandler {
                            if (showStorage) {
                                showStorage = false
                            } else {
                                showSettings = false
                            }
                        }
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(if (showStorage) "Storage" else "Settings") },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            if (showStorage) {
                                                showStorage = false
                                            } else {
                                                showSettings = false
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { paddingValues ->
                            if (showStorage) {
                                Box(modifier = Modifier.padding(paddingValues)) {
                                    StorageScreen(info = storageInfo)
                                }
                            } else {
                                SettingsScreen(
                                    state = settingsState,
                                    onViewModeChange = viewModel::setViewMode,
                                    onToggleHidden = viewModel::setShowHidden,
                                    onToggleSafSearch = viewModel::onToggleSafSearch,
                                    onRequestAllFilesAccess = viewModel::requestAllFilesAccess,
                                    onOpenStorage = { showStorage = true },
                                    modifier = Modifier.padding(paddingValues)
                                )
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
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 - READ_EXTERNAL_STORAGE only
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 10 and below
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
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (showRationale) {
                    "Xplorer needs storage access to browse and manage your files. Please grant permission in settings."
                } else {
                    "Xplorer needs permission to access your files and folders."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
