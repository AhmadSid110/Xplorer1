package com.droidexplorer.websim.torbox

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * TorBox remote file data model.
 * Read-only representation of a downloadable file.
 */
data class TorBoxFile(
    val id: String,
    val name: String,
    val size: Long,
    val downloadUrl: String
)

/**
 * Read-only TorBox API client.
 *
 * - NO retries
 * - NO polling
 * - NO background services
 * - NO writes
 *
 * Play Store safe.
 */
class TorBoxClient(private val apiKey: String) {

    private val client = OkHttpClient()

    companion object {
        private const val API_BASE_URL = "https://api.torbox.app/v1"
        private const val TAG = "TORBOX"
    }

    /**
     * Lists all available TorBox torrents as files.
     *
     * @return list of TorBoxFile (empty on ANY failure)
     */
    suspend fun listFiles(): List<TorBoxFile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "listFiles() called")
                Log.d(TAG, "API key length=${apiKey.length}")

                val request = Request.Builder()
                    .url("$API_BASE_URL/torrents")
                    .addHeader("X-API-Key", apiKey)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                Log.d(TAG, "HTTP ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error ${response.code}")
                    return@withContext emptyList()
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    Log.e(TAG, "Empty response body")
                    return@withContext emptyList()
                }

                Log.d(TAG, "BODY=${body.take(2000)}")

                val files = parseFiles(body)
                Log.d(TAG, "Parsed files count=${files.size}")

                files
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                emptyList()
            }
        }
    }

    /**
     * Parses TorBox API JSON safely.
     *
     * Actual format (2025):
     * {
     *   "success": true,
     *   "data": [
     *     {
     *       "id": "...",
     *       "name": "...",
     *       "size": 123,
     *       "download": "https://..."
     *     }
     *   ]
     * }
     */
    private fun parseFiles(jsonString: String): List<TorBoxFile> {
        return try {
            val root = JSONObject(jsonString)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            val files = mutableListOf<TorBoxFile>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.optJSONObject(i) ?: continue

                val id = obj.optString("id")
                val name = obj.optString("name")
                val size = obj.optLong("size", 0L)

                // ✅ CORRECT FIELD NAME
                val downloadUrl = obj.optString("download")

                if (id.isNotBlank() && name.isNotBlank() && downloadUrl.isNotBlank()) {
                    files.add(
                        TorBoxFile(
                            id = id,
                            name = name,
                            size = size,
                            downloadUrl = downloadUrl
                        )
                    )
                }
            }

            files
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
            emptyList()
        }
    }
}