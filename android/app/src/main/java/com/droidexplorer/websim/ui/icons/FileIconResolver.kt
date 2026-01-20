package com.droidexplorer.websim.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.NeonAmber
import com.droidexplorer.websim.ui.theme.NeonBlue
import com.droidexplorer.websim.ui.theme.NeonGreen
import com.droidexplorer.websim.ui.theme.NeonMagenta
import com.droidexplorer.websim.ui.theme.NeonPink
import com.droidexplorer.websim.ui.theme.NeonPurple
import com.droidexplorer.websim.ui.theme.TextMuted

@Composable
fun fileIconFor(node: FsNode): ImageVector {
    val ext = node.name.substringAfterLast('.', "").lowercase()

    return when (ext) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.Archive
        "apk" -> Icons.Outlined.Android
        "py"  -> Icons.Outlined.Code
        "kt", "java", "json", "xml", "js", "ts", "c", "cpp", "h", "sh" ->
            Icons.Outlined.Code

        else -> Icons.Outlined.InsertDriveFile
    }
}

@Composable
fun fileIconColor(node: FsNode): Color {
    val accent = LocalCyberAccent.current
    if (node.isDirectory) return accent

    val ext = node.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> NeonMagenta
        "jpg", "jpeg", "png", "gif", "webp", "heic", "bmp" -> NeonPurple
        "mp4", "mkv", "avi", "mov", "webm", "3gp" -> NeonGreen
        "mp3", "wav", "ogg", "flac", "m4a", "aac" -> NeonBlue
        "apk" -> NeonAmber
        "zip", "rar", "7z", "tar", "gz" -> NeonPink
        else -> TextMuted
    }
}
