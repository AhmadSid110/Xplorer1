package com.droidexplorer.websim.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.droidexplorer.websim.file.FileOperator
import com.droidexplorer.websim.file.SafRequired
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewerSheet(
    file: File,
    fileOperator: FileOperator,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSafRequired: (File) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        isLoading = true
        fileOperator.readText(file).fold(
            onSuccess = { content = it },
            onFailure = {
                error = it.message
                if (it is SafRequired) onSafRequired(it.directory)
            }
        )
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        item {
                            Text(
                                text = content,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorSheet(
    file: File,
    fileOperator: FileOperator,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onSafRequired: (File) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        isLoading = true
        fileOperator.readText(file).fold(
            onSuccess = { content = it },
            onFailure = {
                error = it.message
                if (it is SafRequired) onSafRequired(it.directory)
            }
        )
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Edit ${file.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    placeholder = { Text("Edit content") },
                    singleLine = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        saving = true
                        fileOperator.writeText(file, content).fold(
                            onSuccess = {
                                saving = false
                                onSaved()
                                onDismiss()
                            },
                            onFailure = {
                                error = it.message
                                if (it is SafRequired) onSafRequired(it.directory)
                                saving = false
                            }
                        )
                    },
                    enabled = !saving
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerSheet(
    file: File,
    fileOperator: FileOperator,
    onDismiss: () -> Unit,
    onSafRequired: (File) -> Unit
) {
    val context = LocalContext.current
    var pageIndex by rememberSaveable(file.absolutePath) { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var pageImage by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(file, pageIndex) {
        isLoading = true
        fileOperator.openDescriptor(file, ParcelFileDescriptor.MODE_READ_ONLY).fold(
            onSuccess = { descriptor ->
                descriptor.use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        pageCount = renderer.pageCount
                        if (renderer.pageCount == 0) return@use
                        val index = pageIndex.coerceIn(0, renderer.pageCount - 1)
                        if (index != pageIndex) pageIndex = index
                        renderer.openPage(index).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageImage = bitmap
                        }
                    }
                }
            },
            onFailure = {
                error = it.message
                if (it is SafRequired) onSafRequired(it.directory)
            }
        )
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { openPdfExternal(context, file) }) {
                    Text("Open in another app")
                }
            } else {
                pageImage?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF page",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }
                    Text(
                        text = "Page ${pageIndex + 1} / ${pageCount.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                        enabled = pageIndex < pageCount - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerSheet(
    file: File,
    onDismiss: () -> Unit
) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                else -> bitmap?.let { loaded ->
                    Image(
                        bitmap = loaded.asImageBitmap(),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text("Unable to load image")
            }
        }
    }
}

private fun openPdfExternal(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
    } catch (e: Exception) {
        Log.e("PdfViewer", "Unable to open PDF externally: ${file.absolutePath}", e)
    }
}
