package com.droidexplorer.websim.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.droidexplorer.websim.file.SafRequired
import java.io.File

class SafPermissionManager(
    private val context: Context,
    private val store: SafStore
) {
    @Volatile
    private var cachedHasAnyPermission: Boolean? = null
    fun getOrRequest(dir: File): Uri {
        return store.get(dir.absolutePath) ?: throw SafRequired(dir)
    }

    fun has(dir: File): Boolean = store.get(dir.absolutePath) != null

    fun hasAnyPermission(): Boolean {
        cachedHasAnyPermission?.let { return it }
        val hasPermission = context.contentResolver.persistedUriPermissions.any { it.isReadPermission || it.isWritePermission }
        cachedHasAnyPermission = hasPermission
        return hasPermission
    }

    fun persist(uri: Uri, path: String) {
        persist(uri)
        store.put(path, uri.toString())
    }

    /**
     * Persist a SAF permission when only a URI is available (e.g., generic search toggles).
     * This does not store a file-system path mapping; use [persist] with the path parameter when a target directory is known.
     */
    fun persist(uri: Uri) {
        takePersistable(uri)
    }

    private fun takePersistable(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun isPersisted(dir: File): Boolean {
        val uri = store.get(dir.absolutePath) ?: return false
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    fun isRevoked(dir: File): Boolean = has(dir) && !isPersisted(dir)
}
