package com.droidexplorer.websim.file

import java.io.File
import java.util.zip.ZipFile

object ZipManager {

    fun list(zip: File): List<String> {
        ZipFile(zip).use { z ->
            return z.entries().asSequence()
                .map { it.name }
                .toList()
        }
    }

    fun extract(
        zip: File,
        targetDir: File
    ) {
        targetDir.mkdirs()
        val targetCanonical = targetDir.canonicalPath
        ZipFile(zip).use { z ->
            z.entries().asSequence().forEach { entry ->
                val entryName = entry.name
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    return@forEach
                }

                val out = File(targetDir, entryName)
                val outCanonical = out.canonicalPath
                if (!outCanonical.startsWith(targetCanonical)) {
                    return@forEach
                }

                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    z.getInputStream(entry).use { input ->
                        out.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}
