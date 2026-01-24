package com.droidexplorer.websim.storage

import android.content.Context
import android.content.SharedPreferences
import com.droidexplorer.websim.torbox.TorBoxFilter
import com.droidexplorer.websim.torbox.TorBoxSortMode

class TorBoxUiStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortMode(): TorBoxSortMode {
        val raw = prefs.getString(KEY_SORT_MODE, TorBoxSortMode.NAME.name)
        return raw?.let { runCatching { TorBoxSortMode.valueOf(it) }.getOrNull() }
            ?: TorBoxSortMode.NAME
    }

    fun setSortMode(mode: TorBoxSortMode) {
        prefs.edit().putString(KEY_SORT_MODE, mode.name).apply()
    }

    fun isSortAscending(): Boolean {
        return prefs.getBoolean(KEY_SORT_ASC, true)
    }

    fun setSortAscending(asc: Boolean) {
        prefs.edit().putBoolean(KEY_SORT_ASC, asc).apply()
    }

    fun getFilter(): TorBoxFilter {
        val raw = prefs.getString(KEY_FILTER, TorBoxFilter.ALL.name)
        return raw?.let { runCatching { TorBoxFilter.valueOf(it) }.getOrNull() }
            ?: TorBoxFilter.ALL
    }

    fun setFilter(filter: TorBoxFilter) {
        prefs.edit().putString(KEY_FILTER, filter.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "torbox_ui"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_SORT_ASC = "sort_asc"
        private const val KEY_FILTER = "filter"
    }
}
