package com.droidexplorer.websim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.droidexplorer.websim.search.SearchEngine
import com.droidexplorer.websim.search.SearchResult
import com.droidexplorer.websim.search.SearchRoot
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.settings.ViewMode
import com.droidexplorer.websim.storage.SafPermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _requestSafPermission = MutableSharedFlow<Unit>()
    val requestSafPermission: SharedFlow<Unit> = _requestSafPermission.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
                _requestSafPermission.emit(Unit)
            } else {
                updateSearchSaf(enabled)
            }
        }
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
