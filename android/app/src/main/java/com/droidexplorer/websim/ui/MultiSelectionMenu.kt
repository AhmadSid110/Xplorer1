package com.droidexplorer.websim.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.ui.glass.CyberGlassPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectionMenu(
    items: List<FsNode>,
    onZipSelection: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val menuText = Color.White.copy(alpha = 0.92f)
    val accent = Color(0xFF00E5FF)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.Transparent) {
        CyberGlassPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${items.size} selected",
                style = MaterialTheme.typography.titleMedium,
                color = menuText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SelectionItem(
                icon = Icons.Outlined.Archive,
                text = "Zip selection",
                textColor = menuText,
                iconTint = accent,
                onClick = onZipSelection
            )

            SelectionItem(
                icon = Icons.Outlined.Close,
                text = "Clear selection",
                textColor = menuText,
                iconTint = menuText,
                onClick = onClear
            )
        }
    }
}

@Composable
private fun SelectionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodyLarge)
    }
}
