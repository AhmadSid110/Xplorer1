package com.droidexplorer.websim.ui.debug

data class DebugOverlayState(
    val visible: Boolean = true,
    val path: String = "",
    val torBoxClientPresent: Boolean = false,
    val totalFiles: Int = 0,
    val torBoxFiles: Int = 0,
    val localFiles: Int = 0,
    val lastTrigger: String = "",
    val rawTorBoxResponse: String = ""
)
