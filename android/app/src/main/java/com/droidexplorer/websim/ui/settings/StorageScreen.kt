package com.droidexplorer.websim.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.storage.StorageInfo
import com.droidexplorer.websim.ui.formatFileSize
import com.droidexplorer.websim.ui.theme.backgroundGradient
import kotlin.math.min

/**
 * Data class for storage category information.
 * Contains byte totals for different file categories.
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

    // ✅ MUST be outside Canvas
    val progressColor =
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

    // Dynamic gradient background
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
                text = "Storage",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Overview of your device space",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
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
                                text = "Used space",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatFileSize(info.used),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "of ${formatFileSize(info.total)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(110.dp)) {
                                val strokeWidth = 14.dp.toPx()
                                val diameter = min(size.width, size.height)
                                val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
                                val topLeft = Offset(
                                    (size.width - arcSize.width) / 2,
                                    (size.height - arcSize.height) / 2
                                )

                                drawArc(
                                    color = Color.Gray.copy(alpha = 0.18f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                drawArc(
                                    color = progressColor,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (categoryData != null) "Storage by category" else "Estimated categories",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

        // ─────────────────────────────
        // PHASE-2: REAL DATA FROM MEDIASTORE
        // Data is provided by the ViewModel
        // ─────────────────────────────
        val categories = remember(categoryData) {
            if (categoryData != null) {
                // Use real data from MediaStore
                listOf(
                    StorageCategory("Images", categoryData.images, Color(0xFF5AC8FA)),
                    StorageCategory("Videos", categoryData.videos, Color(0xFFB388FF)),
                    StorageCategory("Audio", categoryData.audio, Color(0xFF00E5A8)),
                    StorageCategory("APKs", categoryData.apks, Color(0xFFFFD166)),
                    StorageCategory("Archives", categoryData.archives, Color(0xFF9EA7B8))
                )
            } else {
                // Fallback to placeholder data
                listOf(
                    StorageCategory("Images", (info.total * 0.25f).toLong(), Color(0xFF5AC8FA)),
                    StorageCategory("Videos", (info.total * 0.20f).toLong(), Color(0xFFB388FF)),
                    StorageCategory("Audio", (info.total * 0.15f).toLong(), Color(0xFF00E5A8)),
                    StorageCategory("APKs", (info.total * 0.18f).toLong(), Color(0xFFFFD166)),
                    StorageCategory("Archives", (info.total * 0.12f).toLong(), Color(0xFF9EA7B8))
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

// ─────────────────────────────
// MODELS
// ─────────────────────────────

data class StorageCategory(
    val name: String,
    val bytes: Long,
    val color: Color
)

// ─────────────────────────────
// CATEGORY ROW
// ─────────────────────────────

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
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
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

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = category.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
