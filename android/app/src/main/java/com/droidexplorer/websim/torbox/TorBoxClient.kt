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

    private fun parse(json: String): List<TorBoxFile> {
        val root = JSONObject(json)
        val data = root.getJSONObject("data")
        val torrents = data.getJSONArray("torrents")

        val out = mutableListOf<TorBoxFile>()

        for (i in 0 until torrents.length()) {
            val torrent = torrents.getJSONObject(i)
            val files = torrent.getJSONArray("files")

            for (j in 0 until files.length()) {
                val f = files.getJSONObject(j)

                out += TorBoxFile(
                    id = f.getString("id"),
                    name = f.getString("name"),
                    size = f.optLong("size", 0),
                    downloadUrl = f.getString("download_link")
                )
            }
        }
        return out
    }
}