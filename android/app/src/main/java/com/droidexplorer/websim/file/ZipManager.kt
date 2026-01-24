package com.droidexplorer.websim.file

import java.io.File
import org.apache.commons.compress.archivers.zip.ZipFile

object ZipManager {

    fun list(zip: File): List<ZipEntryItem> {
        if (!zip.extension.equals("zip", ignoreCase = true)) return emptyList()
        ZipFile(zip).use { z ->
            return z.entries.asSequence()
                .map { entry ->
                    ZipEntryItem(
                        name = entry.name,
                        size = entry.size.coerceAtLeast(0L),
                        isDirectory = entry.isDirectory
                    )
                }
                .toList()
        }
    }

    fun extract(
        zip: File,
        targetDir: File
    ) {
        if (!zip.extension.equals("zip", ignoreCase = true)) return
        targetDir.mkdirs()
        val targetCanonical = targetDir.canonicalPath.let {
            if (it.endsWith(File.separator)) it else it + File.separator
        }
        ZipFile(zip).use { z ->
            z.entries.asSequence().forEach { entry ->
                val entryName = entry.name
                val normalized = entryName.replace("\\", "/")
                val out = resolveEntryFile(targetDir, normalized, targetCanonical) ?: return@forEach

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

    private fun resolveEntryFile(
        targetDir: File,
        normalized: String,
        targetCanonical: String
    ): File? {
        if (
            normalized.isBlank() ||
            normalized.contains("..") ||
            normalized.startsWith("/") ||
            normalized.contains(":") ||
            normalized.contains('\u0000')
        ) {
            return null
        }

        val out = File(targetDir, normalized)
        val outCanonical = out.canonicalPath
        return if (outCanonical.startsWith(targetCanonical)) out else null
    }
}
