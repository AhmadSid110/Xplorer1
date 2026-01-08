package com.droidexplorer.websim.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.storage.StorageInfo
import com.droidexplorer.websim.ui.formatFileSize
import kotlin.math.min

@Composable
fun StorageScreen(info: StorageInfo) {
    val progress = if (info.total > 0) {
        (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "storageProgress"
    )

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Storage", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        // Storage Hero Ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 24.dp.toPx()
                val diameter = min(size.width, size.height)
                val radius = (diameter - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - diameter + strokeWidth) / 2,
                    (size.height - diameter + strokeWidth) / 2
                )

                // Background arc
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter - strokeWidth, diameter - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc - using Material theme colors
                val progressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter - strokeWidth, diameter - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatFileSize(info.used),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of ${formatFileSize(info.total)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Category visualization
        Text(
            "Estimated categories",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Placeholder data for categories - these are estimates only
        // In production, these would be calculated from actual file system data
        // Colors are specified per requirements for visual consistency
        val categories = remember {
            listOf(
                StorageCategory("Images", 0.25f, Color(0xFF2196F3).copy(alpha = 0.7f)), // Blue
                StorageCategory("Videos", 0.20f, Color(0xFF9C27B0).copy(alpha = 0.7f)), // Purple
                StorageCategory("Audio", 0.15f, Color(0xFF009688).copy(alpha = 0.7f)), // Teal
                StorageCategory("Apps", 0.18f, Color(0xFFFF9800).copy(alpha = 0.7f)), // Orange
                StorageCategory("Archives", 0.12f, Color(0xFF757575).copy(alpha = 0.7f)), // Gray
                StorageCategory("Other", 0.10f, Color(0xFF607D8B).copy(alpha = 0.7f)) // SurfaceTint-like
            )
        }

        categories.forEach { category ->
            CategoryItem(
                category = category,
                totalBytes = info.total
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

data class StorageCategory(
    val name: String,
    val percentage: Float,
    val color: Color
)

@Composable
private fun CategoryItem(
    category: StorageCategory,
    totalBytes: Long
) {
    val animatedWidth by animateFloatAsState(
        targetValue = category.percentage,
        animationSpec = tween(durationMillis = 600),
        label = "categoryProgress"
    )
    
    val estimatedSize = (totalBytes * category.percentage).toLong()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatFileSize(estimatedSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                // Background bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {}
                
                // Progress bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(animatedWidth)
                        .height(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = category.color
                ) {}
            }
        }
    }
}

private fun StorageInfo.usedReadable() = formatFileSize(used)
private fun StorageInfo.freeReadable() = formatFileSize(free)
private fun StorageInfo.totalReadable() = formatFileSize(total)
