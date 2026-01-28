package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.glass.neonGlass

@Composable
fun SelectionActionBar(
    count: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onZip: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .neonGlass(alpha = 0.32f),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$count selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                androidx.compose.material3.IconButton(onClick = onCut) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCut,
                        contentDescription = "Cut",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                androidx.compose.material3.IconButton(onClick = onPaste) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = "Paste",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.IconButton(onClick = onZip) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = "Zip selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
