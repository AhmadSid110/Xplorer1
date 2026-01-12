package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SideDrawerContent(
    onTorBox: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Xplorer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(16.dp))
        Divider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Cloud, null) },
            label = { Text("TorBox") },
            selected = false,
            onClick = onTorBox,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
