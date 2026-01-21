package com.droidexplorer.websim.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.VideoFile
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
import com.droidexplorer.websim.ui.effects.IndeterminateArc
import com.droidexplorer.websim.ui.theme.CyberDarkSurface
import com.droidexplorer.websim.ui.theme.CyberElevated
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
import com.droidexplorer.websim.ui.theme.TextMuted
import com.droidexplorer.websim.ui.theme.TextPrimary
import com.droidexplorer.websim.ui.theme.cyberGlow
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
    CACHE
}

private data class CleanerCategorySpec(
    val category: CleanerCategory,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

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
    val accent = LocalCyberAccent.current
    val prefs = remember { context.getSharedPreferences("cleaner_prefs", Context.MODE_PRIVATE) }

    val storageInfo = remember { StorageInfoProvider().internalStorage() }
    val usedFraction = if (storageInfo.total > 0) {
        (storageInfo.used.toFloat() / storageInfo.total.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CleanerCategory?>(null) }
    var lastCleaned by remember { mutableStateOf(prefs.getLong("last_cleaned", 0L)) }
    var protectAndroid by remember { mutableStateOf(prefs.getBoolean("protect_android", true)) }
    var protectDcim by remember { mutableStateOf(prefs.getBoolean("protect_dcim", false)) }
    val categoryFiles = remember { mutableStateMapOf<CleanerCategory, List<File>>() }
    val categorySizes = remember { mutableStateMapOf<CleanerCategory, Long>() }
    val scanning = remember { mutableStateMapOf<CleanerCategory, Boolean>() }
    val selectedFiles = remember { mutableStateMapOf<String, Boolean>() }

    val categories = remember {
        listOf(
            CleanerCategorySpec(
                CleanerCategory.LARGE_FILES,
                "Large files",
                "> 100 MB",
                Icons.Outlined.VideoFile
            ),
            CleanerCategorySpec(
                CleanerCategory.UNUSED_APKS,
                "APKs",
                "Installers",
                Icons.Outlined.Android
            ),
            CleanerCategorySpec(
                CleanerCategory.ARCHIVES,
                "Archives",
                "ZIP, RAR, 7z",
                Icons.Outlined.Archive
            ),
            CleanerCategorySpec(
                CleanerCategory.VIDEOS,
                "Videos",
                "Media",
                Icons.Outlined.VideoFile
            ),
            CleanerCategorySpec(
                CleanerCategory.IMAGES,
                "Images",
                "Screenshots & photos",
                Icons.Outlined.Image
            ),
            CleanerCategorySpec(
                CleanerCategory.AUDIO,
                "Audio",
                "Music & recordings",
                Icons.Outlined.LibraryMusic
            ),
            CleanerCategorySpec(
                CleanerCategory.CACHE,
                "Cache",
                "App temporary files",
                Icons.Outlined.CleaningServices
            )
        )
    }

    fun startScan(category: CleanerCategory) {
        if (scanning[category] == true) return
        scanning[category] = true
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                scanCategoryFiles(context, category, protectAndroid, protectDcim)
            }
            categoryFiles[category] = results
            categorySizes[category] = results.sumOf { it.length() }
            selectedFiles.clear()
            scanning[category] = false
        }
    }

    val totalJunk = categorySizes.values.sum()
    val previewFiles = selectedCategory?.let { categoryFiles[it].orEmpty() }.orEmpty()
    val estimatedTotal = if (totalJunk > 0) {
        totalJunk
    } else {
        data.images + data.videos + data.audio + data.apks + data.archives
    }
    val smartCategory = remember(data, categorySizes) {
        val fallback = listOf(
            CleanerCategory.ARCHIVES to data.archives,
            CleanerCategory.UNUSED_APKS to data.apks,
            CleanerCategory.VIDEOS to data.videos,
            CleanerCategory.IMAGES to data.images,
            CleanerCategory.AUDIO to data.audio
        ).maxByOrNull { it.second }?.first
        if (categorySizes.isNotEmpty()) {
            categorySizes.maxByOrNull { it.value }?.key ?: fallback
        } else fallback
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkSurface)
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
                            text = "Cleaner",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Review and delete safely",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = accent
                        )
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface, RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.4f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("JUNK FOUND", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (totalJunk > 0) formatSize(totalJunk) else "--",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${formatSize(storageInfo.used)} used · ${formatSize(storageInfo.free)} free",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        if (lastCleaned > 0) {
                            Text(
                                text = "Last cleaned ${formatTimestamp(lastCleaned)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        LinearProgressIndicator(
                            progress = usedFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = accent,
                            trackColor = CyberElevated
                        )
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface, RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SMART SUGGESTION", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (estimatedTotal > 0) "Clear ${formatSize(estimatedTotal)} safely" else "Run a smart scan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                smartCategory?.let {
                                    selectedCategory = it
                                    startScan(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.15f),
                                contentColor = accent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    accent.copy(alpha = 0.6f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text("Scan smart")
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Protected folders",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Protect Android/", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = protectAndroid,
                            onCheckedChange = {
                                protectAndroid = it
                                prefs.edit().putBoolean("protect_android", it).apply()
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Protect DCIM/", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = protectDcim,
                            onCheckedChange = {
                                protectDcim = it
                                prefs.edit().putBoolean("protect_dcim", it).apply()
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMuted
                )
            }

            items(categories.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { spec ->
                        val size = categorySizes[spec.category]
                        val isSelected = selectedCategory == spec.category
                        val isScanning = scanning[spec.category] == true
                        CleanerCategoryCard(
                            spec = spec,
                            accent = accent,
                            sizeLabel = when {
                                isScanning -> "Scanning"
                                size == null -> "Tap to scan"
                                size == 0L -> "No files"
                                else -> formatSize(size)
                            },
                            selected = isSelected,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = spec.category
                                if (size == null || isScanning) {
                                    startScan(spec.category)
                                }
                                onOpenCategory(spec.category)
                            },
                            onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedCategory = spec.category
                                val files = categoryFiles[spec.category].orEmpty()
                                if (files.isNotEmpty()) {
                                    files.forEach { file ->
                                        selectedFiles[file.absolutePath] = true
                                    }
                                } else {
                                    startScan(spec.category)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = selectedCategory != null,
                    enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 3 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextMuted
                        )

                        if (previewFiles.isEmpty()) {
                            Text(
                                text = "No files to preview yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            previewFiles.forEach { file ->
                                val checked = selectedFiles[file.absolutePath] == true
                                CleanerFileRow(
                                    file = file,
                                    checked = checked,
                                    accent = accent,
                                    onCheckedChange = { selectedFiles[file.absolutePath] = it }
                                )
                            }
                        }
                    }
                }
            }

            item {
                val selectedCount = selectedFiles.values.count { it }
                val selectedSize = previewFiles.filter { selectedFiles[it.absolutePath] == true }
                    .sumOf { it.length() }

                AnimatedVisibility(
                    visible = selectedCount > 0,
                    enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberDarkSurface, RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                accent.copy(alpha = 0.35f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Delete $selectedCount files (${formatSize(selectedSize)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showConfirmDialog = true
                            },
                            enabled = selectedCount > 0 && !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.15f),
                                contentColor = accent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    accent.copy(alpha = 0.6f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text("Clean selected")
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
                IndeterminateArc(
                    modifier = Modifier.size(64.dp),
                    color = accent
                )
            }
        }
    }

    if (showConfirmDialog) {
        val selectedCount = selectedFiles.values.count { it }
        val selectedSize = previewFiles.filter { selectedFiles[it.absolutePath] == true }
            .sumOf { it.length() }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = CyberDarkSurface,
            title = { Text("Delete permanently?") },
            text = { Text("Delete $selectedCount files (${formatSize(selectedSize)}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showConfirmDialog = false
                        isProcessing = true
                        scope.launch {
                            val toDelete = previewFiles.filter {
                                selectedFiles[it.absolutePath] == true
                            }
                            val deletedCount = withContext(Dispatchers.IO) {
                                toDelete.count { it.delete() }
                            }
                            selectedCategory?.let { category ->
                                val updated = (categoryFiles[category].orEmpty() - toDelete.toSet())
                                categoryFiles[category] = updated
                                categorySizes[category] = updated.sumOf { it.length() }
                            }
                            selectedFiles.clear()
                            Toast.makeText(
                                context,
                                "Deleted $deletedCount files",
                                Toast.LENGTH_SHORT
                            ).show()
                            lastCleaned = System.currentTimeMillis()
                            prefs.edit().putLong("last_cleaned", lastCleaned).apply()
                            isProcessing = false
                        }
                    },
                    enabled = selectedCount > 0
                ) {
                    Text("Delete", color = accent)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanerCategoryCard(
    spec: CleanerCategorySpec,
    accent: Color,
    sizeLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        CyberDarkSurface
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .then(
                if (selected) {
                    Modifier
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .cyberGlow(accent, intensity = 0.3f)
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(spec.icon, null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(spec.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted
                )
                Text(
                    sizeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun CleanerFileRow(
    file: File,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (checked) accent.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .then(
                if (checked) {
                    Modifier
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .cyberGlow(accent, intensity = 0.25f)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = TextMuted,
                checkmarkColor = CyberDarkSurface
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
            Text(
                text = file.parent ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Text(
            formatSize(file.length()),
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
    }
    Divider(color = DividerSoft)
}

private fun scanCategoryFiles(
    context: android.content.Context,
    category: CleanerCategory,
    protectAndroid: Boolean,
    protectDcim: Boolean
): List<File> {
    val rootPath = "/storage/emulated/0"
    val maxResults = 200
    val extensions = when (category) {
        CleanerCategory.UNUSED_APKS -> setOf("apk")
        CleanerCategory.ARCHIVES -> setOf("zip", "rar", "7z", "tar", "gz")
        CleanerCategory.VIDEOS -> setOf("mp4", "mkv", "avi", "mov", "webm", "3gp")
        CleanerCategory.IMAGES -> setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp")
        CleanerCategory.AUDIO -> setOf("mp3", "wav", "ogg", "flac", "m4a", "aac")
        else -> emptySet()
    }

    fun isProtected(path: String): Boolean {
        if (protectAndroid && path.contains("/Android/")) return true
        if (protectDcim && path.contains("/DCIM/")) return true
        return false
    }

    return when (category) {
        CleanerCategory.LARGE_FILES -> FileManager.findLargeFiles(rootPath)
        CleanerCategory.CACHE -> {
            val cacheRoots = buildList<File> {
                add(context.cacheDir)
                context.externalCacheDir?.let { add(it) }
                context.externalCacheDirs?.filterNotNull()?.let { addAll(it) }
            }
            cacheRoots
                .flatMap { root -> root.walkTopDown().filter { it.isFile && !isProtected(it.absolutePath) }.take(maxResults).toList() }
                .distinctBy { it.absolutePath }
        }
        else -> {
            File(rootPath)
                .walkTopDown()
                .filter { file ->
                    file.isFile && extensions.contains(file.extension.lowercase()) && !isProtected(file.absolutePath)
                }
                .take(maxResults)
                .toList()
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
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
