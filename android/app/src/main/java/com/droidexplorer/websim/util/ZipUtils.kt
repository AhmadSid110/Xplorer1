package com.droidexplorer.websim.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    fun zipFile(sourceFile: File): Result<File> {
        return try {
            val zipFile = File(sourceFile.parent, "${sourceFile.nameWithoutExtension}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
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

    private fun zipDirectory(folder: File, parentPath: String, zos: ZipOutputStream) {
        folder.listFiles()?.forEach { file ->
            val path = "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipDirectory(file, path, zos)
            } else {
                zipSingleFile(file, path, zos)
            }
        }
    }

    private fun zipSingleFile(file: File, entryName: String, zos: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    fun unzip(zipFile: File): Result<File> {
        return try {
            val destFolder = File(zipFile.parent, zipFile.nameWithoutExtension)
            if (!destFolder.exists()) {
                destFolder.mkdirs()
            }
            val destFolderCanonicalPath = destFolder.canonicalPath
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Security: Validate entry name to prevent Zip Slip attacks
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue // Skip malicious entries
                    }
                    
                    val file = File(destFolder, entryName)
                    val fileCanonicalPath = file.canonicalPath
                    
                    // Verify the file is within the destination folder (Zip Slip protection)
                    if (!fileCanonicalPath.startsWith(destFolderCanonicalPath)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue // Skip entries that would escape destination
                    }
                    
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(destFolder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
