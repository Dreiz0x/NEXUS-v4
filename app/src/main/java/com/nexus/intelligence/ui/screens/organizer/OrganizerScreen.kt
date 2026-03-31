package com.nexus.intelligence.ui.screens.organizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.FileCluster
import com.nexus.intelligence.ui.viewmodel.OrganizerViewModel

// ── Colores del tema NEXUS ───────────────────────────────────────
private val NexusBg = Color(0xFF0A0A0A)
private val NexusSurface = Color(0xFF121212)
private val NexusCard = Color(0xFF1A1A1A)
private val NexusTeal = Color(0xFF00E5CC)
private val NexusRed = Color(0xFFFF2D55)
private val NexusOrange = Color(0xFFFF6B35)
private val NexusGray = Color(0xFF555555)
private val NexusTextPrimary = Color(0xFFE0E0E0)
private val NexusTextSecondary = Color(0xFF888888)

@Composable
fun OrganizerScreen(
    viewModel: OrganizerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────
            OrganizerHeader()

            // ── Carpeta maestra ──────────────────────────────────
            MasterFolderInput(
                value = state.masterFolder,
                onValueChange = viewModel::setMasterFolder
            )

            // ── Botón analizar ───────────────────────────────────
            if (state.clusters.isEmpty() && !state.isAnalyzing) {
                AnalyzeButton(onClick = viewModel::analyzeAllDocuments)
            }

            // ── Progreso de análisis ─────────────────────────────
            AnimatedVisibility(visible = state.isAnalyzing) {
                AnalyzingProgress(message = state.analysisProgress)
            }

            // ── Lista de clusters ────────────────────────────────
            if (state.clusters.isNotEmpty()) {
                ClusterSummaryBar(clusters = state.clusters)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.clusters, key = { it.id }) { cluster ->
                        ClusterCard(
                            cluster = cluster,
                            onToggleApproval = { viewModel.toggleClusterApproval(cluster.id) },
                            onRename = { newName -> viewModel.renameCluster(cluster.id, newName) },
                            onRemoveDocument = { docId ->
                                viewModel.removeDocumentFromCluster(cluster.id, docId)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // ── Botón aplicar ────────────────────────────────
                ApplyBar(
                    isApplying = state.isApplying,
                    approvedCount = state.clusters.count { it.isApproved },
                    onClick = viewModel::applyOrganization
                )
            }

            // ── Error ────────────────────────────────────────────
            state.error?.let { error ->
                ErrorBanner(message = error, onDismiss = viewModel::clearResult)
            }
        }

        // ── Resultado aplicación ─────────────────────────────────
        state.applyResult?.let { result ->
            ResultDialog(
                result = result,
                onDismiss = viewModel::clearResult
            )
        }
    }
}

// ── Header ───────────────────────────────────────────────────────

@Composable
private fun OrganizerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ORGANIZER",
            color = NexusTeal,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "NEXUS",
            color = NexusGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }

    Divider(color = NexusTeal.copy(alpha = 0.2f), thickness = 1.dp)
}

// ── Carpeta maestra ───────────────────────────────────────────────

@Composable
private fun MasterFolderInput(value: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "CARPETA MAESTRA",
            color = NexusTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexusTeal.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "/storage/emulated/0/Documentos/NEXUS",
                    color = NexusGray,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = NexusTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Botón analizar ────────────────────────────────────────────────

@Composable
private fun AnalyzeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NexusTeal),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = NexusBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ANALIZAR DOCUMENTOS",
                color = NexusBg,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

// ── Progreso ──────────────────────────────────────────────────────

@Composable
private fun AnalyzingProgress(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = NexusTeal, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message.ifEmpty { "Analizando..." },
            color = NexusTeal,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ── Barra resumen ─────────────────────────────────────────────────

@Composable
private fun ClusterSummaryBar(clusters: List<FileCluster>) {
    val approved = clusters.count { it.isApproved }
    val totalFiles = clusters.sumOf { it.fileCount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryChip("${clusters.size} GRUPOS", NexusTeal)
        SummaryChip("$totalFiles ARCHIVOS", NexusTextSecondary)
        SummaryChip("$approved APROBADOS", if (approved > 0) NexusOrange else NexusGray)
    }
}

@Composable
private fun SummaryChip(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

// ── Cluster Card ──────────────────────────────────────────────────

@Composable
private fun ClusterCard(
    cluster: FileCluster,
    onToggleApproval: () -> Unit,
    onRename: (String) -> Unit,
    onRemoveDocument: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(cluster.folderName) }

    val borderColor = if (cluster.isApproved) NexusTeal.copy(alpha = 0.5f) else NexusGray.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(NexusCard)
    ) {
        // ── Cabecera del cluster ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox aprobación
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.5.dp, if (cluster.isApproved) NexusTeal else NexusGray, RoundedCornerShape(3.dp))
                    .background(if (cluster.isApproved) NexusTeal.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onToggleApproval() },
                contentAlignment = Alignment.Center
            ) {
                if (cluster.isApproved) {
                    Text("✓", color = NexusTeal, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Nombre de carpeta (editable)
            if (editingName) {
                BasicTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = NexusTeal,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        onRename(nameInput)
                        editingName = false
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar", tint = NexusTeal, modifier = Modifier.size(16.dp))
                }
            } else {
                Text(
                    text = "📁 ${cluster.folderName}",
                    color = if (cluster.isApproved) NexusTextPrimary else NexusGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        nameInput = cluster.folderName
                        editingName = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NexusGray, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Contador
            Text(
                text = "${cluster.fileCount}",
                color = NexusTeal,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NexusGray,
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Lista de archivos ────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexusBg.copy(alpha = 0.5f))
            ) {
                Divider(color = NexusGray.copy(alpha = 0.2f))
                cluster.documents.forEach { doc ->
                    DocumentRow(
                        doc = doc,
                        onRemove = { onRemoveDocument(doc.id) }
                    )
                }
            }
        }
    }
}

// ── Fila de documento dentro del cluster ─────────────────────────

@Composable
private fun DocumentRow(doc: DocumentInfo, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = doc.fileTypeIcon,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.fileName,
                color = NexusTextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = doc.fileSizeFormatted,
                color = NexusTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Quitar del grupo",
                tint = NexusRed.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Barra inferior aplicar ────────────────────────────────────────

@Composable
private fun ApplyBar(isApplying: Boolean, approvedCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusSurface)
            .padding(12.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = !isApplying && approvedCount > 0,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NexusOrange,
                disabledContainerColor = NexusGray
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    color = NexusBg,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Done, contentDescription = null, tint = NexusBg)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isApplying) "APLICANDO..." else "APLICAR $approvedCount GRUPOS",
                color = NexusBg,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Error Banner ──────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusRed.copy(alpha = 0.15f))
            .border(1.dp, NexusRed.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚠", color = NexusRed, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = NexusRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = NexusRed, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Diálogo resultado ─────────────────────────────────────────────

@Composable
private fun ResultDialog(
    result: com.nexus.intelligence.domain.model.ApplyResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexusCard,
        title = {
            Text("RESULTADO", color = NexusTeal, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        },
        text = {
            Column {
                ResultRow("Movidos", result.moved.toString(), NexusTeal)
                ResultRow("Renombrados", result.renamed.toString(), NexusOrange)
                if (result.failed > 0) {
                    ResultRow("Fallidos", result.failed.toString(), NexusRed)
                    result.failedFiles.take(3).forEach { file ->
                        Text("  • $file", color = NexusGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = NexusTeal, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun ResultRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NexusTextSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
