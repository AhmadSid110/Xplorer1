package com.droidexplorer.websim.search

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.droidexplorer.websim.file.FsNode
import java.io.File

private const val DEFAULT_MAX_SEARCH_RESULTS = 500

class FileSearcher(
    private val context: Context
) {

    fun searchLocal(
        path: String,
        query: String,
        maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS
    ): List<FsNode> {
        return runCatching {
            File(path)
                .walkTopDown()
                .filter { it.isFile && it.name.contains(query, ignoreCase = true) }
                .take(maxResults)
                .map { FsNode.Local(it) }
                .toList()
        }.getOrElse { emptyList() }
    }

    fun searchSaf(
        rootId: String,
        query: String,
        maxResults: Int = DEFAULT_MAX_SEARCH_RESULTS
    ): List<FsNode>? {
        return runCatching {
            val uri = Uri.parse(rootId)
            val hasPermission = context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission
            }
            if (!hasPermission) return@runCatching null

            val root = DocumentFile.fromTreeUri(context, uri) ?: return@runCatching null
            val basePath = resolveBasePath(uri) ?: root.uri.path.orEmpty()
            val hits = mutableListOf<FsNode>()
            searchSafTree(root, query, basePath, hits, maxResults)
            hits
        }.getOrNull()
    }

    private fun searchSafTree(
        root: DocumentFile,
        query: String,
        currentPath: String,
        out: MutableList<FsNode>,
        maxResults: Int
    ) {
        if (out.size >= maxResults) return
        root.listFiles().forEach { child ->
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

    private fun resolveBasePath(uri: Uri): String? {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
        val relativePath = docId.substringAfter(':', "")
        return if (relativePath.isNotEmpty()) {
            "/storage/emulated/0/$relativePath"
        } else {
            "/storage/emulated/0"
        }
    }
}
