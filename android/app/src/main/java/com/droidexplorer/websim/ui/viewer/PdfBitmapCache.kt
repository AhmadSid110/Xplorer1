package com.droidexplorer.websim.ui.viewer

import android.graphics.Bitmap

/**
 * Small LRU cache for rendered PDF page Bitmaps to avoid repeated expensive renders.
 * Keeps the most-recently-used pages up to MAX_PAGES.
 */
object PdfBitmapCache {
    private const val MAX_PAGES = 6
    private val cache = LinkedHashMap<Int, Bitmap>(MAX_PAGES, 0.75f, true)

    @Synchronized
    fun get(page: Int): Bitmap? = cache[page]

    @Synchronized
    fun put(page: Int, bitmap: Bitmap) {
        cache[page] = bitmap
        if (cache.size > MAX_PAGES) {
            val iter = cache.entries.iterator()
            if (iter.hasNext()) {
                iter.next()
                iter.remove()
            }
        }
    }

    @Synchronized
    fun clear() = cache.clear()
}
