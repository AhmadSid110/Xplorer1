package com.droidexplorer.websim.file

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
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
            ClipboardOperation.COPY -> copy(File(clipboardItem.sourcePath), destination)
            ClipboardOperation.MOVE -> move(File(clipboardItem.sourcePath), destination)
        }
    }

    private fun copyInternal(src: File, dest: File, destDir: File) {
        if (dest.exists()) {
            throw FileWriteFailed("Destination already exists")
        }
        val partial = File(dest.parentFile ?: destDir, "${dest.name}.partial")
        if (partial.exists()) partial.deleteRecursively()
        try {
            if (src.isDirectory) {
                src.copyRecursively(partial, overwrite = true)
                if (!partial.renameTo(dest)) throw FileWriteFailed("Failed to finalize copy")
            } else {
                src.copyTo(partial, overwrite = true)
                if (!partial.renameTo(dest)) throw FileWriteFailed("Failed to finalize copy")
            }
        } catch (e: Exception) {
            if (partial.exists()) {
                partial.deleteRecursively()
            }
            if (e.isPermissionError()) {
                val uri = saf.getOrRequest(destDir)
                copyViaSaf(src, destDir, uri, dest.name)
            } else {
                throw e
            }
        }
    }

    private fun copyViaSaf(src: File, destDir: File, uri: Uri, finalName: String) {
        val documentDir = DocumentFile.fromTreeUri(context, uri)
            ?: throw SafRequired(destDir)
        copyViaSaf(src, destDir, documentDir, finalName)
    }

    private fun copyViaSaf(
        src: File,
        destDir: File,
        documentDir: DocumentFile,
        finalName: String
    ) {
        val partialName = "$finalName.partial"
        if (documentDir.findFile(finalName) != null) {
            throw FileWriteFailed("Destination already exists")
        }
        if (src.isDirectory) {
            val partialDir = documentDir.findFile(partialName) ?: documentDir.createDirectory(partialName)
            val destDocument = partialDir ?: throw FileWriteFailed("Unable to create directory via SAF")
            src.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    copyViaSaf(child, File(destDir, child.name), destDocument, child.name)
                } else {
                    copyFileToDocument(child, destDocument, child.name)
                }
            }
            if (!destDocument.renameTo(finalName)) {
                throw FileWriteFailed("Failed to finalize SAF copy")
            }
        } else {
            val copied = copyFileToDocument(src, documentDir, finalName, partialName)
            if (!copied.renameTo(finalName)) {
                throw FileWriteFailed("Failed to finalize SAF copy")
            }
        }
    }

    private fun copyFileToDocument(
        src: File,
        parentDoc: DocumentFile,
        finalName: String,
        partialName: String = "$finalName.partial"
    ): DocumentFile {
        val targetDoc = parentDoc.findFile(partialName) ?: parentDoc.createFile(
            "application/octet-stream",
            partialName
        )
        if (targetDoc == null) throw FileWriteFailed("Unable to create file via SAF")
        src.inputStream().use { input ->
            streamCopy(input, context.contentResolver.openOutputStream(targetDoc.uri))
        }
        return targetDoc
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
