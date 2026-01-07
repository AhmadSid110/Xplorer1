package com.droidexplorer.websim.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.droidexplorer.websim.file.FsNode

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
