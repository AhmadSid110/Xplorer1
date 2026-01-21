package com.droidexplorer.websim.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.ui.theme.LocalCyberAccent

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val segments = currentPath.split("/").filter { it.isNotEmpty() }
    val accent = LocalCyberAccent.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root segment
        Surface(
            modifier = Modifier
                .padding(end = 4.dp)
                .clickable { onNavigateToPath("/") },
            shape = RoundedCornerShape(8.dp),
            color = if (segments.isEmpty()) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "Internal Storage",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        var pathBuilder = ""
        segments.forEachIndexed { index, segment ->
            pathBuilder = "$pathBuilder/$segment"
            val fullPath = pathBuilder
            val isLast = index == segments.lastIndex
            
            Text(
                text = "/",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            
            Surface(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .then(
                        if (isLast) {
                            Modifier.drawBehind {
                                val strokeWidth = 1.dp.toPx()
                                val y = size.height - strokeWidth / 2
                                drawLine(
                                    color = accent.copy(alpha = 0.6f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = strokeWidth
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onNavigateToPath(fullPath) },
                shape = RoundedCornerShape(8.dp),
                color = if (isLast) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isLast) 
                        MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
