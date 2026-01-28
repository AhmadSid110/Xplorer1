package com.droidexplorer.websim.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.lastModified
import com.droidexplorer.websim.file.safeSize
import com.droidexplorer.websim.ui.effects.rememberPulseAlpha
import com.droidexplorer.websim.ui.theme.ChamferShape
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.cyberGlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd // HH:mm", Locale.getDefault()) }
    val sizeText = remember(file.uniqueKey) { formatFileSize(file.safeSize()) }
    val dateText = remember(file.uniqueKey) { dateFormat.format(Date(file.lastModified())).uppercase() }
    val accent = LocalCyberAccent.current
    val pulse = rememberPulseAlpha(isSelected)

    val highlightColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.05f) else Color.Transparent,
        label = "fileRowSelection"
    )
    val iconSize = 24.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor, ChamferShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.dp, accent.copy(alpha = 0.6f + (0.2f * pulse)), ChamferShape(8.dp))
                        .cyberGlow(accent, intensity = 0.2f + (0.1f * pulse))
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileIcon(
                file = file,
                size = iconSize,
                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.uppercase(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        letterSpacing = 1.sp,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val labelStyle = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (!file.isDirectory) {
                        Text(text = "SIZE: $sizeText", style = labelStyle)
                    } else {
                        Text(text = "TYPE: DIR", style = labelStyle)
                    }
                    Text(text = "MOD: $dateText", style = labelStyle)
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
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", size.toDouble() / (1024 * 1024 * 1024))
    }
}
