package com.droidexplorer.websim.ui.permission

import com.droidexplorer.websim.storage.SafPermissionManager
import java.io.File

class SafRecoveryFlow(
    private val safPermissionManager: SafPermissionManager
) {
    private var lastPromptedPath: String? = null

    /**
     * Detect all tracked directories whose persisted permissions have been revoked.
     */
    fun detectRevoked(trackedPaths: List<File>): List<File> {
        if (trackedPaths.isEmpty()) return emptyList()
        return trackedPaths.filter { dir -> safPermissionManager.isRevoked(dir) }
    }

    /**
     * Build a state object suitable for showing a recovery dialog without causing loops.
     */
    fun buildState(trackedPaths: List<File>): SafRecoveryState {
        val revoked = detectRevoked(trackedPaths)
        val target = revoked.firstOrNull()
        val shouldPrompt = target != null && target.absolutePath != lastPromptedPath
        if (shouldPrompt) {
            lastPromptedPath = target?.absolutePath
        }
        return SafRecoveryState(
            revokedPath = target,
            revoked = revoked,
            showDialog = shouldPrompt
        )
    }

    fun markHandled() {
        lastPromptedPath = null
    }
}

data class SafRecoveryState(
    val revokedPath: File?,
    val revoked: List<File>,
    val showDialog: Boolean
)
