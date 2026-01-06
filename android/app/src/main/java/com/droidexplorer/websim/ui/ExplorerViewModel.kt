package com.droidexplorer.websim.ui

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
import com.droidexplorer.websim.ui.events.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ExplorerViewModel(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager,
    private val searchEngine: SearchEngine
) : ViewModel() {

    val settings: StateFlow<SettingsState> =
        settingsRepository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsState()
        )

    private val _uiEvents = Channel<UiEvent>(capacity = 64)
    val uiEvents = _uiEvents.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _permissionRefresh = MutableStateFlow(0)
    val permissionRefresh: StateFlow<Int> = _permissionRefresh.asStateFlow()

    private var pendingPermissionPath: String? = null
    private var pendingSearchEnable: Boolean = false

    private val debouncedQuery = searchQuery
        .debounce(350)
        .distinctUntilChanged()

    val searchResults: StateFlow<SearchResult?> =
        combine(
            debouncedQuery,
            settings
        ) { query, settings ->
            query to buildSearchRoots(settings)
        }.flatMapLatest { (query, roots) ->
            if (query.isBlank()) {
                flowOf(null)
            } else {
                searchEngine.search(query, roots)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    fun onToggleSafSearch(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !safPermissionManager.hasAnyPermission()) {
                pendingSearchEnable = true
                _uiEvents.send(UiEvent.RequestSafAccess(initialUri = null))
            } else {
                updateSearchSaf(enabled)
            }
        }
    }

    fun requestSafAccessFor(folder: FsNode) {
        viewModelScope.launch {
            val targetPath = folder.path
            if (!folder.isDirectory) return@launch
            if (safPermissionManager.isPersisted(File(targetPath))) return@launch
            pendingPermissionPath = targetPath
            val initialUri = when (folder) {
                is FsNode.Saf -> folder.document.uri
                is FsNode.Local -> safPermissionManager.findStoredUri(folder.path)
            }
            _uiEvents.send(UiEvent.RequestSafAccess(initialUri = initialUri))
        }
    }

    fun onSafPermissionGranted(uri: Uri) {
        val targetPath = pendingPermissionPath
        if (targetPath != null) {
            safPermissionManager.persist(uri, targetPath)
        } else {
            safPermissionManager.persist(uri)
        }
        pendingPermissionPath = null
        if (pendingSearchEnable) {
            viewModelScope.launch { updateSearchSaf(true) }
            pendingSearchEnable = false
        }
        _permissionRefresh.value = _permissionRefresh.value + 1
    }

    fun onSafPermissionDenied() {
        pendingPermissionPath = null
        pendingSearchEnable = false
    }

    fun updateSearchQuery(text: String) {
        _searchQuery.value = text
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { settingsRepository.setViewMode(mode) }
    }

    fun setShowHidden(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHidden(enabled) }
    }

    fun setSearchSaf(enabled: Boolean) {
        viewModelScope.launch { updateSearchSaf(enabled) }
    }

    private suspend fun updateSearchSaf(enabled: Boolean) {
        settingsRepository.setSearchSaf(enabled)
    }

    private fun buildSearchRoots(settings: SettingsState): List<SearchRoot> {
        val roots = mutableListOf<SearchRoot>()
        roots += SearchRoot.Local("/storage/emulated/0")
        if (settings.searchIncludeSaf) {
            safPermissionManager.getPersistedRootIds().forEach { id ->
                roots += SearchRoot.Saf(id)
            }
        }
        return roots
    }
}

class ExplorerViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager,
    private val searchEngine: SearchEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExplorerViewModel(settingsRepository, safPermissionManager, searchEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
