package com.droidexplorer.websim.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedFolderMenu(
    folderName: String,
    onGrantAccess: () -> Unit,
    onExplain: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        Text(
            text = folderName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        Divider()

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RestrictedFolderItem(
    name: String,
    onLongPress: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = {
            Text(
                "Permission needed",
                color = MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongPress
        )
    )
}
