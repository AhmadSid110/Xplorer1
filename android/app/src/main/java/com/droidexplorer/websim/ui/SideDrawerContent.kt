package com.droidexplorer.websim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.cyberGlow

enum class DrawerDestination {
    FILES,
    CLEANER,
    TORBOX,
    DOWNLOADS,
    PERMISSIONS,
    SETTINGS
}

@Composable
fun SideDrawerContent(
    activeDestination: DrawerDestination,
    onFiles: () -> Unit,
    onCleaner: () -> Unit,
    onTorBox: () -> Unit,
    onDownloads: () -> Unit,
    onPermissions: () -> Unit,
    onSettings: () -> Unit
) {
    val accent = LocalCyberAccent.current

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    accent.copy(alpha = 0.35f),
                    RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "XPLORER",
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Secure File System",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))
            Divider(color = accent.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            DrawerItemRow(
                active = activeDestination == DrawerDestination.FILES,
                icon = Icons.Outlined.Folder,
                label = "Files",
                onClick = onFiles
            )
            DrawerItemRow(
                active = activeDestination == DrawerDestination.CLEANER,
                icon = Icons.Outlined.CleaningServices,
                label = "Cleaner",
                onClick = onCleaner
            )
            DrawerItemRow(
                active = activeDestination == DrawerDestination.TORBOX,
                icon = Icons.Outlined.Cloud,
                label = "TorBox",
                onClick = onTorBox
            )
            DrawerItemRow(
                active = activeDestination == DrawerDestination.DOWNLOADS,
                icon = Icons.Outlined.Cloud,
                label = "Downloads",
                onClick = onDownloads
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerSoft)
            Spacer(Modifier.height(12.dp))

            DrawerItemRow(
                active = activeDestination == DrawerDestination.PERMISSIONS,
                icon = Icons.Outlined.Security,
                label = "Permissions",
                onClick = onPermissions
            )
            DrawerItemRow(
                active = activeDestination == DrawerDestination.SETTINGS,
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun DrawerItemRow(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val accent = LocalCyberAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (active) accent.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .then(
                if (active) {
                    Modifier
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .cyberGlow(accent, intensity = 0.3f)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp)
            .wrapContentHeight()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(8.dp))
}
