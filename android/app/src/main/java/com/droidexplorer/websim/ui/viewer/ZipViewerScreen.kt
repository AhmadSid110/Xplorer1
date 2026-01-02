package com.droidexplorer.websim.ui.viewer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.ZipManager
import java.io.File

@Composable
fun ZipViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val entries = remember(file.absolutePath) { ZipManager.list(file) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text("←")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val outDir = File(file.parentFile, file.nameWithoutExtension)
                        ZipManager.extract(file, outDir)
                    }) {
                        Text("Extract")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(entries.size) {
                Text(entries[it], modifier = Modifier.padding(8.dp))
            }
        }
    }
}
