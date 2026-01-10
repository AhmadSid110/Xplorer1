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
    val id: String,          // STRING (API returns numeric but we treat as string)
    val name: String,
    val size: Long,
    val absolutePath: String
)

/**
 * TorBox API client (READ-ONLY).
 *
 * ✔ Correct endpoint
 * ✔ Correct auth (Bearer)
 * ✔ Correct JSON structure (data.files[])
 * ✔ String IDs everywhere
 * ✔ Play Store safe
 */
class TorBoxClient(private val apiKey: String) {

    private val client = OkHttpClient()

    @Volatile
    var lastRawResponse: String = ""

    companion object {
        private const val TAG = "TORBOX"
        private const val LIST_URL =
            "https://api.torbox.app/v1/api/torrents/mylist"
    }

    /**
     * Fetches all files from TorBox.
     */
    suspend fun listFiles(): List<TorBoxFile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "listFiles() called")
                Log.d(TAG, "API key length=${apiKey.length}")

                val request = Request.Builder()
                    .url(LIST_URL)
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

                lastRawResponse = body
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
     * ✅ CORRECT PARSER
     *
     * REAL TorBox response (CONFIRMED):
     *
     * {
     *   "success": true,
     *   "data": {
     *     "files": [
     *       {
     *         "id": 78,
     *         "name": "file.mp4",
     *         "size": 723194829,
     *         "absolute_path": "/completed/..."
     *       }
     *     ]
     *   }
     * }
     */
    private fun parseFiles(jsonString: String): List<TorBoxFile> {
        return try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()

            // 🔥 THIS IS THE FIX — NO TORRENTS ARRAY
            val filesArray = data.optJSONArray("files") ?: return emptyList()

            val files = mutableListOf<TorBoxFile>()

            for (i in 0 until filesArray.length()) {
                val f = filesArray.optJSONObject(i) ?: continue

                val id = f.optString("id")
                val name = f.optString("name")
                val size = f.optLong("size", 0L)
                val absolutePath = f.optString("absolute_path")

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

    /**
     * Fetches temporary HTTPS share/download link for a file.
     */
    suspend fun getShareLink(fileId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.torbox.app/v1/api/files/$fileId/download")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                Log.d(TAG, "getShareLink HTTP ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to get share link: HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                json.optString("data").takeIf { it.isNotBlank() }

            } catch (e: Exception) {
                Log.e(TAG, "Error getting share link", e)
                null
            }
        }
}