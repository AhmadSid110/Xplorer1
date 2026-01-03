package com.droidexplorer.websim.core.ops

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private val Context.operationDataStore by preferencesDataStore("operation_state")

/**
 * Persists the currently running file operation so it can be restored after process death
 * or device reboot. Uses DataStore to keep the payload small and resilient.
 */
class OperationPersistence(private val context: Context) {

    private val keyActive = stringPreferencesKey("active_operation")

    suspend fun persist(operation: FileOperation) {
        withContext(Dispatchers.IO) {
            context.operationDataStore.edit { prefs ->
                prefs[keyActive] = serialize(operation)
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            context.operationDataStore.edit { prefs ->
                prefs.remove(keyActive)
            }
        }
    }

    suspend fun restore(): FileOperation? {
        val prefs = withContext(Dispatchers.IO) {
            context.operationDataStore.data.first()
        }
        val encoded = prefs[keyActive] ?: return null
        return deserialize(encoded)
    }

    fun observe(): Flow<FileOperation?> =
        context.operationDataStore.data.map { prefs ->
            prefs[keyActive]?.let { encoded ->
                deserialize(encoded)
            }
        }

    private fun serialize(operation: FileOperation): String {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { it.writeObject(operation) }
        return Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun deserialize(value: String): FileOperation? {
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
                input.readObject() as? FileOperation
            }
        }.getOrNull()
    }
}
