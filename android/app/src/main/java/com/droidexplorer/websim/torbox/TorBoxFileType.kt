package com.droidexplorer.websim.torbox

enum class TorBoxFileType {
    VIDEO,
    AUDIO,
    IMAGE,
    PDF,
    OTHER;

    companion object {
        fun fromFileName(name: String): TorBoxFileType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext in VIDEO_EXTENSIONS -> VIDEO
                ext in AUDIO_EXTENSIONS -> AUDIO
                ext in IMAGE_EXTENSIONS -> IMAGE
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
