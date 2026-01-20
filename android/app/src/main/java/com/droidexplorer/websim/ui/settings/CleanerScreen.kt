package com.droidexplorer.websim.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.file.FileManager
import com.droidexplorer.websim.storage.StorageInfoProvider
import com.droidexplorer.websim.ui.glass.neonGlass
import com.droidexplorer.websim.ui.theme.DarkSurfaceAlt
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.NeonCyan
import com.droidexplorer.websim.ui.theme.NeonPink
import com.droidexplorer.websim.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class CleanerCategory {
    LARGE_FILES,
    UNUSED_APKS,
    ARCHIVES,
    VIDEOS,
    IMAGES,
    AUDIO,
    EMPTY_FOLDERS
}

@Composable
fun CleanerScreen(
    data: StorageCategoryData,
    onClose: () -> Unit,
    onOpenCategory: (CleanerCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val storageInfo = remember { StorageInfoProvider().internalStorage() }
    val usedFraction = if (storageInfo.total > 0) {
        (storageInfo.used.toFloat() / storageInfo.total.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isProcessing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var largeFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CleanerCategory?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateMapOf<String, Boolean>() }

    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear space",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Review storage and clean safely",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neonGlass()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Storage usage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${formatSize(storageInfo.used)} used · ${formatSize(storageInfo.free)} free",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = usedFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = NeonCyan,
                        trackColor = DarkSurfaceAlt
                    )
                }
            }

            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = { selectedCategory = CleanerCategory.IMAGES },
                            label = { Text("Images • ${formatSize(data.images)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceAlt
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { selectedCategory = CleanerCategory.VIDEOS },
                            label = { Text("Videos • ${formatSize(data.videos)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceAlt
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { selectedCategory = CleanerCategory.UNUSED_APKS },
                            label = { Text("APKs • ${formatSize(data.apks)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceAlt
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Smart suggestions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SuggestionRow(
                    title = "Empty folders",
                    subtitle = "Remove unused directories",
                    icon = Icons.Outlined.FolderDelete,
                    enabled = !isProcessing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isProcessing = true
                        scope.launch {
                            val count = withContext(Dispatchers.IO) {
                                FileManager.deleteEmptyFolders("/storage/emulated/0")
                            }
                            Toast.makeText(context, "Deleted $count empty folders", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                )
            }

            item {
                SuggestionRow(
                    title = "App cache",
                    subtitle = "Clear cached temporary files",
                    icon = Icons.Outlined.Delete,
                    enabled = !isProcessing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isProcessing = true
                        scope.launch {
                            val cachePath = context.cacheDir.absolutePath
                            val count = withContext(Dispatchers.IO) {
                                FileManager.clearCache(cachePath)
                            }
                            Toast.makeText(context, "Cleared $count cache items", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                )
            }

            item {
                Text(
                    text = "Cleaner categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Large files",
                    subtitle = "Files over 100 MB",
                    size = largeFiles.size,
                    totalSize = largeFiles.sumOf { it.length() },
                    icon = Icons.Outlined.VideoFile,
                    accent = NeonPurple,
                    onClick = {
                        selectedCategory = CleanerCategory.LARGE_FILES
                        onOpenCategory(CleanerCategory.LARGE_FILES)
                    }
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Unused APKs",
                    subtitle = "Installed packages you can reinstall",
                    size = 0,
                    totalSize = data.apks,
                    icon = Icons.Outlined.Android,
                    accent = NeonCyan,
                    onClick = {
                        selectedCategory = CleanerCategory.UNUSED_APKS
                        onOpenCategory(CleanerCategory.UNUSED_APKS)
                    }
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Archives",
                    subtitle = "ZIP, RAR and compressed files",
                    size = 0,
                    totalSize = data.archives,
                    icon = Icons.Outlined.Archive,
                    accent = NeonPink,
                    onClick = {
                        selectedCategory = CleanerCategory.ARCHIVES
                        onOpenCategory(CleanerCategory.ARCHIVES)
                    }
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Videos",
                    subtitle = "Media videos",
                    size = 0,
                    totalSize = data.videos,
                    icon = Icons.Outlined.VideoFile,
                    accent = NeonPurple,
                    onClick = {
                        selectedCategory = CleanerCategory.VIDEOS
                        onOpenCategory(CleanerCategory.VIDEOS)
                    }
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Images",
                    subtitle = "Photos and screenshots",
                    size = 0,
                    totalSize = data.images,
                    icon = Icons.Outlined.Image,
                    accent = NeonCyan,
                    onClick = {
                        selectedCategory = CleanerCategory.IMAGES
                        onOpenCategory(CleanerCategory.IMAGES)
                    }
                )
            }

            item {
                CleanerCategoryCard(
                    title = "Audio",
                    subtitle = "Music and recordings",
                    size = 0,
                    totalSize = data.audio,
                    icon = Icons.Outlined.LibraryMusic,
                    accent = NeonPink,
                    onClick = {
                        selectedCategory = CleanerCategory.AUDIO
                        onOpenCategory(CleanerCategory.AUDIO)
                    }
                )
            }

            item {
                AnimatedVisibility(
                    visible = selectedCategory != null,
                    enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 3 }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selectedCategory == CleanerCategory.LARGE_FILES) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neonGlass()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Scan large files", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Preview before deleting",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isScanning = true
                                            scope.launch {
                                                val results = withContext(Dispatchers.IO) {
                                                    FileManager.findLargeFiles("/storage/emulated/0")
                                                }
                                                largeFiles = results
                                                selectedFiles.clear()
                                                isScanning = false
                                            }
                                        },
                                        enabled = !isScanning && !isProcessing,
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                    ) {
                                        Text("Scan")
                                    }
                                }

                                if (isScanning) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }

                            if (largeFiles.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    largeFiles.forEach { file ->
                                        val isChecked = selectedFiles[file.absolutePath] == true
                                        CleanerFileRow(
                                            file = file,
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedFiles[file.absolutePath] = checked
                                            }
                                        )
                                    }
                                }
                            } else if (!isScanning) {
                                Text(
                                    text = "No large files found yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Preview for this category is coming soon.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                val selectedCount = selectedFiles.values.count { it }
                val selectedSize = largeFiles.filter { selectedFiles[it.absolutePath] == true }
                    .sumOf { it.length() }

                AnimatedVisibility(
                    visible = selectedCount > 0,
                    enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neonGlass()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Delete $selectedCount files (${formatSize(selectedSize)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clean safely")
                        }
                    }
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (showConfirmDialog) {
        val selectedCount = selectedFiles.values.count { it }
        val selectedSize = largeFiles.filter { selectedFiles[it.absolutePath] == true }
            .sumOf { it.length() }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            modifier = Modifier.neonGlass(),
            containerColor = Color.Transparent,
            title = { Text("Delete permanently?") },
            text = {
                Text("Delete $selectedCount files (${formatSize(selectedSize)}).")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isProcessing = true
                        showConfirmDialog = false
                        scope.launch {
                            val toDelete = largeFiles.filter {
                                selectedFiles[it.absolutePath] == true
                            }
                            val deletedCount = withContext(Dispatchers.IO) {
                                toDelete.count { it.delete() }
                            }
                            largeFiles = largeFiles - toDelete.toSet()
                            selectedFiles.clear()
                            Toast.makeText(
                                context,
                                "Deleted $deletedCount files",
                                Toast.LENGTH_SHORT
                            ).show()
                            isProcessing = false
                        }
                    },
                    enabled = selectedCount > 0
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlass()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(NeonCyan.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text("Review")
        }
    }
}

@Composable
fun CleanerCategoryCard(
    title: String,
    subtitle: String,
    size: Int,
    totalSize: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent)
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                if (size > 0) {
                    Text(
                        "$size items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                formatSize(totalSize),
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun CleanerFileRow(
    file: File,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.parent ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatSize(file.length()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Divider(color = DividerSoft)
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
