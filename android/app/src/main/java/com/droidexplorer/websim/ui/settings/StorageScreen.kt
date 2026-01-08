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

            Spacer(Modifier.height(24.dp))

        // ─────────────────────────────
        // STORAGE RING (Hero)
        // ─────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 24.dp.toPx()
                val diameter = min(size.width, size.height)
                val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
                val topLeft = Offset(
                    (size.width - arcSize.width) / 2,
                    (size.height - arcSize.height) / 2
                )

                // Background
                drawArc(
                    color = Color.Gray.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        }

        Spacer(Modifier.height(32.dp))

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
                    StorageCategory("Images", categoryData.images, Color(0xFF2196F3)),
                    StorageCategory("Videos", categoryData.videos, Color(0xFF9C27B0)),
                    StorageCategory("Audio", categoryData.audio, Color(0xFF009688)),
                    StorageCategory("APKs", categoryData.apks, Color(0xFFFF9800)),
                    StorageCategory("Archives", categoryData.archives, Color(0xFF757575))
                )
            } else {
                // Fallback to placeholder data
                listOf(
                    StorageCategory("Images", (info.total * 0.25f).toLong(), Color(0xFF2196F3)),
                    StorageCategory("Videos", (info.total * 0.20f).toLong(), Color(0xFF9C27B0)),
                    StorageCategory("Audio", (info.total * 0.15f).toLong(), Color(0xFF009688)),
                    StorageCategory("APKs", (info.total * 0.18f).toLong(), Color(0xFFFF9800)),
                    StorageCategory("Archives", (info.total * 0.12f).toLong(), Color(0xFF757575))
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
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {}

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(4.dp),
                    color = category.color.copy(alpha = 0.85f)
                ) {}
            }
        }
    }
}
