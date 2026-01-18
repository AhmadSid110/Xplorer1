package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import java.util.concurrent.Executors

private const val MAX_BITMAP_DIMENSION = 2048

object PdfRenderExecutor {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "PdfRendererWorker") }

    suspend fun renderPage(renderer: PdfRenderer, pageIndex: Int, targetWidth: Int): Pair<String, Bitmap> {
        return withContext(Dispatchers.IO) {
            val future = executor.submit(Callable {
                val page = synchronized(renderer) { renderer.openPage(pageIndex) }
                try {
                    val srcWidth = page.width
                    val srcHeight = page.height

                    val w = targetWidth.coerceAtMost(MAX_BITMAP_DIMENSION)
                    val h = (srcHeight * (w / srcWidth.toFloat())).toInt().coerceAtLeast(100).coerceAtMost(MAX_BITMAP_DIMENSION)

                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    // Cache from worker thread
                    PdfBitmapCache.put(pageIndex, bmp)

                    Pair(Thread.currentThread().name, bmp)
                } finally {
                    synchronized(renderer) { page.close() }
                }
            })

            try {
                future.get()
            } catch (e: Exception) {
                // propagate
                throw e
            }
        }
    }
}
