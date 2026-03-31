
package com.nexus.intelligence.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSearch: (String) -> Unit,
    onDocumentClick: (DocumentInfo) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val indexingProgress by viewModel.indexingProgress.collectAsState()

    HudGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NEXUS INTELLIGENCE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NexusColors.Cyan,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "SYSTEM STATUS: OPERATIONAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.Green,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = { viewModel.startScan() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan",
                        tint = NexusColors.Cyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HudSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { onSearch(searchQuery) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            HudSectionHeader(title = "CORE ANALYTICS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "INDEXED",
                    value = stats.totalDocuments.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "API STATUS",
                    value = if (stats.apiOnline) "ONLINE" else "OFFLINE",
                    color = if (stats.apiOnline) NexusColors.Green else NexusColors.Red,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (stats.isScanning) {
                HudSectionHeader(title = "INDEXING IN PROGRESS")
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "SCANNING: ${indexingProgress.currentFile}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusColors.TextSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressValue = indexingProgress.progressFraction
                        LinearProgressIndicator(
                            progress = progressValue,
                            modifier = Modifier.fillMaxWidth(),
                            color = NexusColors.Cyan,
                            trackColor = NexusColors.Cyan.copy(alpha = 0.2f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            HudSectionHeader(title = "RECENT INTELLIGENCE")
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recentDocs) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick = { onDocumentClick(doc) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = NexusColors.Cyan,
    modifier: Modifier = Modifier
) {
    HolographicCard(
        modifier = modifier,
        borderColor = color.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
