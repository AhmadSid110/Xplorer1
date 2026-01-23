package com.droidexplorer.websim.torbox

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.droidexplorer.websim.torbox.download.DownloadStatus
import com.droidexplorer.websim.torbox.download.TorBoxDatabaseProvider
import com.droidexplorer.websim.torbox.download.TorBoxDownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object TorBoxDownloadManager {
    const val KEY_FILE_ID = "torbox_file_id"
    const val KEY_FILE_NAME = "torbox_file_name"
    const val KEY_FILE_URL = "torbox_file_url"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(context: Context, fileId: String, name: String, url: String) {
        scope.launch {
            val dao = TorBoxDatabaseProvider.get(context).dao()
            dao.upsert(
                TorBoxDownloadEntity(
                    id = fileId,
                    name = name,
                    downloaded = 0L,
                    total = 0L,
                    status = DownloadStatus.QUEUED,
                    path = null,
                    speedBytesPerSec = 0L,
                    sourceUrl = url
                )
            )
        }

        enqueueWork(context, fileId, name, url)
    }

    fun pause(context: Context, fileId: String) {
        scope.launch {
            val dao = TorBoxDatabaseProvider.get(context).dao()
            val existing = dao.get(fileId) ?: return@launch
            dao.upsert(
                existing.copy(
                    status = DownloadStatus.PAUSED,
                    speedBytesPerSec = 0L
                )
            )
        }
        WorkManager.getInstance(context).cancelUniqueWork("torbox_download_$fileId")
    }

    fun resume(context: Context, fileId: String, name: String, url: String) {
        scope.launch {
            val dao = TorBoxDatabaseProvider.get(context).dao()
            val existing = dao.get(fileId)
            dao.upsert(
                TorBoxDownloadEntity(
                    id = fileId,
                    name = name,
                    downloaded = existing?.downloaded ?: 0L,
                    total = existing?.total ?: 0L,
                    status = DownloadStatus.QUEUED,
                    path = existing?.path,
                    speedBytesPerSec = 0L,
                    sourceUrl = url
                )
            )
        }
        enqueueWork(context, fileId, name, url)
    }

    private fun enqueueWork(context: Context, fileId: String, name: String, url: String) {
        val data = workDataOf(
            KEY_FILE_ID to fileId,
            KEY_FILE_NAME to name,
            KEY_FILE_URL to url
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<TorBoxDownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag("torbox_download:$fileId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("torbox_download_$fileId", ExistingWorkPolicy.REPLACE, request)
    }
}
