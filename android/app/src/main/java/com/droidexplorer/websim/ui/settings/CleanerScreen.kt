package com.droidexplorer.websim.ui.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.droidexplorer.websim.ui.theme.DividerSoft
import com.droidexplorer.websim.ui.theme.LocalCyberAccent
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

private enum class CleanerDestination {
    HOME,
    SMART_RESULTS,
    CATEGORY
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
    var destination by rememberSaveable { mutableStateOf(CleanerDestination.HOME) }
    var activeCategory by rememberSaveable { mutableStateOf<CleanerCategory?>(null) }
    var smartScanTrigger by remember { mutableStateOf(0) }
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

    fun startScan(category: CleanerCategory, clearSelection: Boolean = true) {
        if (scanning[category] == true) return
        scanning[category] = true
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                scanCategoryFiles(context, category, protectAndroid, protectDcim)
            }
            categoryFiles[category] = results
            categorySizes[category] = results.sumOf { it.length() }
            if (clearSelection) {
                selectedFiles.clear()
            }
            scanning[category] = false
        }
    }

    fun startSmartScan() {
        smartScanTrigger++
        val categoriesToScan = categories.map { it.category }
        categoriesToScan.forEach { scanning[it] = true }
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                categoriesToScan.associateWith { category ->
                    scanCategoryFiles(context, category, protectAndroid, protectDcim)
                }
            }
            results.forEach { (category, files) ->
                categoryFiles[category] = files
                categorySizes[category] = files.sumOf { it.length() }
                scanning[category] = false
            }
        }
    }

    fun removeFilesFromAllCategories(files: List<File>) {
        val toRemove = files.map { it.absolutePath }.toSet()
        categories.forEach { spec ->
            val current = categoryFiles[spec.category].orEmpty()
            val updated = current.filterNot { toRemove.contains(it.absolutePath) }
            categoryFiles[spec.category] = updated
            categorySizes[spec.category] = updated.sumOf { it.length() }
        }
    }

    val totalJunk = categorySizes.values.sum()
    val estimatedTotal = if (totalJunk > 0) {
        totalJunk
    } else {
        data.images + data.videos + data.audio + data.apks + data.archives
    }
    LaunchedEffect(destination, activeCategory) {
        selectedFiles.clear()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (destination) {
            CleanerDestination.HOME -> LazyColumn(
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Review and delete safely",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.4f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("JUNK FOUND", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (totalJunk > 0) formatSize(totalJunk) else "--",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatSize(storageInfo.used)} used · ${formatSize(storageInfo.free)} free",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lastCleaned > 0) {
                            Text(
                                text = "Last cleaned ${formatTimestamp(lastCleaned)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = usedFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            accent.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SMART SUGGESTION", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (estimatedTotal > 0) "Clear ${formatSize(estimatedTotal)} safely" else "Run a smart scan",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                destination = CleanerDestination.SMART_RESULTS
                                startSmartScan()
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Protect Android/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
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
                        Text("Protect DCIM/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(categories.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { spec ->
                        val size = categorySizes[spec.category]
                        val isSelected = activeCategory == spec.category && destination == CleanerDestination.HOME
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
                                activeCategory = spec.category
                                destination = CleanerDestination.CATEGORY
                                onOpenCategory(spec.category)
                            },
                            onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeCategory = spec.category
                                destination = CleanerDestination.CATEGORY
                                onOpenCategory(spec.category)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            }

            CleanerDestination.SMART_RESULTS -> {
                val allFiles = categories
                    .flatMap { categoryFiles[it.category].orEmpty() }
                    .distinctBy { it.absolutePath }
                CleanerResultsScreen(
                    title = "Smart scan",
                    subtitle = "${allFiles.size} files · ${formatSize(allFiles.sumOf { it.length() })}",
                    files = allFiles,
                    isScanning = scanning.values.any { it },
                    accent = accent,
                    onBack = { destination = CleanerDestination.HOME },
                    isSelected = { file -> selectedFiles[file.absolutePath] == true },
                    onSelectAll = {
                        val selectAll = allFiles.any { selectedFiles[it.absolutePath] != true }
                        allFiles.forEach { selectedFiles[it.absolutePath] = selectAll }
                    },
                    onClearSelection = { selectedFiles.clear() },
                    onExcludeSelected = {
                        val toExclude = allFiles.filter { selectedFiles[it.absolutePath] == true }
                        removeFilesFromAllCategories(toExclude)
                        selectedFiles.clear()
                    },
                    onDeleteSelected = { showConfirmDialog = true },
                    onToggleFile = { file, checked -> selectedFiles[file.absolutePath] = checked }
                )
            }

            CleanerDestination.CATEGORY -> {
                val category = activeCategory
                if (category != null) {
                    val files = categoryFiles[category].orEmpty()
                    LaunchedEffect(category, smartScanTrigger) {
                        if (files.isEmpty() && scanning[category] != true) {
                            startScan(category, clearSelection = false)
                        }
                    }
                    CleanerResultsScreen(
                        title = category.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                        subtitle = "${files.size} files · ${formatSize(files.sumOf { it.length() })}",
                        files = files,
                        isScanning = scanning[category] == true,
                        accent = accent,
                        onBack = { destination = CleanerDestination.HOME },
                        isSelected = { file -> selectedFiles[file.absolutePath] == true },
                        onSelectAll = {
                            val selectAll = files.any { selectedFiles[it.absolutePath] != true }
                            files.forEach { selectedFiles[it.absolutePath] = selectAll }
                        },
                        onClearSelection = { selectedFiles.clear() },
                        onExcludeSelected = {
                            val toExclude = files.filter { selectedFiles[it.absolutePath] == true }
                            val updated = files - toExclude.toSet()
                            categoryFiles[category] = updated
                            categorySizes[category] = updated.sumOf { it.length() }
                            selectedFiles.clear()
                        },
                        onDeleteSelected = { showConfirmDialog = true },
                        onToggleFile = { file, checked -> selectedFiles[file.absolutePath] = checked }
                    )
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

    val currentFilesForDeletion = when (destination) {
        CleanerDestination.SMART_RESULTS -> categories
            .flatMap { categoryFiles[it.category].orEmpty() }
            .distinctBy { it.absolutePath }
        CleanerDestination.CATEGORY -> activeCategory?.let { categoryFiles[it].orEmpty() }.orEmpty()
        CleanerDestination.HOME -> emptyList()
    }

    if (showConfirmDialog) {
        val selectedCount = selectedFiles.values.count { it }
        val selectedSize = currentFilesForDeletion.filter { selectedFiles[it.absolutePath] == true }
            .sumOf { it.length() }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete permanently?") },
            text = { Text("Delete $selectedCount files (${formatSize(selectedSize)}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showConfirmDialog = false
                        isProcessing = true
                        scope.launch {
                            val toDelete = currentFilesForDeletion.filter {
                                selectedFiles[it.absolutePath] == true
                            }
                            val deletedCount = withContext(Dispatchers.IO) {
                                toDelete.count { it.delete() }
                            }
                            if (destination == CleanerDestination.SMART_RESULTS) {
                                removeFilesFromAllCategories(toDelete)
                            } else if (destination == CleanerDestination.CATEGORY) {
                                activeCategory?.let { category ->
                                    val updated = (categoryFiles[category].orEmpty() - toDelete.toSet())
                                    categoryFiles[category] = updated
                                    categorySizes[category] = updated.sumOf { it.length() }
                                }
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

@Composable
private fun CleanerResultsScreen(
    title: String,
    subtitle: String,
    files: List<File>,
    isScanning: Boolean,
    accent: Color,
    onBack: () -> Unit,
    isSelected: (File) -> Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onExcludeSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleFile: (File, Boolean) -> Unit
) {
    val selectedCount = files.count { isSelected(it) }
    val selectedSize = files.filter { isSelected(it) }.sumOf { it.length() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isScanning) {
                    IndeterminateArc(
                        modifier = Modifier.size(28.dp),
                        color = accent,
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected $selectedCount (${formatSize(selectedSize)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSelectAll) { Text("Select all", color = accent) }
                TextButton(onClick = onClearSelection) { Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExcludeSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exclude")
                }
                Button(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.15f),
                        contentColor = accent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }

        if (files.isEmpty() && !isScanning) {
            item {
                Text(
                    text = "No files found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(files) { file ->
                CleanerFileRow(
                    file = file,
                    checked = isSelected(file),
                    accent = accent,
                    onCheckedChange = { onToggleFile(file, it) }
                )
            }
        }
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
                        MaterialTheme.colorScheme.surface
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
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sizeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
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
