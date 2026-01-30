package com.droidexplorer.websim.file

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.droidexplorer.websim.data.ClipboardItem
import com.droidexplorer.websim.data.ClipboardOperation
import com.droidexplorer.websim.storage.SafPermissionManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class FileOperator(
    private val context: Context,
    private val saf: SafPermissionManager
) {

    fun copy(src: File, destDir: File): Result<File> {
        val dest = File(destDir, src.name)
        return runCatching {
            copyInternal(src, dest, destDir)
            dest
        }
    }

    fun move(src: File, destDir: File): Result<File> {
        val dest = File(destDir, src.name)
        return runCatching {
            copyInternal(src, dest, destDir)
            deleteInternal(src)
            dest
        }
    }

    fun delete(target: File): Result<Boolean> = runCatching {
        deleteInternal(target)
        true
    }

    fun rename(target: File, newName: String): Result<File> = runCatching {
        val renamed = File(target.parentFile, newName)
        try {
            if (target.renameTo(renamed)) {
                renamed
            } else {
                throw FileWriteFailed("Failed to rename file")
            }
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(target.parentFile ?: target)
                val parentDocument = DocumentFile.fromTreeUri(context, uri)
                    ?: throw SafRequired(target.parentFile ?: target)
                val doc = parentDocument.findFile(target.name)
                    ?: throw SafRequired(target.parentFile ?: target)
                if (doc.renameTo(newName)) {
                    renamed
                } else {
                    throw FileWriteFailed("Failed to rename file with SAF", e)
                }
            } else {
                throw e
            }
        }
    }

    fun readText(file: File): Result<String> = runCatching {
        try {
            file.readText()
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(file.parentFile ?: file)
                val doc = findDocumentFile(uri, file) ?: throw SafRequired(file.parentFile ?: file)
                context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw FileWriteFailed("Unable to read via SAF")
            } else {
                throw e
            }
        }
    }

    fun writeText(file: File, content: String): Result<Unit> = runCatching {
        try {
            file.writeText(content)
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(file.parentFile ?: file)
                val dirDoc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw SafRequired(file.parentFile ?: file)
                val doc = dirDoc.findFile(file.name) ?: dirDoc.createFile("text/plain", file.name)
                if (doc == null) throw FileWriteFailed("Unable to create document via SAF", e)
                context.contentResolver.openOutputStream(doc.uri, "wt")?.use { stream ->
                    stream.write(content.toByteArray())
                } ?: throw FileWriteFailed("Unable to write via SAF")
            } else {
                throw e
            }
        }
    }

    fun createFile(parentDir: File, name: String): Result<File> = runCatching {
        val newFile = File(parentDir, name)
        try {
            if (newFile.createNewFile()) {
                newFile
            } else {
                throw FileWriteFailed("File already exists")
            }
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(parentDir)
                val dirDoc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw SafRequired(parentDir)
                val ext = name.substringAfterLast('.', "")
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "text/plain"
                dirDoc.createFile(mime, name)
                    ?: throw FileWriteFailed("Unable to create file via SAF", e)
                newFile
            } else {
                throw e
            }
        }
    }

    fun createDirectory(parentDir: File, name: String): Result<File> = runCatching {
        val newDir = File(parentDir, name)
        try {
            if (newDir.mkdir()) {
                newDir
            } else {
                throw FileWriteFailed("Failed to create directory")
            }
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(parentDir)
                val dirDoc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw SafRequired(parentDir)
                dirDoc.createDirectory(name)
                    ?: throw FileWriteFailed("Unable to create directory via SAF", e)
                newDir
            } else {
                throw e
            }
        }
    }

    fun openDescriptor(
        file: File,
        mode: Int = ParcelFileDescriptor.MODE_READ_ONLY
    ): Result<ParcelFileDescriptor> = runCatching {
        try {
            ParcelFileDescriptor.open(file, mode)
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(file.parentFile ?: file)
                val doc = findDocumentFile(uri, file) ?: throw SafRequired(file.parentFile ?: file)
                context.contentResolver.openFileDescriptor(doc.uri, "r")
                    ?: throw FileWriteFailed("Unable to open descriptor via SAF", e)
            } else {
                throw e
            }
        }
    }

    fun performClipboard(clipboardItem: ClipboardItem, destination: File): Result<File> {
        return when (clipboardItem.operation) {
            ClipboardOperation.COPY -> copy(File(clipboardItem.sourcePaths.first()), destination)
            ClipboardOperation.MOVE -> move(File(clipboardItem.sourcePaths.first()), destination)
        }
    }

    private fun copyInternal(src: File, dest: File, destDir: File) {
        try {
            if (src.isDirectory) {
                src.copyRecursively(dest, overwrite = false)
            } else {
                src.copyTo(dest, overwrite = false)
            }
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(destDir)
                copyViaSaf(src, destDir, uri)
            } else {
                throw e
            }
        }
    }

    private fun copyViaSaf(src: File, destDir: File, uri: Uri) {
        val documentDir = DocumentFile.fromTreeUri(context, uri)
            ?: throw SafRequired(destDir)
        copyViaSaf(src, destDir, documentDir)
    }

    private fun copyViaSaf(src: File, destDir: File, documentDir: DocumentFile) {
        if (src.isDirectory) {
            val targetDir = documentDir.findFile(src.name) ?: documentDir.createDirectory(src.name)
            val destDocument = targetDir ?: throw FileWriteFailed("Unable to create directory via SAF")
            src.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    copyViaSaf(child, File(destDir, child.name), destDocument)
                } else {
                    copyFileToDocument(child, destDocument)
                }
            }
        } else {
            copyFileToDocument(src, documentDir)
        }
    }

    private fun copyFileToDocument(src: File, parentDoc: DocumentFile) {
        val targetDoc = parentDoc.findFile(src.name) ?: parentDoc.createFile(
            "application/octet-stream",
            src.name
        )
        if (targetDoc == null) throw FileWriteFailed("Unable to create file via SAF")
        src.inputStream().use { input ->
            streamCopy(input, context.contentResolver.openOutputStream(targetDoc.uri))
        }
    }

    private fun deleteInternal(target: File) {
        try {
            if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
        } catch (e: Exception) {
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(target.parentFile ?: target)
                val doc = findDocumentFile(uri, target) ?: throw SafRequired(target.parentFile ?: target)
                if (!doc.delete()) throw FileWriteFailed("Failed to delete via SAF", e)
            } else {
                throw e
            }
        }
    }

    private fun findDocumentFile(treeUri: Uri, target: File): DocumentFile? {
        val base = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return base.findFile(target.name)
    }

    private fun streamCopy(input: InputStream, output: OutputStream?) {
        if (output == null) throw FileWriteFailed("Unable to open output stream")
        output.use { outputStream ->
            input.copyTo(outputStream)
        }
    }

    companion object {
        fun canWrite(dir: File): Boolean {
            if (dir.canWrite()) return true
            val probe = File(dir, ".probe_${java.util.UUID.randomUUID()}")
            return try {
                probe.createNewFile()
            } catch (e: Exception) {
                Log.w("FileOperator", "Write probe failed for ${dir.absolutePath}", e)
                false
            } finally {
                if (probe.exists()) probe.delete()
            }
        }
    }
}
