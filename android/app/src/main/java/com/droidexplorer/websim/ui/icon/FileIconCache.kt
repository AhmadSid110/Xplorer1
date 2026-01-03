package com.droidexplorer.websim.ui.icon

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.painter.Painter

object FileIconCache {
    private val cache = mutableStateMapOf<String, Painter>()

    fun get(
        key: String,
        loader: () -> Painter
    ): Painter {
        return cache.getOrPut(key) { loader() }
    }
}
