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
    val id: String,               // ✅ STRING (CRITICAL)
    val name: String,
    val size: Long,
    val absolutePath: String
)

/**
 * TorBox API client (READ-ONLY).
 *
 * ✔ Correct endpoint
 * ✔ Correct auth (Bearer)
 * ✔ Correct JSON structure
 * ✔ String IDs everywhere
 * ✔ Play Store safe
 */
class TorBoxClient(private val apiKey: String) {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "TORBOX"
        private const val LIST_URL =
            "https://api.torbox.app/v1/api/torrents/mylist"
    }

    /**
     * Fetches all files from TorBox.
     *
     * @return list of TorBoxFile (empty only on real failure)
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
     *   "data": {
     *     "torrents": [
     *       {
     *         "files": [
     *           {
     *             "id": 108,
     *             "name": "file.mp4",
     *             "size": 721727538,
     *             "absolute_path": "/completed/..."
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * }
     */
    private fun parseFiles(jsonString: String): List<TorBoxFile> {
        return try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()
            val torrents = data.optJSONArray("torrents") ?: return emptyList()

            val files = mutableListOf<TorBoxFile>()

            for (i in 0 until torrents.length()) {
                val torrent = torrents.optJSONObject(i) ?: continue
                val fileArray = torrent.optJSONArray("files") ?: continue

                for (j in 0 until fileArray.length()) {
                    val f = fileArray.optJSONObject(j) ?: continue

                    val id = f.optString("id")          // ✅ STRING
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
     *
     * @param fileId TorBox file ID (STRING)
     * @return HTTPS URL or null
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
                val link = json.optString("data").takeIf { it.isNotBlank() }

                Log.d(TAG, "Share link OK")
                link

            } catch (e: Exception) {
                Log.e(TAG, "Error getting share link", e)
                null
            }
        }
}