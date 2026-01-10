package com.droidexplorer.websim.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DebugOverlay(
    state: DebugOverlayState,
    onClose: () -> Unit
) {
    if (!state.visible) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Text("DEBUG PANEL", color = Color.Red, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(color = Color.Red)

            Text("Path: ${state.path}", color = Color.White)
            Text("TorBox Client: ${state.torBoxClientPresent}", color = Color.White)
            Text("Total Files: ${state.totalFiles}", color = Color.White)
            Text("TorBox Files: ${state.torBoxFiles}", color = Color.White)
            Text("Local Files: ${state.localFiles}", color = Color.White)
            Text("Last Trigger: ${state.lastTrigger}", color = Color.White)

            Spacer(Modifier.height(6.dp))

            Text("RAW TORBOX RESPONSE:", color = Color.Yellow)
            Text(
                state.rawTorBoxResponse.ifBlank { "<empty>" },
                color = Color.LightGray,
                maxLines = 6
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Close Debug")
            }
        }
    }
}
