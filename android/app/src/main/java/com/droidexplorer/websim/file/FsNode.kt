package com.droidexplorer.websim.file

import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val UNKNOWN_NAME = "Unknown"

/**
 * Unified filesystem node abstraction.
 *
 * RULES:
 * - Local  → real filesystem (java.io.File)
 * - SAF    → Storage Access Framework
 * - TorBox → REMOTE, READ-ONLY, VIRTUAL (NO FILE IO, NO NAVIGATION)
 */
sealed class FsNode {

    abstract val name: String
    abstract val isDirectory: Boolean
    abstract val uniqueKey: String
    abstract val path: String
    abstract val size: Long?

    /* ─────────────────────────────────────────────
     * LOCAL FILESYSTEM
     * ───────────────────────────────────────────── */

    data class Local(
        val file: File
    ) : FsNode() {

        override val name: String = file.name
        override val isDirectory: Boolean = file.isDirectory
        override val uniqueKey: String = "local:${file.absolutePath}"
        override val path: String = file.absolutePath
        override val size: Long? = if (file.isFile) file.length() else null
    }

    /* ─────────────────────────────────────────────
     * STORAGE ACCESS FRAMEWORK (SAF)
     * ───────────────────────────────────────────── */

    data class Saf(
        val document: DocumentFile,
        override val path: String
    ) : FsNode() {

        override val name: String =
            document.name?.takeIf { it.isNotBlank() }
                ?: document.uri.lastPathSegment
                ?: UNKNOWN_NAME

        override val isDirectory: Boolean = document.isDirectory
        override val uniqueKey: String = "saf:${document.uri}"
        override val size: Long? = if (document.isFile) document.length() else null
    }

    /* ─────────────────────────────────────────────
     * TORBOX (REMOTE, READ-ONLY)
     * ───────────────────────────────────────────── */

    /**
     * TorBox remote file.
     *
     * CRITICAL DESIGN:
     * - id is STRING (API accurate)
     * - NOT a directory
     * - UNIQUE path per file
     * - NO filesystem navigation
     * - NO java.io.File usage
     */
    data class TorBox(
        val id: String,
        override val name: String,
        override val size: Long?,
        val absolutePath: String
    ) : FsNode() {

        override val isDirectory: Boolean = false

        // Compose-stable unique identity
        override val uniqueKey: String = "torbox:file:$id"

        // Virtual path (MUST be unique per file)
        override val path: String = "torbox:file:$id"
    }
}

/* ─────────────────────────────────────────────
 * EXTENSIONS
 * ───────────────────────────────────────────── */

/**
 * Convert FsNode to java.io.File.
 *
 * WARNING:
 * - TorBox has NO local file representation.
 */
fun FsNode.asFile(): File = when (this) {
    is FsNode.Local -> file
    is FsNode.Saf -> File(path)
    is FsNode.TorBox ->
        error("TorBox files have no local File representation")
}

/**
 * Safe size accessor.
 */
fun FsNode.safeSize(): Long = size ?: 0L

/**
 * Last-modified timestamp.
 */
fun FsNode.lastModified(): Long = when (this) {
    is FsNode.Local -> file.lastModified()
    is FsNode.Saf -> document.lastModified()
    is FsNode.TorBox -> 0L
}