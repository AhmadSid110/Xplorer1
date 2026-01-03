package com.droidexplorer.websim.core.ops

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.minusAssign
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

private val Context.operationDataStore by preferencesDataStore("operation_state")

/**
 * Persists the currently active file operation so it can be resumed after process death or reboot.
 * Uses Java serialization to avoid adding new dependencies.
 */
class OperationPersistence(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeKey = stringPreferencesKey("active_operation")

    val activeOperation: Flow<FileOperation?> = context.operationDataStore.data.map { prefs ->
        prefs[activeKey]?.let { deserializeOperation(it) }
    }

    suspend fun snapshot(): FileOperation? = withContext(Dispatchers.IO) {
        activeOperation.firstOrNull()
    }

    fun persistAsync(operation: FileOperation) {
        scope.launch { persist(operation) }
    }

    suspend fun persist(operation: FileOperation) {
        withContext(Dispatchers.IO) {
            context.operationDataStore.edit { prefs ->
                prefs[activeKey] = serializeOperation(operation)
            }
        }
    }

    fun clearAsync(operationId: OperationId? = null) {
        scope.launch { clear(operationId) }
    }

    suspend fun clear(operationId: OperationId? = null) {
        withContext(Dispatchers.IO) {
            context.operationDataStore.edit { prefs ->
                val stored = prefs[activeKey]?.let { deserializeOperation(it) }
                if (operationId == null || stored?.id == operationId) {
                    prefs -= activeKey
                }
            }
        }
    }

    private fun serializeOperation(operation: FileOperation): String =
        serializeToBase64(operation)

    private fun deserializeOperation(data: String): FileOperation? =
        deserializeFromBase64(data)

    private fun serializeToBase64(value: Serializable): String {
        val stream = ByteArrayOutputStream()
        ObjectOutputStream(stream).use { out ->
            out.writeObject(value)
        }
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeFromBase64(data: String): FileOperation? {
        return runCatching {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
                input.readObject() as? FileOperation
            }
        }.getOrNull()
    }
}
