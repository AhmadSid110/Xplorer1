package com.droidexplorer.websim.torbox

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object TorBoxTempDownloader {
    private val client = OkHttpClient()

    suspend fun downloadToCache(context: Context, url: String, fileName: String): File =
        withContext(Dispatchers.IO) {
            val safeName = sanitizeFileName(fileName)
            val dir = File(context.cacheDir, "torbox_temp").apply { mkdirs() }
            val target = File(dir, safeName)

            if (target.exists() && target.length() > 0) {
                return@withContext target
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response")
                target.outputStream().use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
            target
        }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (cleaned.isBlank()) "torbox_temp" else cleaned
    }
}
