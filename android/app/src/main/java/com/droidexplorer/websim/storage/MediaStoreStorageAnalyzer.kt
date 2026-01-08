package com.droidexplorer.websim.storage

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.os.Build

/**
 * Analyzes storage usage by category using MediaStore queries.
 * 
 * This implementation is Play Store-safe and complies with scoped storage:
 * - Uses MediaStore APIs only (no filesystem scanning)
 * - Does NOT use File.listFiles()
 * - Does NOT require MANAGE_ALL_FILES_ACCESS
 * - Does NOT scan /storage/emulated/0 recursively
 * - Does NOT count app private storage
 * 
 * Categories computed:
 * - Images: MIME types image/*
 * - Videos: MIME types video/*
 * - Audio: MIME types audio/*
 * - APKs: MIME type application/vnd.android.package-archive
 * - Archives: MIME types for zip, rar, tar, gz, etc.
 */
class MediaStoreStorageAnalyzer(private val contentResolver: ContentResolver) {

    data class StorageCategoryData(
        val images: Long = 0,
        val videos: Long = 0,
        val audio: Long = 0,
        val apks: Long = 0,
        val archives: Long = 0
    )

    /**
     * Analyzes storage usage by querying MediaStore for each category.
     * Returns byte totals for each category.
     */
    fun analyze(): StorageCategoryData {
        return StorageCategoryData(
            images = queryMediaStoreSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videos = queryMediaStoreSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audio = queryMediaStoreSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            apks = queryFilesByMimeType("application/vnd.android.package-archive"),
            archives = queryArchives()
        )
    }

    /**
     * Queries MediaStore for total size of files in a specific content URI.
     */
    private fun queryMediaStoreSize(contentUri: Uri): Long {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        var totalSize = 0L

        try {
            contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeColumn)
                }
            }
        } catch (e: Exception) {
            // Handle gracefully - may fail on some devices or Android versions
            // Return 0 if query fails
        }

        return totalSize
    }

    /**
     * Queries files by MIME type using MediaStore.Files.
     * Used for APKs and other file types not in standard media collections.
     */
    private fun queryFilesByMimeType(mimeType: String): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // MediaStore.Files queries may be restricted on older versions
            return 0
        }

        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf(mimeType)
        var totalSize = 0L

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeColumn)
                }
            }
        } catch (e: Exception) {
            // Handle gracefully - return 0 if query fails
        }

        return totalSize
    }

    /**
     * Queries archive files (zip, rar, tar, gz, etc.) using MediaStore.Files.
     */
    private fun queryArchives(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return 0
        }

        val archiveMimeTypes = listOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-7z-compressed"
        )

        var totalSize = 0L

        for (mimeType in archiveMimeTypes) {
            totalSize += queryFilesByMimeType(mimeType)
        }

        return totalSize
    }
}
