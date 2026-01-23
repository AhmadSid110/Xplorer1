package com.droidexplorer.websim.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(
    private val context: Context
) {
    private val VIEW_MODE = stringPreferencesKey("view_mode")
    private val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
    private val SEARCH_SAF = booleanPreferencesKey("search_saf")
    private val TORBOX_ENABLED = booleanPreferencesKey("torbox_enabled")
    private val THEME_MODE = stringPreferencesKey("theme_mode")

    val settings: Flow<SettingsState> =
        context.dataStore.data.map { prefs ->
            val storedViewMode = prefs[VIEW_MODE]
            val viewMode = ViewMode.values().firstOrNull { it.name == storedViewMode } ?: ViewMode.LIST
            val storedThemeMode = prefs[THEME_MODE]
            val themeMode = ThemeMode.values().firstOrNull { it.name == storedThemeMode } ?: ThemeMode.LIGHT
            SettingsState(
                defaultViewMode = viewMode,
                showHiddenFiles = prefs[SHOW_HIDDEN] ?: false,
                searchIncludeSaf = prefs[SEARCH_SAF] ?: false,
                torBoxEnabled = prefs[TORBOX_ENABLED] ?: false,
                themeMode = themeMode
            )
        }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit {
            it[VIEW_MODE] = mode.name
        }
    }

    suspend fun setShowHidden(enabled: Boolean) {
        context.dataStore.edit {
            it[SHOW_HIDDEN] = enabled
        }
    }

    suspend fun setSearchSaf(enabled: Boolean) {
        context.dataStore.edit {
            it[SEARCH_SAF] = enabled
        }
    }

    suspend fun setTorBoxEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[TORBOX_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
            it[THEME_MODE] = mode.name
        }
    }
}
