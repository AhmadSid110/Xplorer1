package com.droidexplorer.websim.torbox

import kotlin.math.max

/**
 * Presentation-layer model for TorBox items (folders + files).
 */
data class TorBoxItem(
    val id: String?,
    val name: String,
    val size: Long,
    val fullPath: String,
    val folderPath: String,
    val isFolder: Boolean,
    val type: TorBoxFileType,
    val modified: Long = 0L
) {
    val uniqueKey: String = if (isFolder) "torbox:folder:$fullPath" else "torbox:file:$id"
}

enum class TorBoxSortMode {
    NAME,
    SIZE,
    DATE,
    TYPE
}

enum class TorBoxFilter {
    ALL,
    VIDEO,
    AUDIO,
    IMAGE,
    ARCHIVE,
    DOC,
    APK,
    LARGE,
    OTHER
}

fun mapTorBoxFile(file: TorBoxFile): TorBoxItem {
    val normalizedPath = normalizeTorBoxPath(file.absolutePath, file.name)
    val folderPath = normalizedPath.substringBeforeLast('/', "").trim('/')
    return TorBoxItem(
        id = file.id,
        name = file.name,
        size = max(0L, file.size),
        fullPath = normalizedPath,
        folderPath = folderPath,
        isFolder = false,
        type = TorBoxFileType.fromFileName(file.name),
        modified = 0L
    )
}

fun torBoxFolderItem(path: String, name: String): TorBoxItem {
    return TorBoxItem(
        id = null,
        name = name,
        size = 0L,
        fullPath = path,
        folderPath = path.trim('/'),
        isFolder = true,
        type = TorBoxFileType.OTHER,
        modified = 0L
    )
}

fun torBoxPathSegments(path: String): List<String> {
    val trimmed = path.trim('/').trim()
    if (trimmed.isBlank()) return emptyList()
    return trimmed.split('/').filter { it.isNotBlank() }
}

fun torBoxPanePath(subPath: String): String {
    val cleaned = subPath.trim('/').trim()
    return if (cleaned.isBlank()) "torbox:" else "torbox:/$cleaned"
}

fun torBoxSubPath(currentPath: String): String {
    return currentPath.removePrefix("torbox:").trim('/').trim()
}

fun formatTorBoxSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gb -> String.format("%.2f GB", value / gb)
        value >= mb -> String.format("%.2f MB", value / mb)
        value >= kb -> String.format("%.1f KB", value / kb)
        else -> "$bytes B"
    }
}

fun extractVideoTag(name: String): String? {
    val lower = name.lowercase()
    val candidates = listOf("2160p", "1440p", "1080p", "720p", "480p", "360p")
    return candidates.firstOrNull { lower.contains(it) }
}

private fun normalizeTorBoxPath(absolutePath: String, name: String): String {
    val cleaned = absolutePath.trim()
    if (cleaned.isNotBlank()) return cleaned
    return name.trim()
}
