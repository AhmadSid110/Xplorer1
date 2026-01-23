package com.droidexplorer.websim.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.search.SearchEngine
import com.droidexplorer.websim.search.SearchResult
import com.droidexplorer.websim.search.SearchRoot
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.settings.ViewMode
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.storage.TorBoxStore
import com.droidexplorer.websim.ui.events.UiEvent
import com.droidexplorer.websim.ui.settings.StorageCategoryData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ExplorerViewModel(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager,
    private val searchEngine: SearchEngine,
    private val contentResolver: ContentResolver,
    private val torBoxStore: TorBoxStore
) : ViewModel() {

    // ─────────────────────────────────────────────
    // Settings
    // ─────────────────────────────────────────────
    val settings: StateFlow<SettingsState> =
        settingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsState()
        )

    // ─────────────────────────────────────────────
    // UI events (one-shot)
    // ─────────────────────────────────────────────
    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    // ─────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val debouncedQuery =
        _searchQuery.debounce(350).distinctUntilChanged()

    val searchResults: StateFlow<SearchResult?> =
        combine(debouncedQuery, settings) { query, settings ->
            query to buildSearchRoots(settings)
        }.flatMapLatest { (query, roots) ->
            if (query.isBlank()) flowOf(null)
            else searchEngine.search(query, roots)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    // ─────────────────────────────────────────────
    // Permission refresh trigger
    // ─────────────────────────────────────────────
    private val _permissionRefresh = MutableStateFlow(0)
    val permissionRefresh: StateFlow<Int> = _permissionRefresh.asStateFlow()

    // ─────────────────────────────────────────────
    // Storage category data
    // ─────────────────────────────────────────────
    private val _storageCategoryData = MutableStateFlow<StorageCategoryData?>(null)
    val storageCategoryData: StateFlow<StorageCategoryData?> =
        _storageCategoryData.asStateFlow()

    fun updateStorageData(data: StorageCategoryData?) {
        _storageCategoryData.value = data
    }

    private var pendingPermissionPath: String? = null
    private var pendingSearchEnable = false

    // ─────────────────────────────────────────────
    // SAF logic
    // ─────────────────────────────────────────────
    fun onToggleSafSearch(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !safPermissionManager.hasAnyPermission()) {
                pendingSearchEnable = true
                _uiEvents.send(UiEvent.RequestSafAccess(initialUri = null))
            } else {
                settingsRepository.setSearchSaf(enabled)
            }
        }
    }

    /**
     * ❗ FIXED: TorBox explicitly handled
     */
    fun requestSafAccessFor(folder: FsNode) {
        when (folder) {

            is FsNode.Local,
            is FsNode.Saf -> {
                if (!folder.isDirectory) return

                viewModelScope.launch {
                    val path = folder.path
                    if (safPermissionManager.isPersisted(File(path))) return@launch

                    pendingPermissionPath = path

                    val initialUri = when (folder) {
                        is FsNode.Saf -> folder.document.uri
                        is FsNode.Local -> safPermissionManager.findStoredUri(path)
                        else -> null // unreachable
                    }

                    _uiEvents.send(UiEvent.RequestSafAccess(initialUri))
                }
            }

            is FsNode.TorBox -> {
                // 🚫 TorBox is remote + read-only
                // No SAF, no filesystem permissions
                return
            }
        }
    }

    fun onSafPermissionGranted(uri: Uri) {
        pendingPermissionPath?.let {
            safPermissionManager.persist(uri, it)
        } ?: safPermissionManager.persist(uri)

        pendingPermissionPath = null

        if (pendingSearchEnable) {
            viewModelScope.launch {
                settingsRepository.setSearchSaf(true)
            }
            pendingSearchEnable = false
        }

        _permissionRefresh.value++
    }

    fun onSafPermissionDenied() {
        pendingPermissionPath = null
        pendingSearchEnable = false
    }

    // ─────────────────────────────────────────────
    // ALL FILES ACCESS
    // ─────────────────────────────────────────────
    fun onAllFilesAccessChanged() {
        _permissionRefresh.value++
    }

    // ─────────────────────────────────────────────
    // Settings
    // ─────────────────────────────────────────────
    fun updateSearchQuery(text: String) {
        _searchQuery.value = text
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { settingsRepository.setViewMode(mode) }
    }

    fun setShowHidden(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHidden(enabled) }
    }

    fun setThemeMode(mode: com.droidexplorer.websim.settings.ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    // ─────────────────────────────────────────────
    // TorBox
    // ─────────────────────────────────────────────
    fun setTorBoxEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && torBoxStore.getApiKey() == null) {
                _uiEvents.send(UiEvent.ShowTorBoxSetup)
            } else {
                settingsRepository.setTorBoxEnabled(enabled)
            }
        }
    }

    fun onTorBoxSetupSave(apiKey: String) {
        torBoxStore.saveApiKey(apiKey)
        viewModelScope.launch {
            settingsRepository.setTorBoxEnabled(true)
        }
    }

    fun onTorBoxSetupCancel() {
        viewModelScope.launch {
            settingsRepository.setTorBoxEnabled(false)
        }
    }

    fun requestAllFilesAccess() {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.RequestAllFilesAccess)
        }
    }

    // ─────────────────────────────────────────────
    // Search roots
    // ─────────────────────────────────────────────
    private fun buildSearchRoots(settings: SettingsState): List<SearchRoot> {
        val roots = mutableListOf<SearchRoot>()
        roots += SearchRoot.Local("/storage/emulated/0")

        if (settings.searchIncludeSaf) {
            safPermissionManager
                .getPersistedRootIds()
                .forEach { roots += SearchRoot.Saf(it) }
        }
        return roots
    }
}

// ─────────────────────────────────────────────
// Factory
// ─────────────────────────────────────────────
class ExplorerViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager,
    private val searchEngine: SearchEngine,
    private val contentResolver: ContentResolver,
    private val torBoxStore: TorBoxStore
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExplorerViewModel(
                settingsRepository,
                safPermissionManager,
                searchEngine,
                contentResolver,
                torBoxStore
            ) as T
        }
        error("Unknown ViewModel class")
    }
}