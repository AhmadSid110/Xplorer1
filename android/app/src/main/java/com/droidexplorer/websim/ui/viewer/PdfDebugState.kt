package com.droidexplorer.websim.ui.viewer

import androidx.compose.runtime.Stable

@Stable
data class PdfDebugState(
    val pageIndex: Int = -1,
    val pageCount: Int = -1,
    val bitmapInfo: String = "",
    val pageOpened: Boolean = false,
    val pageDisposed: Boolean = false,
    val renderThread: String = "",
    val openThread: String = "",
    val lastEvent: String = ""
)
