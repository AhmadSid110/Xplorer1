package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DualPaneScreen(singlePane: Boolean) {
    Row(Modifier.fillMaxSize()) {
        FileListPane(Modifier.weight(1f), "/storage/emulated/0")
        if (!singlePane) {
            FileListPane(Modifier.weight(1f), "/storage/emulated/0")
        }
    }
}
