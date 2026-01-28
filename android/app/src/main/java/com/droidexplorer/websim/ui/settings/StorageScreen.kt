package com.droidexplorer.websim.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.storage.StorageInfo
import com.droidexplorer.websim.ui.formatFileSize
import com.droidexplorer.websim.ui.theme.ChamferShape
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.backgroundGradient
import kotlin.math.min

/**
 * Data class for storage category information.
 */
data class StorageCategoryData(
    val images: Long = 0,
    val videos: Long = 0,
    val audio: Long = 0,
    val apks: Long = 0,
    val archives: Long = 0
)

@Composable
fun StorageScreen(
    info: StorageInfo,
    categoryData: StorageCategoryData?,
    modifier: Modifier = Modifier
) {
    val progress =
        if (info.total > 0) (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f)
        else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "storageProgress"
    )

    val accent = LocalCyberAccent.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "STORAGE",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "OVERVIEW OF DEVICE SPACE",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = ChamferShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CAPACITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (animatedProgress * 100).toInt().toString() + "% // CRITICAL",
                                style = MaterialTheme.typography.titleMedium,
                                color = accent
                            )
                            Text(
                                text = formatFileSize(info.used) + " / " + formatFileSize(info.total),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(110.dp)) {
                                val strokeWidth = 10.dp.toPx()
                                val diameter = min(size.width, size.height)
                                val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
                                val topLeft = Offset(
                                    (size.width - arcSize.width) / 2,
                                    (size.height - arcSize.height) / 2
                                )

                                val dash = floatArrayOf(8f, 6f)
                                val pathEffect = PathEffect.dashPathEffect(dash, 0f)

                                drawArc(
                                    color = Color(0xFF333333),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt, pathEffect = pathEffect)
                                )

                                drawArc(
                                    color = accent,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt, pathEffect = pathEffect)
                                )
                            }
                            Text(
                                text = (animatedProgress * 100).toInt().toString() + "%",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SegmentedBar(progress = animatedProgress, accent = accent)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (categoryData != null) "STORAGE BY CATEGORY" else "ESTIMATED CATEGORIES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            val categories = remember(categoryData) {
                if (categoryData != null) {
                    listOf(
                        StorageCategory("IMAGES", categoryData.images, Color(0xFF00F3FF)),
                        StorageCategory("VIDEOS", categoryData.videos, Color(0xFFE82127)),
                        StorageCategory("AUDIO", categoryData.audio, Color(0xFF00E5A8)),
                        StorageCategory("APKS", categoryData.apks, Color(0xFFFFD166)),
                        StorageCategory("ARCHIVES", categoryData.archives, Color(0xFF9EA7B8))
                    )
                } else {
                    listOf(
                        StorageCategory("IMAGES", (info.total * 0.25f).toLong(), Color(0xFF00F3FF)),
                        StorageCategory("VIDEOS", (info.total * 0.20f).toLong(), Color(0xFFE82127)),
                        StorageCategory("AUDIO", (info.total * 0.15f).toLong(), Color(0xFF00E5A8)),
                        StorageCategory("APKS", (info.total * 0.18f).toLong(), Color(0xFFFFD166)),
                        StorageCategory("ARCHIVES", (info.total * 0.12f).toLong(), Color(0xFF9EA7B8))
                    )
                }
            }

            categories.forEach { category ->
                CategoryItem(category, info.total)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// MODELS

data class StorageCategory(
    val name: String,
    val bytes: Long,
    val color: Color
)

@Composable
private fun CategoryItem(
    category: StorageCategory,
    totalBytes: Long
) {
    val percentage = if (totalBytes > 0) {
        (category.bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(600),
        label = "categoryBar"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ChamferShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatFileSize(category.bytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            SegmentedBar(progress = animatedFraction, accent = category.color)
        }
    }
}

@Composable
private fun SegmentedBar(progress: Float, accent: Color) {
    val segments = 24
    val active = (segments * progress).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .weight(1f)
                    .background(if (index < active) accent else Color(0xFF333333))
            )
        }
    }
}
