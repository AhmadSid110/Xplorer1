package com.droidexplorer.websim.ui.viewer

import java.io.File

sealed class Viewer {
    data class Pdf(val file: File) : Viewer()
    data class Zip(val file: File) : Viewer()
    data class Image(val file: File, val items: List<File> = emptyList(), val index: Int = 0) : Viewer()
    data class Text(val file: File, val showLineNumbers: Boolean = true) : Viewer()
    data class Code(val file: File, val language: String) : Viewer()
}
