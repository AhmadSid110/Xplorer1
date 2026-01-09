package com.droidexplorer.websim.torbox

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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
        private const val API_BASE =
            "https://api.torbox.app/v1/api/torrents/mylist"
    }

    suspend fun listFiles(): List<TorBoxFile> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling TorBox API")

                val request = Request.Builder()
                    .url(API_BASE)
                    .addHeader("Authorization", "Bearer $apiKey") // ✅ REQUIRED
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                Log.d(TAG, "HTTP ${response.code}")

                if (!response.isSuccessful) return@withContext emptyList()

                val body = response.body?.string() ?: return@withContext emptyList()
                Log.d(TAG, "BODY=${body.take(1000)}")

                parse(body)
            } catch (e: Exception) {
                Log.e(TAG, "TorBox error", e)
                emptyList()
            }
        }

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

            // TorBox does NOT give direct download_url here
            // Use absolute_path as identifier (download handled elsewhere)
            val path = obj.optString("absolute_path")

            if (id.isNotBlank() && name.isNotBlank()) {
                files.add(
                    TorBoxFile(
                        id = id,
                        name = name,
                        size = size,
                        downloadUrl = path
                    )
                )
            }
        }

        files
    } catch (e: Exception) {
        Log.e("TORBOX", "JSON parse failed", e)
        emptyList()
    }
}