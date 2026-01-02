package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.SortOrder
import com.droidexplorer.websim.file.SortType

@Composable
fun SortMenu(
    currentSortType: SortType,
    currentSortOrder: SortOrder,
    onSortChange: (SortType, SortOrder) -> Unit,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        DropdownMenuItem(
            text = { Text("Name") },
            onClick = { onSortChange(SortType.NAME, currentSortOrder); onDismiss() },
            leadingIcon = {
                if (currentSortType == SortType.NAME) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        )
        
        DropdownMenuItem(
            text = { Text("Size") },
            onClick = { onSortChange(SortType.SIZE, currentSortOrder); onDismiss() },
            leadingIcon = {
                if (currentSortType == SortType.SIZE) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        )
        
        DropdownMenuItem(
            text = { Text("Date") },
            onClick = { onSortChange(SortType.DATE, currentSortOrder); onDismiss() },
            leadingIcon = {
                if (currentSortType == SortType.DATE) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        Text(
            text = "Order",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        DropdownMenuItem(
            text = { Text("Ascending") },
            onClick = { onSortChange(currentSortType, SortOrder.ASC); onDismiss() },
            leadingIcon = {
                if (currentSortOrder == SortOrder.ASC) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        )
        
        DropdownMenuItem(
            text = { Text("Descending") },
            onClick = { onSortChange(currentSortType, SortOrder.DESC); onDismiss() },
            leadingIcon = {
                if (currentSortOrder == SortOrder.DESC) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        )
    }
}
