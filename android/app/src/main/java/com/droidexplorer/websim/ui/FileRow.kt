package com.droidexplorer.websim.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.lastModified
import com.droidexplorer.websim.file.size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays a single file or directory as a row.
 * 
 * CRITICAL VISIBILITY RULE: This composable renders the file unconditionally.
 * DO NOT add any logic to skip rendering based on extension, MIME type, or category.
 * 
 * @param file The file to render
 * @param isSelected Whether the file is currently selected
 * @param onClick Callback when the row is clicked
 * @param onLongClick Callback when the row is long-pressed
 * @param requiresPermission Whether the file requires permission to access
 * @param modifier Optional modifier for the row
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FsNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    requiresPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val sizeText = remember(file.uniqueKey) { formatFileSize(file.size()) }
    val dateText = remember(file.uniqueKey) { dateFormat.format(Date(file.lastModified())) }
    
    // Animated background with spring animation for smoother transitions
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fileRowSelection"
    )
    val iconSize = 18.dp

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with fade + scale animation
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(100)),
                exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.9f, animationSpec = tween(100))
            ) {
                FileIcon(
                    file = file,
                    size = iconSize
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = if (file.isDirectory) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!file.isDirectory) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (requiresPermission) {
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(100)),
                    exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.9f, animationSpec = tween(100))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Permission required",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}
