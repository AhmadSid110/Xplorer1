package com.droidexplorer.websim.torbox

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.droidexplorer.websim.R
import com.droidexplorer.websim.torbox.download.DownloadStatus
import com.droidexplorer.websim.torbox.download.TorBoxDatabaseProvider
import com.droidexplorer.websim.torbox.download.TorBoxDownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class TorBoxDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(TorBoxDownloadManager.KEY_FILE_ID) ?: return Result.failure()
        val fileName = inputData.getString(TorBoxDownloadManager.KEY_FILE_NAME) ?: "torbox_$fileId"
        val url = inputData.getString(TorBoxDownloadManager.KEY_FILE_URL) ?: return Result.failure()

        val dao = TorBoxDatabaseProvider.get(applicationContext).dao()
        val existing = dao.get(fileId)
        val downloadedBytes = existing?.downloaded ?: 0L

        setForeground(createForegroundInfo(fileId, fileName, downloadedBytes, existing?.total ?: 0L))

        val downloadsDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: applicationContext.filesDir
        val safeName = sanitizeFileName(fileName)
        val finalFile = File(downloadsDir, safeName)
        val partFile = File(downloadsDir, "$safeName.part")

        return withContext(Dispatchers.IO) {
            var existingBytes = 0L
            try {
                existingBytes = if (partFile.exists()) partFile.length() else 0L
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()

                if (existingBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 416) {
                            if (partFile.exists()) {
                                if (finalFile.exists()) finalFile.delete()
                                partFile.renameTo(finalFile)
                                showCompletedNotification(fileId, fileName)
                            }
                            return@use
                        }
                        throw IOException("HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty response")
                    val contentLength = body.contentLength().let { if (it < 0) 0L else it }
                    val totalBytes = if (response.code == 206) existingBytes + contentLength else contentLength

                    if (response.code == 200 && existingBytes > 0) {
                        partFile.delete()
                    }

                    val target = partFile.apply { parentFile?.mkdirs() }
                    val append = response.code == 206 && existingBytes > 0
                    if (!append && target.exists()) {
                        target.delete()
                    }

                    var downloaded = if (append && target.exists()) target.length() else 0L
                    dao.upsert(
                        TorBoxDownloadEntity(
                            id = fileId,
                            name = fileName,
                            downloaded = downloaded,
                            total = totalBytes,
                            status = DownloadStatus.DOWNLOADING,
                            path = target.absolutePath
                        )
                    )
                    setForeground(createForegroundInfo(fileId, fileName, downloaded, totalBytes))

                    body.byteStream().use { input ->
                        FileOutputStream(target, append).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            var lastUpdate = 0L
                            while (input.read(buffer).also { read = it } >= 0) {
                                if (read == 0) continue
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 750L) {
                                    dao.upsert(
                                        TorBoxDownloadEntity(
                                            id = fileId,
                                            name = fileName,
                                            downloaded = downloaded,
                                            total = totalBytes,
                                            status = DownloadStatus.DOWNLOADING,
                                            path = target.absolutePath
                                        )
                                    )
                                    setForeground(createForegroundInfo(fileId, fileName, downloaded, totalBytes))
                                    lastUpdate = now
                                }
                            }
                        }
                    }

                    if (finalFile.exists()) finalFile.delete()
                    partFile.renameTo(finalFile)
                    dao.upsert(
                        TorBoxDownloadEntity(
                            id = fileId,
                            name = fileName,
                            downloaded = downloaded,
                            total = totalBytes,
                            status = DownloadStatus.COMPLETED,
                            path = finalFile.absolutePath
                        )
                    )
                    showCompletedNotification(fileId, fileName)
                }

                Result.success()
            } catch (e: Exception) {
                dao.upsert(
                    TorBoxDownloadEntity(
                        id = fileId,
                        name = fileName,
                        downloaded = existingBytes,
                        total = existing?.total ?: 0L,
                        status = DownloadStatus.FAILED,
                        path = finalFile.absolutePath
                    )
                )
                Result.retry()
            }
        }
    }

    private fun createForegroundInfo(
        fileId: String,
        fileName: String,
        downloaded: Long,
        total: Long
    ): ForegroundInfo {
        val manager = notificationManager()
        ensureChannel(manager)

        val safeTotal = if (total > 0) min(total, Int.MAX_VALUE.toLong()) else 0L
        val progressMax = if (safeTotal > 0) max(safeTotal.toInt(), 1) else 0
        val progressValue = if (safeTotal > 0) downloaded.coerceAtMost(safeTotal).toInt() else 0

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText("Downloading…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progressMax > 0) {
            builder.setProgress(progressMax, progressValue, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return ForegroundInfo(notificationId(fileId), builder.build())
    }

    private fun showCompletedNotification(fileId: String, fileName: String) {
        val manager = notificationManager()
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId(fileId) + 1, notification)
    }

    private fun notificationManager(): NotificationManager {
        return applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TorBox Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun notificationId(fileId: String): Int = fileId.hashCode()

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (cleaned.isBlank()) "torbox_download" else cleaned
    }

    companion object {
        private const val CHANNEL_ID = "torbox_downloads"
    }
}
