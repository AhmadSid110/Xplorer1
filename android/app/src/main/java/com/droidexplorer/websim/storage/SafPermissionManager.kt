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
    fun getOrRequest(dir: File): Uri {
        return store.get(dir.absolutePath) ?: throw SafRequired(dir)
    }

    fun has(dir: File): Boolean = store.get(dir.absolutePath) != null

    fun persist(uri: Uri, path: String) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        store.put(path, uri.toString())
    }
}
