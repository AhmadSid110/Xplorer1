package com.droidexplorer.websim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.droidexplorer.websim.ui.DualPaneScreen
import com.droidexplorer.websim.ui.theme.XplorerTheme

class MainActivity : ComponentActivity() {
    
    private var hasStoragePermission by mutableStateOf(false)
    private var showPermissionRationale by mutableStateOf(false)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.all { it.value }
        if (!hasStoragePermission) {
            showPermissionRationale = true
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            XplorerTheme {
                if (hasStoragePermission) {
                    DualPaneScreen(singlePane = false)
                } else {
                    PermissionScreen(
                        showRationale = showPermissionRationale,
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 - READ_EXTERNAL_STORAGE only
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 10 and below
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        hasStoragePermission = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasStoragePermission) {
            permissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun PermissionScreen(
    showRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (showRationale) {
                    "Xplorer needs storage access to browse and manage your files. Please grant permission in settings."
                } else {
                    "Xplorer needs permission to access your files and folders."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
