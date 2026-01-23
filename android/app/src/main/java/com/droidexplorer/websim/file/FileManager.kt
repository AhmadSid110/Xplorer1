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
private const val DEFAULT_LARGE_FILE_MIN_BYTES = 100L * 1024 * 1024
private const val DEFAULT_LARGE_FILE_RESULTS = 50

/**
 * ONLY allowed visibility filter: hidden files
 */
private fun List<FsNode>.filterHidden(showHidden: Boolean): List<FsNode> =
    if (showHidden) this else filter { !it.name.startsWith(".") }

object FileManager {

    /**
     * LIST DIRECTORY CONTENTS
     *
     * GUARANTEE:
     * - No filtering by extension / mime / category
     * - TorBox is explicitly excluded (remote, read-only)
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

            val nodes: List<FsNode> =
                if (permissionTarget != null && safPermissionManager != null && context != null) {

                    // SAF listing
                    val uri = safPermissionManager.getOrRequest(permissionTarget)
                    val rootDoc = DocumentFile.fromTreeUri(context, uri)
                    val targetDoc =
                        rootDoc?.let { resolveDocument(it, permissionTarget.absolutePath, path) }

                    targetDoc?.listFiles()?.map { child ->
                        val childPath = File(path, child.name ?: "").absolutePath
                        FsNode.Saf(child, childPath)
                    } ?: emptyList()

                } else {

                    // Local listing (scoped vs full access handled by OS)
                    val files = root.listFiles()
                    val result = files?.map { FsNode.Local(it) } ?: emptyList()

                    if (files != null && files.isNotEmpty()) {
                        Log.d(
                            "FILE_ENUM",
                            "Listed ${files.size} files in $path (hasAllFilesAccess=${hasAllFilesAccess()})"
                        )
                    }

                    result
                }

            sortFiles(nodes.filterHidden(showHidden), sortType, sortOrder)

        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * SEARCH (LOCAL + SAF ONLY)
     * TorBox search is handled elsewhere (API-side)
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

            val results =
                if (permissionTarget != null && safPermissionManager != null && context != null) {

                    val uri = safPermissionManager.getOrRequest(permissionTarget)
                    val rootDoc =
                        DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
                    val targetDoc =
                        resolveDocument(rootDoc, permissionTarget.absolutePath, path)
                            ?: return emptyList()

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

        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * SORT
     */
    fun sortFiles(
        files: List<FsNode>,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<FsNode> {
        val secondaryComparator = when (sortType) {
            SortType.NAME -> compareBy<FsNode> { it.name.lowercase() }
            SortType.SIZE -> compareBy<FsNode> { it.safeSize() }
            SortType.DATE -> compareBy<FsNode> { it.lastModified() }
        }

        val asc = sortOrder == SortOrder.ASC

        val dirs = files.filter { it.isDirectory }
            .sortedWith(if (asc) secondaryComparator else secondaryComparator.reversed())

        val others = files.filter { !it.isDirectory }
            .sortedWith(if (asc) secondaryComparator else secondaryComparator.reversed())

        return dirs + others
    }

    /**
     * SAF TREE SEARCH
     */
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

            if (child.isFile && child.name?.contains(query, true) == true) {
                out.add(FsNode.Saf(child, childPath))
            }

            if (child.isDirectory) {
                searchSafTree(child, query, childPath, out, maxResults)
            }
        }
    }

    /**
     * FILE OPERATIONS
     * TorBox is EXPLICITLY BLOCKED
     */

    fun rename(node: FsNode, newName: String): Result<File> =
        when (node) {

            is FsNode.Local -> renameLocal(node.file, newName)

            is FsNode.Saf ->
                Result.failure(IllegalStateException("Rename via SAF not supported"))

            is FsNode.TorBox ->
                Result.failure(IllegalStateException("TorBox is read-only"))
        }

    private fun renameLocal(file: File, newName: String): Result<File> =
        try {
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) Result.success(newFile)
            else Result.failure(Exception("Rename failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun delete(node: FsNode): Result<Boolean> =
        when (node) {

            is FsNode.Local -> {
                val ok =
                    if (node.file.isDirectory) node.file.deleteRecursively()
                    else node.file.delete()
                Result.success(ok)
            }

            is FsNode.Saf ->
                Result.failure(IllegalStateException("Delete via SAF not supported"))

            is FsNode.TorBox ->
                Result.failure(IllegalStateException("TorBox is read-only"))
        }

    fun paste(
        clipboardItem: ClipboardItem,
        destinationPath: String
    ): Result<File> =
        try {
            val src = File(clipboardItem.sourcePath)
            val dest = File(destinationPath, src.name)

            when (clipboardItem.operation) {
                ClipboardOperation.COPY -> {
                    if (src.isDirectory)
                        src.copyRecursively(dest, overwrite = false)
                    else src.copyTo(dest, overwrite = false)
                }

                ClipboardOperation.MOVE -> {
                    if (src.isDirectory) {
                        src.copyRecursively(dest, overwrite = false)
                        src.deleteRecursively()
                    } else {
                        src.copyTo(dest, overwrite = false)
                        src.delete()
                    }
                }
            }

            Result.success(dest)

        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * CLEANUP UTILITIES
     */

    fun deleteEmptyFolders(rootPath: String): Int {
        var count = 0
        try {
            File(rootPath).walkBottomUp().forEach {
                if (it.isDirectory && it.listFiles()?.isEmpty() == true) {
                    if (it.delete()) count++
                }
            }
        } catch (_: Exception) {}
        return count
    }

    fun clearCache(cachePath: String): Int {
        var count = 0
        try {
            File(cachePath).listFiles()?.forEach {
                if (it.deleteRecursively()) count++
            }
        } catch (_: Exception) {}
        return count
    }

    fun findLargeFiles(
        rootPath: String,
        minSizeBytes: Long = DEFAULT_LARGE_FILE_MIN_BYTES,
        maxResults: Int = DEFAULT_LARGE_FILE_RESULTS
    ): List<File> {
        return runCatching {
            val queue = java.util.PriorityQueue<File>(compareBy { it.length() })
            File(rootPath).walkTopDown().forEach { file ->
                if (!file.isFile) return@forEach
                val size = file.length()
                if (size < minSizeBytes) return@forEach

                if (queue.size < maxResults) {
                    queue.add(file)
                } else {
                    val smallest = queue.peek()
                    if (smallest != null && size > smallest.length()) {
                        queue.poll()
                        queue.add(file)
                    }
                }
            }
            queue.sortedByDescending { it.length() }
        }.getOrElse { emptyList() }
    }

    /**
     * SAF HELPERS
     */

    private fun safAnchor(path: String): File? =
        when {
            path.contains("/Android/data") ->
                File(Environment.getExternalStorageDirectory(), "Android/data")

            path.contains("/Android/obb") ->
                File(Environment.getExternalStorageDirectory(), "Android/obb")

            else -> null
        }

    private fun resolveDocument(
        anchor: DocumentFile,
        anchorPath: String,
        targetPath: String
    ): DocumentFile? {

        if (anchorPath == targetPath) return anchor

        val relative =
            targetPath.removePrefix(anchorPath).trimStart(File.separatorChar)

        var current = anchor
        for (segment in relative.split(File.separatorChar)) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }
}