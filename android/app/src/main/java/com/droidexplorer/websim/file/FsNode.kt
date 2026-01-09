package com.droidexplorer.websim.file

import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val UNKNOWN_NAME = "Unknown"

/**
 * Unified filesystem node abstraction.
 *
 * IMPORTANT RULES:
 * - FsNode.Local  → real filesystem
 * - FsNode.Saf    → SAF-backed filesystem
 * - FsNode.TorBox → REMOTE, VIRTUAL, READ-ONLY (NO FILE IO, NO NAVIGATION)
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

    data class Local(val file: File) : FsNode() {
        override val name: String = file.name
        override val isDirectory: Boolean = file.isDirectory
        override val uniqueKey: String = file.absolutePath
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
        override val uniqueKey: String = document.uri.toString()
        override val size: Long? = if (document.isFile) document.length() else null
    }

    /* ─────────────────────────────────────────────
     * TORBOX (REMOTE, READ-ONLY)
     * ───────────────────────────────────────────── */

    /**
     * Remote file from TorBox.
     *
     * CRITICAL DESIGN DECISIONS:
     * - id is STRING (matches API)
     * - path is ALWAYS "torbox:" (flat virtual root)
     * - uniqueKey is stable for Compose
     * - NEVER treated as directory
     * - NO filesystem navigation
     */
    data class TorBox(
        val id: String,
        override val name: String,
        override val size: Long?,
        val absolutePath: String
    ) : FsNode() {

        override val isDirectory: Boolean = false

        // Stable + Compose-safe
        override val uniqueKey: String = "torbox:file:$id"

        // Virtual root (DO NOT CHANGE)
        override val path: String = "torbox:"
    }
}

/* ─────────────────────────────────────────────
 * EXTENSIONS
 * ───────────────────────────────────────────── */

/**
 * Convert FsNode to java.io.File.
 *
 * NOTE:
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
fun FsNode.size(): Long = size ?: 0L

/**
 * Last-modified timestamp.
 */
fun FsNode.lastModified(): Long = when (this) {
    is FsNode.Local -> file.lastModified()
    is FsNode.Saf -> document.lastModified()
    is FsNode.TorBox -> 0L // Remote files have no local timestamp
}