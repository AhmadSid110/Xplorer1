package com.droidexplorer.websim.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    state: SettingsState,
    onViewModeChange: (ViewMode) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onToggleSafSearch: (Boolean) -> Unit,
    onToggleTorBox: (Boolean) -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenCleaner: () -> Unit,
    onToggleBottomNav: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader("View")
        SectionCard {
            ViewMode.values().forEachIndexed { index, mode ->
                ListItem(
                    leadingContent = {
                        Icon(iconForViewMode(mode), contentDescription = null)
                    },
                    headlineContent = { Text(viewModeLabel(mode)) },
                    supportingContent = { Text("Default layout for file lists") },
                    trailingContent = {
                        RadioButton(
                            selected = state.defaultViewMode == mode,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onViewModeChange(mode)
                    }
                )
                if (index < ViewMode.values().lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        SectionHeader("Appearance")
        SectionCard {
            ThemeMode.values().forEachIndexed { index, mode ->
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    headlineContent = { Text(themeModeLabel(mode)) },
                    supportingContent = { Text("Choose the app theme") },
                    trailingContent = {
                        RadioButton(
                            selected = state.themeMode == mode,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onThemeModeChange(mode)
                    }
                )
                if (index < ThemeMode.values().lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        SectionCard {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                headlineContent = { Text("Bottom navigation") },
                supportingContent = { Text("Use bottom nav bar instead of top menu") },
                trailingContent = {
                    Switch(
                        checked = state.showBottomNav,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleBottomNav(!state.showBottomNav)
                }
            )
        }

                SectionHeader("Visibility")
        SectionCard {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Visibility, contentDescription = null) },
                headlineContent = { Text("Show hidden files") },
                supportingContent = { Text("Display dot-prefixed files and folders") },
                trailingContent = {
                    Switch(
                        checked = state.showHiddenFiles,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleHidden(!state.showHiddenFiles)
                }
            )
            HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                headlineContent = { Text("Include SAF locations in search") },
                supportingContent = { Text("Search folders granted via Storage Access Framework") },
                trailingContent = {
                    Switch(
                        checked = state.searchIncludeSaf,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleSafSearch(!state.searchIncludeSaf)
                }
            )
        }

        SectionHeader("Remote Access")
        SectionCard {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                headlineContent = { Text("Enable TorBox (Remote files)") },
                supportingContent = { Text("Access files stored remotely (read-only)") },
                trailingContent = {
                    Switch(
                        checked = state.torBoxEnabled,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleTorBox(!state.torBoxEnabled)
                }
            )
        }

        SectionHeader("Storage")
        SectionCard {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Storage, contentDescription = null) },
                headlineContent = { Text("Enable full storage access") },
                supportingContent = { Text("Grant access to all file types (PDF, ZIP, APK, etc.)") },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRequestAllFilesAccess()
                }
            )
            HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                headlineContent = { Text("Storage usage") },
                supportingContent = { Text("View storage breakdown") },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenStorage()
                }
            )
            HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Filled.CleaningServices, contentDescription = null) },
                headlineContent = { Text("Clean up storage") },
                supportingContent = { Text("Find large files and remove clutter") },
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenCleaner()
                }
            )
        }
    }
}

private fun themeModeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.LIGHT -> "Light (recommended)"
    ThemeMode.DARK -> "Dark"
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.CYBER -> "Cyberpunk / Tesla"
}

@Composable
private fun iconForViewMode(mode: ViewMode) = when (mode) {
    ViewMode.LIST -> Icons.Filled.List
    ViewMode.GRID -> Icons.Filled.GridView
    ViewMode.DETAILS -> Icons.Filled.ViewList
}

private fun viewModeLabel(mode: ViewMode): String = when (mode) {
    ViewMode.LIST -> "List"
    ViewMode.GRID -> "Grid"
    ViewMode.DETAILS -> "Details"
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}
