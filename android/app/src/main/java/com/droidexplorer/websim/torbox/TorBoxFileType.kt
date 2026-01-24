package com.droidexplorer.websim.torbox

enum class TorBoxFileType {
    VIDEO,
    AUDIO,
    IMAGE,
    ARCHIVE,
    DOC,
    APK,
    PDF,
    OTHER;

    companion object {
        fun fromFileName(name: String): TorBoxFileType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext in VIDEO_EXTENSIONS -> VIDEO
                ext in AUDIO_EXTENSIONS -> AUDIO
                ext in IMAGE_EXTENSIONS -> IMAGE
                ext in ARCHIVE_EXTENSIONS -> ARCHIVE
                ext in DOC_EXTENSIONS -> DOC
                ext == "apk" -> APK
                ext == "pdf" -> PDF
                else -> OTHER
            }
        }
    }
}

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp"
)

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "aac", "m4a", "flac", "ogg", "wav", "opus"
)

private val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"
)

private val ARCHIVE_EXTENSIONS = setOf(
    "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
)

private val DOC_EXTENSIONS = setOf(
    "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "rtf", "md", "csv"
)
