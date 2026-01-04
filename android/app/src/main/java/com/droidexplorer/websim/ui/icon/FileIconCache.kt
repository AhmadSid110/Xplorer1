package com.droidexplorer.websim.ui.icon

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.vector.ImageVector

object FileIconCache {
    private val cache = mutableStateMapOf<String, ImageVector>()

    fun get(
        key: String,
        loader: () -> ImageVector
    ): ImageVector {
        return cache.getOrPut(key) { loader() }
    }
}
