package com.droidexplorer.websim.torbox

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.droidexplorer.websim.file.FsNode

object TorBoxDownloadManager {
    const val KEY_FILE_ID = "torbox_file_id"
    const val KEY_FILE_NAME = "torbox_file_name"

    fun enqueue(context: Context, file: FsNode.TorBox) {
        val data = workDataOf(
            KEY_FILE_ID to file.id,
            KEY_FILE_NAME to file.name
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<TorBoxDownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag("torbox_download:${file.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("torbox_download_${file.id}", ExistingWorkPolicy.KEEP, request)
    }
}
