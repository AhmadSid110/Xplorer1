package com.droidexplorer.websim.ui.debug

data class DebugOverlayState(
    val visible: Boolean = true,
    val path: String = "",
    val torBoxClientPresent: Boolean = false
)
