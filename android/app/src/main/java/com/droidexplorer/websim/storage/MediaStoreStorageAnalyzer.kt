package com.droidexplorer.websim.storage

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MediaStoreStorageAnalyzer(
    private val contentResolver: ContentResolver
) {

    data class StorageCategoryData(
        val images: Long = 0,
        val videos: Long = 0,
        val audio: Long = 0,
        val apks: Long = 0,
        val archives: Long = 0
    )

    fun analyze(): StorageCategoryData {
        return StorageCategoryData(
            images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audio = queryMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            apks = queryByMime("application/vnd.android.package-archive"),
            archives = queryArchives()
        )
    }

    private fun queryMedia(uri: Uri): Long {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        var total = 0L

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(index)
            }
        }

        return total
    }

    private fun queryByMime(mime: String): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0

        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
        val args = arrayOf(mime)
        var total = 0L

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(index)
            }
        }

        return total
    }

    private fun queryArchives(): Long {
        val types = listOf(
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip"
        )

        return types.sumOf { queryByMime(it) }
    }
}
