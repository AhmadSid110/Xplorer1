package com.droidexplorer.websim.storage

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.safDataStore by preferencesDataStore("saf_permissions")

class DataStoreSafStore(private val context: Context) : SafStore {

    override fun get(path: String): Uri? = runBlocking {
        context.safDataStore.data.first()[stringPreferencesKey(pathKey(path))]?.let {
            Uri.parse(it)
        }
    }

    override fun put(path: String, uri: String) {
        runBlocking {
            context.safDataStore.edit { prefs ->
                prefs[stringPreferencesKey(pathKey(path))] = uri
            }
        }
    }

    private fun pathKey(path: String): String = "uri_${path.hashCode()}"
}
