package com.droidexplorer.websim.core.metadata

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MetadataReader {
    suspend fun readImage(file: File): ImageMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val exif = ExifInterface(file)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1).takeIf { it > 0 }
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1).takeIf { it > 0 }
            val orientation = exif.rotationDegrees
            val summary = buildMap<String, String> {
                exif.getAttribute(ExifInterface.TAG_DATETIME)?.takeIf { it.isNotBlank() }?.let {
                    put("Taken", it)
                }
                val camera = listOfNotNull(
                    exif.getAttribute(ExifInterface.TAG_MAKE),
                    exif.getAttribute(ExifInterface.TAG_MODEL)
                ).joinToString(" ").trim()
                if (camera.isNotEmpty()) {
                    put("Camera", camera)
                }
                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.takeIf { it.isNotBlank() }?.let {
                    put("FocalLength", it)
                }
            }
            ImageMetadata(width = width, height = height, orientation = orientation, summary = summary)
        }.getOrNull()
    }

    suspend fun readVideo(file: File): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
            VideoMetadata(durationMs = duration, width = width, height = height, rotation = rotation)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    suspend fun readAudio(file: File): AudioMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            AudioMetadata(durationMs = duration, bitrate = bitrate)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    suspend fun readApk(context: Context, file: File): ApkMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val flags = PackageManager.GET_PERMISSIONS
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    file.path,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(file.path, flags)
            }
            pkgInfo?.applicationInfo?.let { info ->
                info.sourceDir = file.path
                info.publicSourceDir = file.path
            }
            ApkMetadata(
                permissions = pkgInfo?.requestedPermissions?.toList().orEmpty()
            )
        }.getOrNull()
    }
}

data class ImageMetadata(
    val width: Int?,
    val height: Int?,
    val orientation: Int?,
    val summary: Map<String, String>
)

data class VideoMetadata(
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val rotation: Int?
)

data class AudioMetadata(
    val durationMs: Long?,
    val bitrate: Int?
)

data class ApkMetadata(
    val permissions: List<String>
)
