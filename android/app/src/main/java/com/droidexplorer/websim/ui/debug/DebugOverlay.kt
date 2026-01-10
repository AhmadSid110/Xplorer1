package com.droidexplorer.websim.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode

@Composable
fun DebugOverlay(
    state: DebugOverlayState,
    onToggle: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        if (!state.enabled) {
            FloatingActionButton(
                onClick = onToggle,
                containerColor = Color.Red,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text("DBG")
            }
            return
        }

        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(8.dp)
                .border(1.dp, Color.Red)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF121212))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Text("DEBUG PANEL", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(color = Color.Red)

                DebugRow("Path", state.currentPath)
                DebugRow("TorBox Client", state.torBoxClientPresent.toString())
                DebugRow("Total Files", state.fileCount.toString())
                DebugRow("TorBox Files", state.torBoxFileCount.toString())
                DebugRow("Local Files", state.localFileCount.toString())
                DebugRow("Last Trigger", state.lastTrigger)

                Spacer(Modifier.height(8.dp))
                Text("FILES SNAPSHOT", color = Color.Yellow)

                state.filesSnapshot.take(10).forEach {
                    Text(
                        when (it) {
                            is FsNode.TorBox -> "🟦 TorBox → ${it.name}"
                            else -> "🟩 Local → ${it.name}"
                        },
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Close Debug")
                }
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray)
        Text(value, color = Color.White)
    }
}
