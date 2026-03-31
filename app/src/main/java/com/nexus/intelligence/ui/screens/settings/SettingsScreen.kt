package com.nexus.intelligence.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import com.nexus.intelligence.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val apiEndpoint by viewModel.apiEndpoint.collectAsState()
    val monitoredFolders by viewModel.monitoredFolders.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val serverEnabled by viewModel.serverEnabled.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val autoIndexEnabled by viewModel.autoIndexEnabled.collectAsState()
    val apiStatus by viewModel.apiStatus.collectAsState()
    val newFolderPath by viewModel.newFolderPath.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Mostrar snackbar cuando hay mensaje
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearSnackbar()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        HudGridBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NexusColors.Cyan)
                        }
                        Text(
                            text = "CONFIGURATION",
                            style = MaterialTheme.typography.headlineMedium,
                            color = NexusColors.Cyan,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // ── API Endpoint ─────────────────────────────────────
                item {
                    HudSectionHeader(title = "API ENDPOINT")
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCard(borderColor = if (apiStatus) NexusColors.Green else NexusColors.Red) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusIndicator(isOnline = apiStatus, label = if (apiStatus) "CONNECTED" else "DISCONNECTED")
                            TextButton(onClick = { viewModel.testApiConnection() }) {
                                Text("[TEST]", style = MaterialTheme.typography.labelMedium, color = NexusColors.Cyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ENDPOINT URL:", style = MaterialTheme.typography.labelSmall, color = NexusColors.TextDim)
                        Spacer(modifier = Modifier.height(4.dp))
                        HudTextField(
                            value = apiEndpoint,
                            onValueChange = { viewModel.updateApiEndpoint(it) },
                            placeholder = "http://127.0.0.1:8080"
                        )
                    }
                }

                // ── Monitored Folders ────────────────────────────────
                item {
                    HudSectionHeader(title = "MONITORED FOLDERS")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(monitoredFolders) { folder ->
                    MonitoredFolderItem(folder = folder, onRemove = { viewModel.removeMonitoredFolder(folder.path) })
                }

                item {
                    HolographicCard(borderColor = NexusColors.Green) {
                        Text("ADD FOLDER TO MONITOR:", style = MaterialTheme.typography.labelSmall, color = NexusColors.Green.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HudTextField(
                                value = newFolderPath,
                                onValueChange = { viewModel.updateNewFolderPath(it) },
                                placeholder = "/storage/emulated/0/Documents",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.addMonitoredFolder() }) {
                                Text("[ADD]", style = MaterialTheme.typography.labelMedium, color = NexusColors.Green)
                            }
                        }
                    }
                }

                // ── Network Server ───────────────────────────────────
                item {
                    HudSectionHeader(title = "NETWORK SERVER")
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCard(borderColor = NexusColors.Magenta) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("LOCAL SERVER", style = MaterialTheme.typography.labelLarge, color = NexusColors.Magenta)
                                Text("Share documents on local network", style = MaterialTheme.typography.bodySmall, color = NexusColors.TextDim)
                            }
                            Switch(
                                checked = serverEnabled,
                                onCheckedChange = { viewModel.toggleServer(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NexusColors.Magenta,
                                    checkedTrackColor = NexusColors.MagentaGlow,
                                    uncheckedThumbColor = NexusColors.TextDim,
                                    uncheckedTrackColor = NexusColors.CardBackground
                                )
                            )
                        }
                        if (serverEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("PORT:", style = MaterialTheme.typography.labelSmall, color = NexusColors.TextDim)
                            HudTextField(value = serverPort, onValueChange = { viewModel.updateServerPort(it) }, placeholder = "9090")
                        }
                    }
                }

                // ── General ──────────────────────────────────────────
                item {
                    HudSectionHeader(title = "GENERAL")
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCard(borderColor = NexusColors.Cyan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("AUTO-INDEX", style = MaterialTheme.typography.labelLarge, color = NexusColors.Cyan)
                                Text("Scan monitored folders automatically", style = MaterialTheme.typography.bodySmall, color = NexusColors.TextDim)
                            }
                            Switch(
                                checked = autoIndexEnabled,
                                onCheckedChange = { viewModel.toggleAutoIndex(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NexusColors.Cyan,
                                    checkedTrackColor = NexusColors.CyanGlow,
                                    uncheckedThumbColor = NexusColors.TextDim,
                                    uncheckedTrackColor = NexusColors.CardBackground
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("SOUND EFFECTS", style = MaterialTheme.typography.labelLarge, color = NexusColors.Cyan)
                                Text("Sci-fi interface sounds", style = MaterialTheme.typography.bodySmall, color = NexusColors.TextDim)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { viewModel.toggleSound(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NexusColors.Cyan,
                                    checkedTrackColor = NexusColors.CyanGlow,
                                    uncheckedThumbColor = NexusColors.TextDim,
                                    uncheckedTrackColor = NexusColors.CardBackground
                                )
                            )
                        }
                    }
                }

                // ── Danger Zone ──────────────────────────────────────
                item {
                    HudSectionHeader(title = "DANGER ZONE", color = NexusColors.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCard(borderColor = NexusColors.Red) {
                        TextButton(onClick = { viewModel.clearIndex() }) {
                            Text("[CLEAR ENTIRE INDEX]", style = MaterialTheme.typography.labelLarge, color = NexusColors.Red, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Elimina todos los documentos indexados de la DB. Los archivos en disco no se tocan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusColors.TextDim
                        )
                    }
                }

                item {
                    HudSectionHeader(title = "ABOUT")
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCard(borderColor = NexusColors.TextDim) {
                        Text("NEXUS v1.1.0", style = MaterialTheme.typography.labelLarge, color = NexusColors.Cyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Personal Document Intelligence System", style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
                        Text("100% Local // Zero Telemetry // Zero External Servers", style = MaterialTheme.typography.labelSmall, color = NexusColors.Green.copy(alpha = 0.5f))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MonitoredFolderItem(folder: MonitoredFolderEntity, onRemove: () -> Unit) {
    HolographicCard(modifier = Modifier.fillMaxWidth(), borderColor = NexusColors.Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = NexusColors.Cyan, modifier = Modifier.size(16.dp))
                Column {
                    if (folder.label.isNotBlank()) {
                        Text(folder.label, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextPrimary)
                    }
                    Text(folder.path, style = MaterialTheme.typography.labelSmall, color = NexusColors.TextDim)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = NexusColors.Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun HudTextField(value: String, onValueChange: (String) -> Unit, placeholder: String = "", modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(8.dp),
        textStyle = TextStyle(fontFamily = NexusMonospace, fontSize = 12.sp, color = NexusColors.TextPrimary, letterSpacing = 0.5.sp),
        singleLine = true,
        cursorBrush = SolidColor(NexusColors.Cyan),
        decorationBox = { innerTextField ->
            androidx.compose.foundation.layout.Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = TextStyle(fontFamily = NexusMonospace, fontSize = 12.sp, color = NexusColors.TextDim))
                }
                innerTextField()
            }
        }
    )
}
