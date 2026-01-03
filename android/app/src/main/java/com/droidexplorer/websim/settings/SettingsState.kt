package com.droidexplorer.websim.settings

data class SettingsState(
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val searchIncludeSaf: Boolean = false
)

enum class ViewMode {
    LIST,
    GRID,
    DETAILS
}
