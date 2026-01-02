package com.droidexplorer.websim.file

import java.io.File

object FileManager {
    fun list(path: String): List<File> {
        return try {
            File(path).listFiles()?.toList()?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
