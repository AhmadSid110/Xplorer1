package com.droidexplorer.websim.file

import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val UNKNOWN_NAME = "Unknown"

sealed class FsNode {
    abstract val name: String
    abstract val isDirectory: Boolean
    abstract val uniqueKey: String
    abstract val path: String

    data class Local(val file: File) : FsNode() {
        override val name: String = file.name
        override val isDirectory: Boolean = file.isDirectory
        override val uniqueKey: String = file.absolutePath
        override val path: String = file.absolutePath
    }

    data class Saf(
        val document: DocumentFile,
        override val path: String
    ) : FsNode() {
        override val name: String = document.name?.takeIf { it.isNotBlank() }
            ?: document.uri.lastPathSegment ?: UNKNOWN_NAME
        override val isDirectory: Boolean = document.isDirectory
        override val uniqueKey: String = document.uri.toString()
    }
}

/**
 * Returns a file-system path representation. For SAF nodes this path is used for permission
 * negotiation and may require SAF-backed access for actual I/O.
 */
fun FsNode.asFile(): File = when (this) {
    is FsNode.Local -> file
    is FsNode.Saf -> File(path)
}

fun FsNode.size(): Long = when (this) {
    is FsNode.Local -> file.length()
    is FsNode.Saf -> document.length()
}

fun FsNode.lastModified(): Long = when (this) {
    is FsNode.Local -> file.lastModified()
    is FsNode.Saf -> document.lastModified()
}
