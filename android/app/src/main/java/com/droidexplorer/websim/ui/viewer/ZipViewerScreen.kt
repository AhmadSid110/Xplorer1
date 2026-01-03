package com.droidexplorer.websim.ui.viewer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipViewerScreen(
    file: File,
    onClose: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val entriesResult by produceState(initialValue = Result.success(emptyList<String>()), key1 = file.absolutePath) {
        value = runCatching { withContext(Dispatchers.IO) { ZipManager.list(file) } }
    }
    val entries = entriesResult.getOrElse { emptyList() }

    LaunchedEffect(entriesResult.exceptionOrNull()) {
        entriesResult.exceptionOrNull()?.let {
            val reason = it.message ?: "Unknown error"
            snackbarHostState.showSnackbar("Failed to open zip: $reason")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val outDir = File(file.parentFile, file.nameWithoutExtension)
                        scope.launch {
                            runCatching { ZipManager.extract(file, outDir) }.fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar("Extracted to ${outDir.absolutePath}")
                                },
                                onFailure = {
                                    val reason = it.message ?: "Unknown error"
                                    snackbarHostState.showSnackbar("Failed to extract: $reason")
                                }
                            )
                        }
                    }) {
                        Text("Extract")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(entries) { entry ->
                Text(entry, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
