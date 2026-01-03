package com.droidexplorer.websim.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

@Composable
fun TextViewerScreen(
    file: File,
    onClose: () -> Unit,
    showLineNumbers: Boolean = true
) {
    val lines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    var reachedEnd by remember { mutableStateOf(false) }
    var reader by remember { mutableStateOf<BufferedReader?>(null) }
    val gutterWidth = remember(lines.size) {
        val digits = (lines.size + 1).toString().length.coerceAtLeast(3)
        (digits * 8).dp.coerceAtLeast(32.dp)
    }

    fun resetReader() {
        reader?.close()
        reader = runCatching { BufferedReader(InputStreamReader(FileInputStream(file))) }
            .getOrNull()
        lines.clear()
        reachedEnd = reader == null
    }

    DisposableEffect(file.absolutePath) {
        resetReader()
        onDispose {
            reader?.close()
            reader = null
        }
    }

    suspend fun loadMore(batch: Int = 400) {
        if (isLoading || reachedEnd) return
        isLoading = true
        val (chunk, endReached) = withContext(Dispatchers.IO) {
            val target = reader ?: return@withContext Pair(emptyList<String>(), true)
            val chunk = mutableListOf<String>()
            repeat(batch) {
                val line = target.readLine() ?: return@withContext Pair(chunk.toList(), true)
                chunk.add(line)
            }
            Pair(chunk.toList(), false)
        }
        lines.addAll(chunk)
        if (endReached) reachedEnd = true
        isLoading = false
    }

    LaunchedEffect(Unit) { loadMore() }

    LaunchedEffect(listState) {
        snapshotFlowWithViewport(listState).collectLatest { lastVisible ->
            if (lastVisible > lines.size - 80) {
                loadMore()
            }
        }
    }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = file.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Showing ${lines.size}${if (reachedEnd) "" else "+"} lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                state = listState
            ) {
                itemsIndexed(lines) { index, line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (showLineNumbers) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .width(gutterWidth),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

private fun snapshotFlowWithViewport(listState: androidx.compose.foundation.lazy.LazyListState) =
    kotlinx.coroutines.flow.snapshotFlow {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) 0 else visible.maxOf { it.index }
    }.filter { it >= 0 }.map { it }
