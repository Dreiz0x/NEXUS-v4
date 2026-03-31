package com.nexus.intelligence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.nexus.intelligence.service.indexing.IndexingService
import com.nexus.intelligence.ui.navigation.NexusNavHost
import com.nexus.intelligence.ui.theme.NexusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onPermissionsGranted()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required for NEXUS to index documents",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required for NEXUS to index documents",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        requestStoragePermission()

        setContent {
            NexusTheme {
                NexusNavHost()
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ : MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    storagePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    storagePermissionLauncher.launch(intent)
                }
            } else {
                onPermissionsGranted()
            }
        } else {
            // Android 10 and below
            val readPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (readPermission != PackageManager.PERMISSION_GRANTED ||
                writePermission != PackageManager.PERMISSION_GRANTED
            ) {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            } else {
                onPermissionsGranted()
            }
        }
    }

    private fun onPermissionsGranted() {
        // Start the indexing service with default directories
        val defaultDirs = listOf(
            Environment.getExternalStorageDirectory().absolutePath
        )
        IndexingService.startScan(this, defaultDirs)
    }
}
