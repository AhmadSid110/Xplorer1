package com.droidexplorer.websim.file

import com.droidexplorer.websim.data.ClipboardItem
import com.droidexplorer.websim.data.ClipboardOperation
import java.io.File

enum class SortType { NAME, SIZE, DATE }
enum class SortOrder { ASC, DESC }

object FileManager {
    fun list(path: String, sortType: SortType = SortType.NAME, sortOrder: SortOrder = SortOrder.ASC): List<File> {
        return try {
            val files = File(path).listFiles()?.toList() ?: emptyList()
            val sorted = when (sortType) {
                SortType.NAME -> files.sortedBy { it.name.lowercase() }
                SortType.SIZE -> files.sortedBy { it.length() }
                SortType.DATE -> files.sortedBy { it.lastModified() }
            }
            if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun search(path: String, query: String): List<File> {
        return try {
            File(path).listFiles()?.filter {
                it.name.contains(query, ignoreCase = true)
            }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun rename(file: File, newName: String): Result<File> {
        return try {
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
                Result.success(newFile)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun delete(file: File): Result<Boolean> {
        return try {
            val success = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun paste(clipboardItem: ClipboardItem, destinationPath: String): Result<File> {
        return try {
            val sourceFile = File(clipboardItem.sourcePath)
            val destFile = File(destinationPath, sourceFile.name)
            
            when (clipboardItem.operation) {
                ClipboardOperation.COPY -> {
                    if (sourceFile.isDirectory) {
                        sourceFile.copyRecursively(destFile, overwrite = false)
                    } else {
                        sourceFile.copyTo(destFile, overwrite = false)
                    }
                    Result.success(destFile)
                }
                ClipboardOperation.MOVE -> {
                    if (sourceFile.isDirectory) {
                        sourceFile.copyRecursively(destFile, overwrite = false)
                        sourceFile.deleteRecursively()
                    } else {
                        sourceFile.copyTo(destFile, overwrite = false)
                        sourceFile.delete()
                    }
                    Result.success(destFile)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Protected directories that should never be cleaned
    private val PROTECTED_PATHS = listOf(
        "/Android/obb",
        "/Android/data",
        "/DCIM",
        "/Pictures",
        "/Download",
        "/Documents",
        "/Music",
        "/Movies"
    )
    
    private fun isProtectedPath(path: String): Boolean {
        return PROTECTED_PATHS.any { protected -> 
            path.contains(protected, ignoreCase = true) 
        }
    }

    fun deleteEmptyFolders(rootPath: String): Int {
        var count = 0
        try {
            val root = File(rootPath)
            if (root.isDirectory) {
                root.walkBottomUp().forEach { file ->
                    if (file.isDirectory && file.listFiles()?.isEmpty() == true) {
                        // Skip protected directories
                        if (!isProtectedPath(file.absolutePath)) {
                            if (file.delete()) count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        return count
    }

    fun clearCache(cachePath: String): Int {
        var count = 0
        try {
            val cacheDir = File(cachePath)
            if (cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.deleteRecursively()) count++
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        return count
    }
}
