package com.droidexplorer.websim.core.automation

import android.os.Environment
import java.io.File

data class FileDescriptor(
    val path: String,
    val name: String,
    val extension: String,
    val parentPath: String
) {
    val nameWithoutExtension: String = name.substringBeforeLast('.', name)

    companion object {
        fun from(file: File): FileDescriptor = FileDescriptor(
            path = file.absolutePath,
            name = file.name,
            extension = file.extension,
            parentPath = file.parent ?: ""
        )
    }
}

interface FileOperations {
    suspend fun listFiles(directory: String): List<FileDescriptor>
    suspend fun move(file: FileDescriptor, destDir: String)
}

object AutoMoveSubtitle {

    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm")

    suspend fun onFileCreated(
        file: FileDescriptor,
        ops: FileOperations
    ) {
        if (!isSubtitle(file)) return

        val subtitleBase = normalize(file.nameWithoutExtension)

        // 1️⃣ Check same directory first (fastest + safest)
        findMatchingVideo(
            ops = ops,
            directory = file.parentPath,
            subtitleBase = subtitleBase
        )?.let { video ->
            ops.move(file, video.parentPath)
            return
        }

        // 2️⃣ Optional fallback: common video dirs (VERY LIMITED)
        val fallbackDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
            File(Environment.getExternalStorageDirectory(), "Video").absolutePath
        )

        for (dir in fallbackDirs) {
            findMatchingVideo(
                ops = ops,
                directory = dir,
                subtitleBase = subtitleBase
            )?.let { video ->
                ops.move(file, video.parentPath)
                return
            }
        }
    }

    // ---------- helpers ----------

    private fun isSubtitle(file: FileDescriptor): Boolean =
        file.extension.equals("srt", ignoreCase = true)

    private suspend fun findMatchingVideo(
        ops: FileOperations,
        directory: String,
        subtitleBase: String
    ): FileDescriptor? {
        return ops.listFiles(directory)
            .asSequence()
            .filter { it.extension.lowercase() in VIDEO_EXTENSIONS }
            .firstOrNull {
                normalize(it.nameWithoutExtension)
                    .contains(subtitleBase)
            }
    }

    private fun normalize(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
}
