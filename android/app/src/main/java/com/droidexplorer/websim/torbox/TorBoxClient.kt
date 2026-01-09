package com.droidexplorer.websim.torbox

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class TorBoxFile(
    val id: String,
    val name: String,
    val size: Long,
    val downloadUrl: String
)

class TorBoxClient(private val apiKey: String) {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "TORBOX"
        // 🔥 THIS IS THE CRITICAL FIX
        private const val BASE_URL = "https://api.torbox.app/v1/api"
    }

    suspend fun listFiles(): List<TorBoxFile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "listFiles() called")

                val request = Request.Builder()
                    .url("$BASE_URL/torrents/mylist")
                    // 🔥 CRITICAL: Bearer auth (NOT X-API-Key)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                Log.d(TAG, "HTTP ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error ${response.code}")
                    return@withContext emptyList()
                }

                val body = response.body?.string().orEmpty()
                Log.d(TAG, "BODY=${body.take(2000)}")

                parse(body)
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                emptyList()
            }
        }
    }

    private fun parse(json: String): List<TorBoxFile> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: return emptyList()
            val torrents = data.optJSONArray("torrents") ?: return emptyList()

            val out = mutableListOf<TorBoxFile>()

            for (i in 0 until torrents.length()) {
                val torrent = torrents.optJSONObject(i) ?: continue
                val files = torrent.optJSONArray("files") ?: continue

                for (j in 0 until files.length()) {
                    val f = files.optJSONObject(j) ?: continue

                    val id = f.optString("id")
                    val name = f.optString("name")
                    val size = f.optLong("size", 0L)
                    val url = f.optString("download_link")

                    if (id.isNotBlank() && name.isNotBlank() && url.isNotBlank()) {
                        out += TorBoxFile(id, name, size, url)
                    }
                }
            }

            out
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
            emptyList()
        }
    }
}