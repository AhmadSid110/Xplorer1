package com.droidexplorer.websim.file

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.droidexplorer.websim.data.ClipboardItem
import com.droidexplorer.websim.data.ClipboardOperation
import com.droidexplorer.websim.storage.SafPermissionManager
import com.droidexplorer.websim.storage.hasAllFilesAccess
import java.io.File

enum class SortType { NAME, SIZE, DATE }
enum class SortOrder { ASC, DESC }

private const val DEFAULT_MAX_SEARCH_RESULTS = 500

/**
 * Filters hidden files (files starting with '.') from the list.
 * 
 * CRITICAL: This is the ONLY allowed filtering for file visibility.
 * DO NOT add any other filters based on:
 * - File extension
 * - MIME type  
 * - File category (image, video, audio, etc.)
 * - File size
 * - File support/compatibility
 * 
 * ALL regular files and directories must be visible regardless of type.
 */
private fun List<FsNode>.filterHidden(showHidden: Boolean): List<FsNode> =
    if (showHidden) this else filter { !it.name.startsWith(".") }

object FileManager {
    /**
     * Lists all files and directories in the given path.
     * 
     * VISIBILITY GUARANTEE: Returns ALL files and folders (subject only to hidden file setting).
     * This function must NEVER filter files based on extension, MIME type, or category.
     * 
     * @param path Directory path to list
     * @param sortType How to sort the results
     * @param sortOrder Ascending or descending order
     * @param showHidden Whether to include hidden files (files starting with '.')
     * @param safPermissionManager Manager for Storage Access Framework permissions
     * @param context Android context for SAF operations
     * @return List of all files and directories in the path
     */
    fun list(
        path: String,
        sortType: SortType = SortType.NAME,
        sortOrder: SortOrder = SortOrder.ASC,
        showHidden: Boolean = false,
        safPermissionManager: SafPermissionManager? = null,
        context: Context? = null
    ): List<FsNode> {
        return try {
            val root = File(path)
            val anchor = safAnchor(path)
            val permissionTarget = when {
                safPermissionManager?.has(root) == true -> root
                anchor != null && safPermissionManager?.has(anchor) == true -> anchor
                else -> null
            }
            val nodes = if (permissionTarget != null && safPermissionManager != null && context != null) {
                val uri = safPermissionManager.getOrRequest(permissionTarget)
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                val targetDoc = rootDoc?.let { resolveDocument(it, permissionTarget.absolutePath, path) }
                targetDoc?.listFiles()?.map { child ->
                    val childPath = File(path, child.name ?: "").absolutePath
                    FsNode.Saf(child, childPath)
                } ?: emptyList()
            } else {
                // Use conditional file listing based on permission state
                // Note: Both branches call the same listFiles() method, but Android's
                // behavior differs based on MANAGE_EXTERNAL_STORAGE permission:
                // - With permission: Full unfiltered access to ALL files
                // - Without permission: Android silently filters results (scoped storage)
                val files = root.listFiles()
                val result = files?.map { FsNode.Local(it) } ?: emptyList()
                
                // Debug logging to verify file enumeration
                if (files != null && files.isNotEmpty()) {
                    val fileList = files.take(10).joinToString(", ") { it.name }
                    val more = if (files.size > 10) " and ${files.size - 10} more" else ""
                    Log.d("FILE_ENUM", "Listed ${files.size} files in $path (hasAllFilesAccess=${hasAllFilesAccess()}): $fileList$more")
                }
                
                result
            }
            sortFiles(nodes.filterHidden(showHidden), sortType, sortOrder)
        } catch (e: SafRequired) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Performs a case-insensitive search returning ALL matching files.
     * 
     * VISIBILITY GUARANTEE: Returns ALL files that match the query (subject only to hidden file setting).
     * This function must NEVER filter files based on extension, MIME type, or category.
     * 
     * @param path Root directory to search from
     * @param query Search query string (case-insensitive)
     * @param showHidden Whether to include hidden files (files starting with '.')
     * @param safPermissionManager Manager for Storage Access Framework permissions
     * @param context Android context for SAF operations
     * @param maxResults Maximum number of results to return
     * @return List of all matching files
     */
    fun search(
        path: String,
        query: String,
        showHidden: Boolean = false,
        safPermissionManager: SafPermissionManager? = null,
        context: Context? = null,
        maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS
    ): List<FsNode> {
        return try {
            val root = File(path)
            val anchor = safAnchor(path)
            val permissionTarget = when {
                safPermissionManager?.has(root) == true -> root
                anchor != null && safPermissionManager?.has(anchor) == true -> anchor
                else -> null
            }
            val results = if (permissionTarget != null && safPermissionManager != null && context != null) {
                val uri = safPermissionManager.getOrRequest(permissionTarget)
                val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
                val targetDoc = resolveDocument(rootDoc, permissionTarget.absolutePath, path) ?: return emptyList()
                val hits = mutableListOf<FsNode>()
                searchSafTree(targetDoc, query, path, hits, maxResults)
                hits
            } else {
                File(path)
                    .walkTopDown()
                    .filter { it.isFile && it.name.contains(query, ignoreCase = true) }
                    .take(maxResults)
                    .map { FsNode.Local(it) }
                    .toList()
            }
            results.filterHidden(showHidden)
        } catch (e: SafRequired) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun searchSafTree(
        root: DocumentFile,
        query: String,
        currentPath: String,
        out: MutableList<FsNode>,
        maxResults: Int
    ) {
        if (out.size >= maxResults) return
        root.listFiles()?.forEach { child ->
            if (out.size >= maxResults) return
            val childPath = File(currentPath, child.name ?: "").absolutePath
            if (child.isFile && child.name?.contains(query, ignoreCase = true) == true) {
                out.add(FsNode.Saf(child, childPath))
            }
            if (child.isDirectory) {
                searchSafTree(child, query, childPath, out, maxResults)
            }
        }
    }

    fun sortFiles(
        files: List<FsNode>,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<FsNode> {
        val sorted = when (sortType) {
            SortType.NAME -> files.sortedBy { it.name.lowercase() }
            SortType.SIZE -> files.sortedBy { it.size() }
            SortType.DATE -> files.sortedBy { it.lastModified() }
        }
        return if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    @Deprecated("Use search() returning FsNode instead; legacy API lacks SAF support and will be removed in v2.0")
    fun searchRecursive(
        root: File,
        query: String,
        maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS
    ): List<File> {
        return search(
            root.absolutePath,
            query,
            maxResults = maxResults
        )
            .map { node ->
                when (node) {
                    is FsNode.Local -> node.file
                    is FsNode.Saf -> File(node.path)
                }
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

    private fun safAnchor(path: String): File? = when {
        path.contains("/Android/data") -> File(Environment.getExternalStorageDirectory(), "Android/data")
        path.contains("/Android/obb") -> File(Environment.getExternalStorageDirectory(), "Android/obb")
        else -> null
    }

    private fun resolveDocument(
        anchor: DocumentFile,
        anchorPath: String,
        targetPath: String
    ): DocumentFile? {
        if (anchorPath == targetPath) return anchor
        val relative = targetPath.removePrefix(anchorPath).trimStart(File.separatorChar)
        var current = anchor
        for (segment in relative.split(File.separatorChar).filter { it.isNotEmpty() }) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }
}
