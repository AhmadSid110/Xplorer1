package com.droidexplorer.websim.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.glass.neonGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedFolderMenu(
    folderName: String,
    onGrantAccess: () -> Unit,
    onExplain: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .neonGlass()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            ListItem(
                headlineContent = { Text("Grant access") },
                supportingContent = { Text("Allow access to this folder") },
                modifier = Modifier.clickable {
                    onGrantAccess()
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Why is this restricted?") },
                supportingContent = {
                    Text("Android restricts some folders for privacy")
                },
                modifier = Modifier.clickable(onClick = onExplain)
            )

            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RestrictedFolderItem(
    name: String,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    ListItem(
        headlineContent = { 
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        supportingContent = {
            Text(
                text = "Access restricted — tap to grant access",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Restricted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            }
        )
    )
}
