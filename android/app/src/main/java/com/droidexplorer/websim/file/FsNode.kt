package com.droidexplorer.websim.file

import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val UNKNOWN_NAME = "Unknown"

sealed class FsNode {
    abstract val name: String
    abstract val isDirectory: Boolean
    abstract val uniqueKey: String
    abstract val path: String
    abstract val size: Long?

    data class Local(val file: File) : FsNode() {
        override val name: String = file.name
        override val isDirectory: Boolean = file.isDirectory
        override val uniqueKey: String = file.absolutePath
        override val path: String = file.absolutePath
        override val size: Long? = if (file.isFile) file.length() else null
    }

    data class Saf(
        val document: DocumentFile,
        override val path: String
    ) : FsNode() {
        override val name: String = document.name?.takeIf { it.isNotBlank() }
            ?: document.uri.lastPathSegment ?: UNKNOWN_NAME
        override val isDirectory: Boolean = document.isDirectory
        override val uniqueKey: String = document.uri.toString()
        override val size: Long? = if (document.isFile) document.length() else null
    }
    
    /**
     * Remote file from TorBox.
     * Read-only, no local File or SAF operations allowed.
     */
    data class TorBox(
        val id: String,
        override val name: String,
        override val size: Long?,
        val downloadUrl: String
    ) : FsNode() {
        override val isDirectory: Boolean = false
        override val uniqueKey: String = "torbox://$id"
        override val path: String = "torbox://$id"
    }
}

/**
 * Returns a file-system path representation. For SAF nodes this path is used for permission
 * negotiation and may require SAF-backed access for actual I/O.
 * For TorBox nodes, this throws an exception as they have no local file representation.
 */
fun FsNode.asFile(): File = when (this) {
    is FsNode.Local -> file
    is FsNode.Saf -> File(path)
    is FsNode.TorBox -> error("TorBox files have no local File representation")
}

fun FsNode.size(): Long = size ?: 0L

fun FsNode.lastModified(): Long = when (this) {
    is FsNode.Local -> file.lastModified()
    is FsNode.Saf -> document.lastModified()
    is FsNode.TorBox -> 0L // No modification time available for remote files
}
