package com.droidexplorer.websim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.droidexplorer.websim.settings.SettingsRepository
import com.droidexplorer.websim.settings.SettingsState
import com.droidexplorer.websim.settings.ViewMode
import com.droidexplorer.websim.storage.SafPermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExplorerViewModel(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager
) : ViewModel() {

    val settings: StateFlow<SettingsState> =
        settingsRepository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsState()
        )

    private val _requestSafPermission = MutableSharedFlow<Unit>()
    val requestSafPermission: SharedFlow<Unit> = _requestSafPermission.asSharedFlow()

    fun onToggleSafSearch(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !safPermissionManager.hasAnyPermission()) {
                _requestSafPermission.emit(Unit)
            } else {
                updateSearchSaf(enabled)
            }
        }
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
}

class ExplorerViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val safPermissionManager: SafPermissionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExplorerViewModel(settingsRepository, safPermissionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
