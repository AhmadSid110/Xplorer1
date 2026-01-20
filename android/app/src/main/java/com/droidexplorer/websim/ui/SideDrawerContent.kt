package com.droidexplorer.websim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.NeonCyan

@Composable
fun SideDrawerContent(
    onTorBox: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neonGlass(radius = 0.dp, alpha = 0.06f)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(NeonCyan.copy(alpha = 0.6f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Xplorer",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(16.dp))
                Divider(color = DividerSoft)

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Outlined.Cloud,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("TorBox") },
                    selected = false,
                    onClick = onTorBox,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Outlined.Settings,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onSettings,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}
