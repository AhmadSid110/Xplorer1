package com.droidexplorer.websim.torbox

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.droidexplorer.websim.R
import com.droidexplorer.websim.torbox.download.DownloadStatus
import com.droidexplorer.websim.torbox.download.TorBoxDatabaseProvider
import com.droidexplorer.websim.torbox.download.TorBoxDownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
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

        setForeground(
            createForegroundInfo(
                fileId,
                fileName,
                downloadedBytes,
                existing?.total ?: 0L,
                existing?.speedBytesPerSec ?: 0L
            )
        )

        val downloadsDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: applicationContext.filesDir
        val safeName = sanitizeFileName(fileName)
        val finalFile = File(downloadsDir, safeName)

        return withContext(Dispatchers.IO) {
            try {
                val probe = probeDownload(url)
                val totalBytes = probe.totalBytes
                val supportsRange = probe.supportsRange && totalBytes > 0L
                val segmentCount = if (supportsRange && totalBytes >= MIN_MULTI_THREAD_BYTES) {
                    DEFAULT_SEGMENTS
                } else {
                    1
                }

                val segments = if (segmentCount > 1) {
                    buildSegments(totalBytes, segmentCount)
                } else {
                    listOf(Segment(0, 0L, if (totalBytes > 0L) totalBytes - 1 else -1L))
                }

                val segmentFiles = segments.map { segment ->
                    if (segmentCount > 1) {
                        File(downloadsDir, "$safeName.part${segment.index}")
                    } else {
                        File(downloadsDir, "$safeName.part")
                    }
                }

                if (!supportsRange) {
                    segmentFiles.forEach { it.delete() }
                }

                val startingBytes = if (supportsRange) {
                    segmentFiles.sumOf { if (it.exists()) it.length() else 0L }
                } else {
                    0L
                }
                val progress = ProgressTracker(
                    fileId = fileId,
                    fileName = fileName,
                    totalBytes = totalBytes,
                    targetPath = finalFile.absolutePath,
                    sourceUrl = existing?.sourceUrl ?: url,
                    dao = dao,
                    worker = this@TorBoxDownloadWorker,
                    startingBytes = startingBytes
                )

                dao.upsert(
                    TorBoxDownloadEntity(
                        id = fileId,
                        name = fileName,
                        downloaded = startingBytes,
                        total = totalBytes,
                        status = DownloadStatus.DOWNLOADING,
                        path = finalFile.absolutePath,
                        speedBytesPerSec = 0L,
                        sourceUrl = existing?.sourceUrl ?: url
                    )
                )
                setForeground(
                    createForegroundInfo(
                        fileId,
                        fileName,
                        startingBytes,
                        totalBytes,
                        0L
                    )
                )

                coroutineScope {
                    segments.mapIndexed { index, segment ->
                        async {
                            downloadSegment(
                                url = url,
                                segment = segment,
                                target = segmentFiles[index],
                                supportsRange = supportsRange,
                                progress = progress
                            )
                        }
                    }.awaitAll()
                }

                if (segmentCount > 1) {
                    mergeSegments(finalFile, segmentFiles)
                } else {
                    val partFile = segmentFiles.first()
                    if (finalFile.exists()) finalFile.delete()
                    partFile.renameTo(finalFile)
                }

                dao.upsert(
                    TorBoxDownloadEntity(
                        id = fileId,
                        name = fileName,
                        downloaded = finalFile.length(),
                        total = totalBytes,
                        status = DownloadStatus.COMPLETED,
                        path = finalFile.absolutePath,
                        speedBytesPerSec = 0L,
                        sourceUrl = existing?.sourceUrl ?: url
                    )
                )
                showCompletedNotification(fileId, fileName, finalFile)
                Result.success()
            } catch (e: PausedException) {
                val latest = dao.get(fileId)
                dao.upsert(
                    TorBoxDownloadEntity(
                        id = fileId,
                        name = fileName,
                        downloaded = latest?.downloaded ?: 0L,
                        total = latest?.total ?: 0L,
                        status = DownloadStatus.PAUSED,
                        path = finalFile.absolutePath,
                        speedBytesPerSec = 0L,
                        sourceUrl = latest?.sourceUrl ?: url
                    )
                )
                Result.success()
            } catch (e: Exception) {
                val latest = dao.get(fileId)
                dao.upsert(
                    TorBoxDownloadEntity(
                        id = fileId,
                        name = fileName,
                        downloaded = latest?.downloaded ?: 0L,
                        total = latest?.total ?: 0L,
                        status = DownloadStatus.FAILED,
                        path = finalFile.absolutePath,
                        speedBytesPerSec = 0L,
                        sourceUrl = latest?.sourceUrl ?: url
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
        total: Long,
        speedBytesPerSec: Long
    ): ForegroundInfo {
        val manager = notificationManager()
        ensureChannel(manager)

        val safeTotal = if (total > 0) min(total, Int.MAX_VALUE.toLong()) else 0L
        val progressMax = if (safeTotal > 0) max(safeTotal.toInt(), 1) else 0
        val progressValue = if (safeTotal > 0) downloaded.coerceAtMost(safeTotal).toInt() else 0

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(
                if (speedBytesPerSec > 0L) {
                    "Downloading… ${formatSpeed(speedBytesPerSec)}"
                } else {
                    "Downloading…"
                }
            )
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

    private fun showCompletedNotification(fileId: String, fileName: String, file: File) {
        val manager = notificationManager()
        ensureChannel(manager)
        val intent = buildOpenFileIntent(file)
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                applicationContext,
                notificationId(fileId) + 1000,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        manager.notify(notificationId(fileId) + 1, builder.build())
    }

    private fun buildOpenFileIntent(file: File): Intent? {
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.provider",
            file
        )
        val mime = file.extension.takeIf { it.isNotBlank() }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
            ?: "*/*"
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
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
        val cleaned = name.replace(Regex("[\\/:*?\"<>|]"), "_")
        return if (cleaned.isBlank()) "torbox_download" else cleaned
    }

    private suspend fun downloadSegment(
        url: String,
        segment: Segment,
        target: File,
        supportsRange: Boolean,
        progress: ProgressTracker
    ) {
        if (segment.end >= 0 && segment.start > segment.end) return

        target.parentFile?.mkdirs()

        var existing = if (target.exists()) target.length() else 0L
        val totalSegmentBytes = if (segment.end >= 0) (segment.end - segment.start + 1) else -1L
        if (totalSegmentBytes > 0 && existing >= totalSegmentBytes) {
            return
        }

        val rangeStart = if (supportsRange) segment.start + existing else 0L
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (supportsRange) {
            val rangeEnd = if (segment.end >= 0) segment.end else ""
            requestBuilder.addHeader("Range", "bytes=$rangeStart-$rangeEnd")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 416) {
                    return
                }
                throw IOException("HTTP ${response.code}")
            }

            val body = response.body ?: throw IOException("Empty response")
            val append = supportsRange && (response.code == 206)
            if (!append && target.exists()) {
                target.delete()
                existing = 0L
            }

            body.byteStream().use { input ->
                FileOutputStream(target, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        progress.onBytesRead(read.toLong())
                    }
                }
            }
        }
    }

    private fun mergeSegments(finalFile: File, parts: List<File>) {
        if (finalFile.exists()) finalFile.delete()
        finalFile.outputStream().use { output ->
            parts.forEach { part ->
                part.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
        parts.forEach { it.delete() }
    }

    private fun buildSegments(totalBytes: Long, segmentCount: Int): List<Segment> {
        val size = totalBytes / segmentCount
        val remainder = totalBytes % segmentCount
        val segments = mutableListOf<Segment>()
        var cursor = 0L
        for (i in 0 until segmentCount) {
            val extra = if (i < remainder) 1 else 0
            val length = size + extra
            val start = cursor
            val end = cursor + length - 1
            segments.add(Segment(i, start, end))
            cursor += length
        }
        return segments
    }

    private fun probeDownload(url: String): DownloadProbe {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val supportsRange = response.code == 206
                val contentRange = response.header("Content-Range")
                val total = when {
                    supportsRange -> parseTotalFromContentRange(contentRange)
                    else -> response.body?.contentLength()?.takeIf { it > 0 } ?: 0L
                }
                DownloadProbe(totalBytes = total, supportsRange = supportsRange)
            }
        } catch (e: Exception) {
            DownloadProbe(totalBytes = 0L, supportsRange = false)
        }
    }

    private fun parseTotalFromContentRange(value: String?): Long {
        val total = value?.substringAfter("/")?.trim()
        return total?.toLongOrNull() ?: 0L
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        val value = bytesPerSec.toDouble()
        return when {
            value >= gb -> String.format("%.2f GB/s", value / gb)
            value >= mb -> String.format("%.2f MB/s", value / mb)
            value >= kb -> String.format("%.1f KB/s", value / kb)
            else -> "${bytesPerSec} B/s"
        }
    }

    private class ProgressTracker(
        private val fileId: String,
        private val fileName: String,
        private val totalBytes: Long,
        private val targetPath: String,
        private val sourceUrl: String?,
        private val dao: com.droidexplorer.websim.torbox.download.TorBoxDownloadDao,
        private val worker: TorBoxDownloadWorker,
        startingBytes: Long
    ) {
        private val downloaded = AtomicLong(startingBytes)
        private val lastBytes = AtomicLong(startingBytes)
        private val lastUpdate = AtomicLong(System.currentTimeMillis())

        suspend fun onBytesRead(delta: Long) {
            val current = downloaded.addAndGet(delta)
            val now = System.currentTimeMillis()
            val last = lastUpdate.get()
            if (now - last < UPDATE_INTERVAL_MS) return
            if (!lastUpdate.compareAndSet(last, now)) return

            val prev = lastBytes.getAndSet(current)
            val elapsed = (now - last).coerceAtLeast(1)
            val speed = ((current - prev) * 1000L / elapsed).coerceAtLeast(0L)

            val status = dao.get(fileId)?.status
            if (status == DownloadStatus.PAUSED) {
                throw PausedException()
            }

            dao.upsert(
                TorBoxDownloadEntity(
                    id = fileId,
                    name = fileName,
                    downloaded = current,
                    total = totalBytes,
                    status = DownloadStatus.DOWNLOADING,
                    path = targetPath,
                    speedBytesPerSec = speed,
                    sourceUrl = sourceUrl
                )
            )
            worker.setForeground(
                worker.createForegroundInfo(
                    fileId,
                    fileName,
                    current,
                    totalBytes,
                    speed
                )
            )
            currentCoroutineContext().ensureActive()
        }
    }

    private data class Segment(val index: Int, val start: Long, val end: Long)

    private data class DownloadProbe(val totalBytes: Long, val supportsRange: Boolean)

    private class PausedException : Exception()

    companion object {
        private const val CHANNEL_ID = "torbox_downloads"
        private const val DEFAULT_SEGMENTS = 4
        private const val MIN_MULTI_THREAD_BYTES = 8L * 1024L * 1024L
        private const val UPDATE_INTERVAL_MS = 750L
    }
}
