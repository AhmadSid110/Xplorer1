package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import com.droidexplorer.websim.ui.glass.CyberGlass

@Composable
fun RenameDialog(
    file: File,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        CyberGlass(
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Rename",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = null
                    },
                    label = { Text("New name", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = Color(0xFFFF4D6D)) } },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0B1620),
                        unfocusedContainerColor = Color(0xFF0B1620),
                        cursorColor = Color(0xFF00E5FF),
                        focusedIndicatorColor = Color(0xFF00E5FF),
                        unfocusedIndicatorColor = Color(0xFF00E5FF).copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White.copy(alpha = 0.85f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                        Text("Rename", color = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }
}
