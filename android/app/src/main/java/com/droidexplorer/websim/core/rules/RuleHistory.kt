package com.droidexplorer.websim.core.rules

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.minusAssign
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
import java.io.Serializable

private val Context.ruleHistoryDataStore by preferencesDataStore("rule_history")

data class RuleHistoryEntry(
    val ruleId: String,
    val affectedFiles: List<String>,
    val operations: List<com.droidexplorer.websim.core.ops.FileOperation>,
    val rollbackOperations: List<com.droidexplorer.websim.core.ops.FileOperation>,
    val timestamp: Long
) : Serializable

class RuleHistoryStore(private val context: Context) {

    private val historyKey = stringPreferencesKey("entries")

    val history: Flow<List<RuleHistoryEntry>> = context.ruleHistoryDataStore.data.map { prefs ->
        prefs[historyKey]?.let { deserializeList(it) } ?: emptyList()
    }

    suspend fun record(entry: RuleHistoryEntry) {
        withContext(Dispatchers.IO) {
            context.ruleHistoryDataStore.edit { prefs ->
                val current = prefs[historyKey]?.let { deserializeList(it) } ?: emptyList()
                val updated = (current + entry).takeLast(HISTORY_LIMIT)
                prefs[historyKey] = serializeList(updated)
            }
        }
    }

    suspend fun popLatest(): RuleHistoryEntry? = withContext(Dispatchers.IO) {
        var latest: RuleHistoryEntry? = null
        context.ruleHistoryDataStore.edit { prefs ->
            val current = prefs[historyKey]?.let { deserializeList(it) } ?: emptyList()
            latest = current.lastOrNull()
            val remaining = if (current.isNotEmpty()) current.dropLast(1) else emptyList()
            if (remaining.isEmpty()) {
                prefs -= historyKey
            } else {
                prefs[historyKey] = serializeList(remaining)
            }
        }
        latest
    }

    private fun serializeList(entries: List<RuleHistoryEntry>): String {
        val stream = ByteArrayOutputStream()
        ObjectOutputStream(stream).use { out ->
            out.writeObject(entries)
        }
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeList(data: String): List<RuleHistoryEntry> {
        return runCatching {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
                input.readObject() as? List<RuleHistoryEntry>
            }
        }.getOrElse {
            Log.w("RuleHistoryStore", "Failed to deserialize rule history", it)
            emptyList()
        } ?: emptyList()
    }

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}
