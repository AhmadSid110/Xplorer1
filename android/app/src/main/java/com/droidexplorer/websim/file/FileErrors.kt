package com.droidexplorer.websim.file

import java.io.File
import java.io.IOException

class SafRequired(val directory: File) : Exception(
    "Android requires permission to write here. Select this folder once."
)

class FileWriteFailed(message: String, cause: Throwable? = null) : Exception(message, cause)

fun Throwable.isPermissionError(): Boolean {
    if (this is SecurityException) return true
    if (this is IOException && (message?.contains("permission", ignoreCase = true) == true ||
                message?.contains("EACCES", ignoreCase = true) == true ||
                message?.contains("EPERM", ignoreCase = true) == true)
    ) return true
    return cause?.isPermissionError() == true
}
