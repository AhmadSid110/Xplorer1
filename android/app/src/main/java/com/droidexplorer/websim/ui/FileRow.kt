package com.droidexplorer.websim.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.lastModified
import com.droidexplorer.websim.file.safeSize
import com.droidexplorer.websim.ui.effects.rememberPulseAlpha
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.TextMuted
import com.droidexplorer.websim.ui.theme.cyberGlow
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
    val sizeText = remember(file.uniqueKey) { formatFileSize(file.safeSize()) }
    val dateText = remember(file.uniqueKey) { dateFormat.format(Date(file.lastModified())) }
    val accent = LocalCyberAccent.current
    val pulse = rememberPulseAlpha(isSelected)
    
    // Animated background with spring animation for smoother transitions
    val highlightColor by animateColorAsState(
        targetValue = if (isSelected) {
            accent.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fileRowSelection"
    )
    val iconSize = 28.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor, RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.5f + (0.2f * pulse)),
                            RoundedCornerShape(12.dp)
                        )
                        .cyberGlow(accent, intensity = 0.35f + (0.25f * pulse))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileIcon(
                file = file,
                size = iconSize,
                tint = null
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = file.name,
                        style = if (file.isDirectory) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (file is FsNode.TorBox) 0.7f else 1f
                        )
                    )
                    if (file is FsNode.TorBox) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Remote file - read only",
                            tint = accent.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (file is FsNode.TorBox) {
                        Text(
                            text = "Remote (read-only)",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (!file.isDirectory) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (file !is FsNode.TorBox) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (requiresPermission) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Permission required",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Divider(
            color = DividerSoft,
            modifier = Modifier.padding(start = 40.dp)
        )
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
