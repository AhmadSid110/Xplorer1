package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.io.File

@Composable
fun RenameDialog(
    file: File,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        error = null
                    },
                    label = { Text("New name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) {
                        error = "Name cannot be empty"
                    } else if (newName.contains("/") || newName.contains("\\")) {
                        error = "Invalid characters in name"
                    } else {
                        onRename(newName)
                        onDismiss()
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
