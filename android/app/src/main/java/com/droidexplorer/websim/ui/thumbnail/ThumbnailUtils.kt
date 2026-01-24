package com.droidexplorer.websim.ui.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import com.droidexplorer.websim.core.metadata.MetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private const val MAX_ENTRIES = 50
    private val cache = object : LruCache<String, Bitmap>(MAX_ENTRIES) {}

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bmp: Bitmap) {
        cache.put(key, bmp)
    }
}

suspend fun loadImageThumbnail(file: File, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
    val key = "img:${file.path}:$sizePx"
    ThumbnailCache.get(key)?.let { return@withContext it }

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

    options.inSampleSize = calculateSampleSize(options, sizePx, sizePx)
    options.inJustDecodeBounds = false
    val bmp = BitmapFactory.decodeFile(file.path, options)
    if (bmp != null) {
        ThumbnailCache.put(key, bmp)
    }
    bmp
}

suspend fun loadVideoThumbnail(file: File, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
    val key = "vid:${file.path}:$sizePx"
    ThumbnailCache.get(key)?.let { return@withContext it }

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)
        val bmp = retriever.getFrameAtTime(0)
        if (bmp != null) {
            ThumbnailCache.put(key, bmp)
        }
        bmp
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

suspend fun loadVideoDurationLabel(file: File): String? = withContext(Dispatchers.IO) {
    val meta = MetadataReader.readVideo(file) ?: return@withContext null
    val durationMs = meta.durationMs ?: return@withContext null
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    "%02d:%02d".format(minutes, seconds)
}

private fun calculateSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (width, height) = options.outWidth to options.outHeight
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
