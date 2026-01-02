package com.droidexplorer.websim.file

import java.io.File

sealed class FileAction {
    data class Copy(val source: File, val destinationDir: File) : FileAction()
    data class Move(val source: File, val destinationDir: File) : FileAction()
    data class Delete(val target: File) : FileAction()
    data class Rename(val target: File, val newName: String) : FileAction()
}
