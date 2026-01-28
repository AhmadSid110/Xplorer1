package com.droidexplorer.websim.settings

data class SettingsState(
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val searchIncludeSaf: Boolean = false,
    val torBoxEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val showBottomNav: Boolean = false
)

enum class ViewMode {
    LIST,
    GRID,
    DETAILS
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    CYBER
}
