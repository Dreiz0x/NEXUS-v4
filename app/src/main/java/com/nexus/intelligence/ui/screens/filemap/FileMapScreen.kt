package com.nexus.intelligence.ui.screens.filemap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import com.nexus.intelligence.ui.viewmodel.FileMapViewModel

@Composable
fun FileMapScreen(
    viewModel: FileMapViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onDocumentClick: (DocumentInfo) -> Unit
) {
    val directories by viewModel.directories.collectAsState()
    val selectedDirectory by viewModel.selectedDirectory.collectAsState()
    val directoryDocuments by viewModel.directoryDocuments.collectAsState()
    val expandedDirs by viewModel.expandedDirs.collectAsState()

    HudGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ── Header ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NexusColors.Cyan
                    )
                }
                Text(
                    text = "FILE MAP",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NexusColors.Cyan,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HudSectionHeader(title = "STORAGE TOPOLOGY")

            Spacer(modifier = Modifier.height(8.dp))

            // ── Directory Tree ────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(directories) { dirPath ->
                    DirectoryNode(
                        path = dirPath,
                        isExpanded = expandedDirs.contains(dirPath),
                        isSelected = selectedDirectory == dirPath,
                        onToggle = { viewModel.toggleDirectory(dirPath) },
                        onSelect = { viewModel.selectDirectory(dirPath) }
                    )

                    // Show documents if expanded and selected
                    AnimatedVisibility(
                        visible = expandedDirs.contains(dirPath) && selectedDirectory == dirPath
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            directoryDocuments.forEach { doc ->
                                FileMapDocumentItem(
                                    document = doc,
                                    onClick = { onDocumentClick(doc) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// ── Directory Node ───────────────────────────────────────────────

@Composable
private fun DirectoryNode(
    path: String,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit
) {
    val color = if (isSelected) NexusColors.Cyan else NexusColors.TextSecondary
    val dirName = path.substringAfterLast("/").ifEmpty { path }

    HolographicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = color,
        onClick = {
            onToggle()
            onSelect()
        }
    ) {
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
                // Connection line indicator
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = color,
                        radius = 3f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = 6f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = dirName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Show full path on selection
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = path,
                style = MaterialTheme.typography.labelSmall,
                color = NexusColors.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── File Map Document Item ───────────────────────────────────────

@Composable
private fun FileMapDocumentItem(
    document: DocumentInfo,
    onClick: () -> Unit
) {
    val typeColor = getDocTypeColor(document.fileType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Connection line
        Canvas(modifier = Modifier.size(8.dp, 16.dp)) {
            drawLine(
                color = typeColor.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1f
            )
            drawCircle(
                color = typeColor,
                radius = 2f,
                center = Offset(size.width, size.height / 2)
            )
        }

        DocumentTypeBadge(type = document.fileType)

        Text(
            text = document.fileName,
            style = MaterialTheme.typography.bodySmall,
            color = NexusColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = document.fileSizeFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = NexusColors.TextDim
        )
    }
}
