package com.droidexplorer.websim.ui.viewer

import java.io.File

sealed class Viewer {
    data class Pdf(val file: File) : Viewer()
    data class Zip(val file: File) : Viewer()
}
