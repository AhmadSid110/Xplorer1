package com.droidexplorer.websim.torbox

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * TorBox remote file data model.
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
 * NO retries, NO polling, NO background work.
 * Returns empty list on any error.
 */
class TorBoxClient(private val apiKey: String) {
    
    private val client = OkHttpClient.Builder()
        .build()
    
    companion object {
        private const val API_BASE_URL = "https://api.torbox.app/v1/api"
    }
    
    /**
     * Lists all files from TorBox.
     * 
     * @return List of files, or empty list on error
     */
    suspend fun listFiles(): List<TorBoxFile> {
        return try {
            val request = Request.Builder()
                .url("$API_BASE_URL/torrents/mylist")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return emptyList()
            }
            
            val body = response.body?.string() ?: return emptyList()
            parseFiles(body)
        } catch (e: IOException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Safely parses JSON response into TorBoxFile list.
     * Returns empty list on any parsing error.
     */
    private fun parseFiles(jsonString: String): List<TorBoxFile> {
        return try {
            val json = JSONObject(jsonString)
            val data = json.optJSONObject("data") ?: return emptyList()
            val torrents = data.optJSONArray("torrents") ?: return emptyList()
            
            val files = mutableListOf<TorBoxFile>()
            
            for (i in 0 until torrents.length()) {
                val torrent = torrents.optJSONObject(i) ?: continue
                val torrentFiles = torrent.optJSONArray("files") ?: continue
                
                for (j in 0 until torrentFiles.length()) {
                    val file = torrentFiles.optJSONObject(j) ?: continue
                    
                    val id = file.optString("id", "")
                    val name = file.optString("name", "")
                    val size = file.optLong("size", 0L)
                    val downloadUrl = file.optString("download_link", "")
                    
                    if (id.isNotEmpty() && name.isNotEmpty() && downloadUrl.isNotEmpty()) {
                        files.add(TorBoxFile(id, name, size, downloadUrl))
                    }
                }
            }
            
            files
        } catch (e: Exception) {
            emptyList()
        }
    }
}
