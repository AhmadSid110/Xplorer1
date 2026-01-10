package com.droidexplorer.websim.ui.debug

import com.droidexplorer.websim.file.FsNode

data class DebugOverlayState(
    val enabled: Boolean = false,
    val currentPath: String = "",
    val torBoxClientPresent: Boolean = false,
    val fileCount: Int = 0,
    val torBoxFileCount: Int = 0,
    val localFileCount: Int = 0,
    val lastTrigger: String = "",
    val filesSnapshot: List<FsNode> = emptyList()
)
