package com.droidexplorer.websim.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileManager

@Composable
fun FileListPane(modifier: Modifier, startPath: String) {
    val navigator = remember { PaneNavigator(startPath) }
    var path by remember { mutableStateOf(startPath) }

    val files = remember(path) { FileManager.list(path) }

    BackHandler(enabled = navigator.canGoBack()) {
        navigator.goBack()
        path = navigator.currentPath
    }

    Column(modifier.padding(8.dp)) {
        Text(path, style = MaterialTheme.typography.labelSmall)
        Divider()
        files.forEach { file ->
            Text(
                text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (file.isDirectory) {
                            navigator.navigateTo(file.absolutePath)
                            path = navigator.currentPath
                        }
                    }
                    .padding(6.dp)
            )
        }
    }
}
