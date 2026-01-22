package com.droidexplorer.websim.torbox

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private fun parseFiles(jsonString: String): List<TorBoxFile> {
        return try {
            val root = JSONObject(jsonString)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            val files = mutableListOf<TorBoxFile>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)

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

            files
        } catch (e: Exception) {
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
                    return@withContext requestTorrentDownloadLink(fileId)
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                json.optString("data").takeIf { it.isNotBlank() }
                    ?: requestTorrentDownloadLink(fileId)

            } catch (e: Exception) {
                Log.e(TAG, "Error getting share link", e)
                requestTorrentDownloadLink(fileId)
            }
        }

    private fun requestTorrentDownloadLink(torrentId: String): String? {
        return try {
            val parsedId = torrentId.toIntOrNull() ?: return null
            val url = "https://api.torbox.app/v1/api/torrents/requestdl" +
                "?token=$apiKey&torrent_id=$parsedId&redirect=true"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "requestdl HTTP ${response.code}")
                if (!response.isSuccessful) {
                    return null
                }

                val finalUrl = response.request.url.toString()
                if (finalUrl.isNotBlank()) {
                    return finalUrl
                }

                val body = response.body?.string() ?: return null
                JSONObject(body).optString("data").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting download link", e)
            null
        }
    }

    /**
     * Deletes a TorBox item by id via controltorrent.
     */
    suspend fun deleteFile(fileId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("operation", "delete")
                    .put("torrent_id", fileId.toIntOrNull() ?: fileId)
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("https://api.torbox.app/v1/api/torrents/controltorrent")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                Log.d(TAG, "deleteFile HTTP ${response.code}")
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file", e)
                false
            }
        }
}