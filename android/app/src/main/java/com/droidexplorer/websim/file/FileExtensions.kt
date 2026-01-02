package com.droidexplorer.websim.file

import java.io.File

private val textExtensions = setOf(
    "txt", "md", "json", "xml", "html", "css",
    "js", "srt", "log", "ini", "yaml", "yml"
)

private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif")

fun File.isTextFile(): Boolean = extension.lowercase() in textExtensions

fun File.isImage(): Boolean = extension.lowercase() in imageExtensions

fun openFile(
    file: File,
    openText: (File) -> Unit,
    openImage: (File) -> Unit,
    openPdf: (File) -> Unit = {},
    openOther: (File) -> Unit = {}
) {
    when {
        file.isTextFile() -> openText(file)
        file.isImage() -> openImage(file)
        file.extension.equals("pdf", ignoreCase = true) -> openPdf(file)
        else -> openOther(file)
    }
}
