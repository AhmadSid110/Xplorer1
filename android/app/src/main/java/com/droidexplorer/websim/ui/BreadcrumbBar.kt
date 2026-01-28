package com.droidexplorer.websim.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val rootPath = "/storage/emulated/0"

    val segmentsWithPaths = mutableListOf<Pair<String, String>>()

    if (currentPath.startsWith(rootPath)) {
        segmentsWithPaths += ("INTERNAL" to rootPath)
        val remainder = currentPath.removePrefix(rootPath).trim('/').takeIf { it.isNotBlank() }
        if (remainder != null) {
            var pathBuilder = rootPath
            remainder.split('/').forEach { segment ->
                pathBuilder += "/$segment"
                segmentsWithPaths += (segment.uppercase() to pathBuilder)
            }
        }
    } else {
        val segments = currentPath.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            segmentsWithPaths += ("ROOT" to "/")
        } else {
            var pathBuilder = ""
            segments.forEach { segment ->
                pathBuilder += "/$segment"
                segmentsWithPaths += (segment.uppercase() to pathBuilder)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segmentsWithPaths.forEachIndexed { index, (label, path) ->
            if (index > 0) {
                Text(
                    text = "/",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onNavigateToPath(path) }
            )
        }
    }
}
