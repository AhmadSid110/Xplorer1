package com.droidexplorer.websim.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    state: SettingsState,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onToggleSafSearch: (Boolean) -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onOpenStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("View", style = MaterialTheme.typography.titleMedium)

        ViewMode.values().forEach { mode ->
            RadioButtonRow(
                text = mode.name,
                selected = state.defaultViewMode == mode,
                onClick = { onViewModeChange(mode) }
            )
        }

        Divider()

        SwitchRow(
            text = "Show hidden files",
            checked = state.showHiddenFiles,
            onCheckedChange = onToggleHidden
        )

        SwitchRow(
            text = "Include SAF locations in search",
            checked = state.searchIncludeSaf,
            onCheckedChange = onToggleSafSearch
        )

        Divider()

        ListItem(
            headlineContent = { Text("Enable full storage access") },
            supportingContent = { Text("Grant access to all file types (PDF, ZIP, APK, etc.)") },
            modifier = Modifier.clickable { onRequestAllFilesAccess() }
        )

        Divider()

        ListItem(
            headlineContent = { Text("Storage") },
            supportingContent = { Text("View storage usage") },
            modifier = Modifier.clickable { onOpenStorage() }
        )
    }
}

@Composable
private fun RadioButtonRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
