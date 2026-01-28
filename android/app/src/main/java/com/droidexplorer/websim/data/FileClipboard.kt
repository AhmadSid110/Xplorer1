package com.droidexplorer.websim.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ClipboardOperation { COPY, MOVE }

data class ClipboardItem(
    val sourcePaths: List<String>,
    val operation: ClipboardOperation
)

object FileClipboard {
    var item by mutableStateOf<ClipboardItem?>(null)
        private set

    fun copy(path: String) {
        item = ClipboardItem(listOf(path), ClipboardOperation.COPY)
    }

    fun copy(paths: List<String>) {
        item = ClipboardItem(paths, ClipboardOperation.COPY)
    }

    fun cut(path: String) {
        item = ClipboardItem(listOf(path), ClipboardOperation.MOVE)
    }

    fun cut(paths: List<String>) {
        item = ClipboardItem(paths, ClipboardOperation.MOVE)
    }

    fun clear() {
        item = null
    }

    fun hasItem(): Boolean = item != null
}
