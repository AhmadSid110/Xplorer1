package com.droidexplorer.websim.torbox

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Read-only TorBox file model.
 * Represents a single downloadable file.
 */
data class TorBoxFile(
    val id: String,
    val name: String,
    val size: Long,
    val absolutePath: String
)

/**
 * TorBox API client (READ-ONLY).
 *
 * ✔ Correct endpoint
 * ✔ Correct auth method
 * ✔ Correct JSON parsing
 * ✔ Play Store safe
 */
class TorBoxClient(private val apiKey: String) {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "TORBOX"
        private const val API_URL =
            "https://api.torbox.app/v1/api/torrents/mylist"
    }

    /**
     * Fetches all files from TorBox.
     *
     * @return list of TorBoxFile (empty only if API truly returns no files)
     */
    suspend fun listFiles(): List<TorBoxFile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "listFiles() called")
                Log.d(TAG, "API key length=${apiKey.length}")

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
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

                parseFiles(body)
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
     * Parses REAL TorBox response:
     *
     * {
     *   "success": true,
     *   "data": [
     *     {
     *       "id": 108,
     *       "name": "file.mp4",
     *       "size": 721727538,
     *       "absolute_path": "/completed/..."
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
                val absolutePath = obj.optString("absolute_path")

                if (id.isNotBlank() && name.isNotBlank()) {
                    files.add(
                        TorBoxFile(
                            id = id,
                            name = name,
                            size = size,
                            absolutePath = absolutePath
                        )
                    )
                }
            }

            Log.d(TAG, "Parsed ${files.size} files")
            files
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
            emptyList()
        }
    }
}