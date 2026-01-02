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
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destFolder, entry.name)
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

    fun isZipFile(file: File): Boolean {
        return file.extension.equals("zip", ignoreCase = true)
    }
}
