package com.droidexplorer.websim.storage

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.safDataStore by preferencesDataStore("saf_permissions")

class DataStoreSafStore(private val context: Context) : SafStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefsState = context.safDataStore.data.stateIn(
        scope,
        SharingStarted.Eagerly,
        emptyPreferences()
    )

    override fun get(path: String): Uri? =
        prefsState.value[stringPreferencesKey(pathKey(path))]?.let { Uri.parse(it) }

    override fun put(path: String, uri: String) {
        scope.launch {
            context.safDataStore.edit { prefs ->
                prefs[stringPreferencesKey(pathKey(path))] = uri
            }
        }
    }

    private fun pathKey(path: String): String =
        "uri_${Base64.encodeToString(path.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)}"
}
