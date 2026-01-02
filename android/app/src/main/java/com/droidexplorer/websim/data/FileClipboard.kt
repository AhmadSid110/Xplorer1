package com.droidexplorer.websim.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ClipboardOperation { COPY, MOVE }

data class ClipboardItem(
    val sourcePath: String,
    val operation: ClipboardOperation
)

object FileClipboard {
    var item by mutableStateOf<ClipboardItem?>(null)
        private set

    fun copy(path: String) {
        item = ClipboardItem(path, ClipboardOperation.COPY)
    }

    fun cut(path: String) {
        item = ClipboardItem(path, ClipboardOperation.MOVE)
    }

    fun clear() {
        item = null
    }

    fun hasItem(): Boolean = item != null
}
