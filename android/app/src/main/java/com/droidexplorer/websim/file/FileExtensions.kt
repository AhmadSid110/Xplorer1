package com.droidexplorer.websim.file

import java.io.File

/**
 * Text file extensions that can be opened in the built-in text viewer.
 * NOTE: This is ONLY used for determining how to OPEN files, NOT for visibility.
 * ALL file types must be visible in the file list regardless of extension.
 */
private val textExtensions = setOf(
    "txt", "md", "json", "xml", "html", "css",
    "js", "py", "srt", "log", "ini", "yaml", "yml", "csv",
    "kt", "java", "c", "cpp", "h", "sh", "bat"
)

/**
 * Image file extensions that can be opened in the built-in image viewer.
 * NOTE: This is ONLY used for determining how to OPEN files, NOT for visibility.
 * ALL file types must be visible in the file list regardless of extension.
 */
private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif")

fun File.isTextFile(): Boolean = extension.lowercase() in textExtensions

fun File.isImage(): Boolean = extension.lowercase() in imageExtensions

/**
 * Opens a file using the appropriate handler based on its extension.
 * 
 * CRITICAL: This function is ONLY for determining which viewer/handler to use.
 * It must NEVER be used to determine file visibility in lists/grids/details views.
 * ALL files (regardless of extension, MIME type, or category) must be visible.
 * 
 * @param file The file to open
 * @param openText Handler for text files (.txt, .py, .json, etc.)
 * @param openImage Handler for image files (.png, .jpg, etc.)
 * @param openPdf Handler for PDF files
 * @param openOther Handler for all other files (uses system intent)
 */
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
