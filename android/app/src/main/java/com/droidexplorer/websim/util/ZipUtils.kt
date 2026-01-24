package com.droidexplorer.websim.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream

object ZipUtils {

    fun zipFile(sourceFile: File): Result<File> {
        return try {
            val zipFile = File(sourceFile.parent, "${sourceFile.nameWithoutExtension}.zip")
            ZipArchiveOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.setMethod(ZipArchiveOutputStream.DEFLATED)
                if (sourceFile.isDirectory) {
                    zipDirectory(sourceFile, sourceFile.name, zos)
                } else {
                    zipSingleFile(sourceFile, sourceFile.name, zos)
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun zipFiles(files: List<File>, zipName: String): Result<File> {
        return try {
            val targetDir = files.firstOrNull()?.parentFile ?: return Result.failure(IllegalStateException("No files"))
            val zipFile = File(targetDir, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
            ZipArchiveOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.setMethod(ZipArchiveOutputStream.DEFLATED)
                files.forEach { file ->
                    if (file.isDirectory) {
                        zipDirectory(file, file.name, zos)
                    } else {
                        zipSingleFile(file, file.name, zos)
                    }
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun zipDirectory(folder: File, parentPath: String, zos: ZipArchiveOutputStream) {
        folder.listFiles()?.forEach { file ->
            val path = "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipDirectory(file, path, zos)
            } else {
                zipSingleFile(file, path, zos)
            }
        }
    }

    private fun zipSingleFile(file: File, entryName: String, zos: ZipArchiveOutputStream) {
        FileInputStream(file).use { fis ->
            val entry = ZipArchiveEntry(entryName)
            zos.putArchiveEntry(entry)
            fis.copyTo(zos)
            zos.closeArchiveEntry()
        }
    }

    fun unzip(zipFile: File): Result<File> = extractToFolder(zipFile)

    fun extractHere(zipFile: File): Result<File> {
        return try {
            if (!isZipFile(zipFile)) return Result.failure(IllegalArgumentException("Not a zip"))
            val destFolder = zipFile.parentFile ?: return Result.failure(IllegalStateException("No parent"))
            com.droidexplorer.websim.file.ZipManager.extract(zipFile, destFolder)
            Result.success(destFolder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractToFolder(zipFile: File): Result<File> {
        return try {
            if (!isZipFile(zipFile)) return Result.failure(IllegalArgumentException("Not a zip"))
            val parent = zipFile.parentFile ?: return Result.failure(IllegalStateException("No parent"))
            val destFolder = File(parent, zipFile.nameWithoutExtension)
            if (!destFolder.exists()) {
                destFolder.mkdirs()
            }
            com.droidexplorer.websim.file.ZipManager.extract(zipFile, destFolder)
            Result.success(destFolder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isZipFile(file: File): Boolean {
        return file.extension.equals("zip", ignoreCase = true)
    }
}
