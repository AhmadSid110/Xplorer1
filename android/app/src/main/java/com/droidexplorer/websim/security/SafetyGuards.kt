package com.droidexplorer.websim.security

import com.droidexplorer.websim.core.ops.FileOperation
import java.io.File

object SafetyGuards {
    data class GuardResult(val allowed: Boolean, val reason: String? = null)

    fun validate(operation: FileOperation): GuardResult {
        return when (operation) {
            is FileOperation.Copy -> guardPaths(operation.source.asFile(), operation.destinationDir.asFile())
            is FileOperation.Move -> guardPaths(operation.source.asFile(), operation.destinationDir.asFile())
            is FileOperation.Delete -> guardTarget(operation.target.asFile())
            is FileOperation.Rename -> guardTarget(operation.target.asFile())
        }
    }

    private fun guardPaths(source: File, destinationDir: File): GuardResult {
        if (isSymlink(source) || isSymlink(destinationDir)) {
            return GuardResult(false, "Symlink target blocked")
        }
        return GuardResult(true)
    }

    private fun guardTarget(target: File): GuardResult {
        if (isCriticalRoot(target)) {
            return GuardResult(false, "Refusing to operate on system root")
        }
        if (isSymlink(target)) {
            return GuardResult(false, "Refusing to operate on symlink")
        }
        return GuardResult(true)
    }

    private fun isCriticalRoot(file: File): Boolean {
        val normalized = file.absolutePath
        return normalized == "/" || normalized.equals("/storage", ignoreCase = true)
    }

    private fun isSymlink(file: File): Boolean {
        return try {
            file.canonicalPath != file.absolutePath
        } catch (_: Exception) {
            false
        }
    }
}
