package com.nexus.intelligence.ui.screens.search

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import com.nexus.intelligence.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    initialQuery: String = "",
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onDocumentClick: (DocumentInfo) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

    // Trigger initial search
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateSearchQuery(initialQuery)
            viewModel.performSearch()
        }
    }

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
                    text = "SEARCH RESULTS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NexusColors.Cyan,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Search Bar ───────────────────────────────────────
            HudSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Search Mode Toggle ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchModeButton(
                    label = "TEXT",
                    isSelected = searchMode == SearchMode.TEXT,
                    color = NexusColors.Cyan,
                    onClick = { viewModel.setSearchMode(SearchMode.TEXT) }
                )
                SearchModeButton(
                    label = "SEMANTIC",
                    isSelected = searchMode == SearchMode.SEMANTIC,
                    color = NexusColors.Magenta,
                    onClick = { viewModel.setSearchMode(SearchMode.SEMANTIC) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Results ──────────────────────────────────────────
            if (isSearching) {
                SearchingAnimation()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AI Response (if semantic search)
                    if (aiResponse != null) {
                        item {
                            HolographicCard(borderColor = NexusColors.Magenta) {
                                Text(
                                    text = "AI ANALYSIS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NexusColors.Magenta
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aiResponse ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NexusColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Result count
                    item {
                        Text(
                            text = ">> ${searchResults.size} MATCHES FOUND",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (searchResults.isNotEmpty()) NexusColors.Green else NexusColors.Red,
                            letterSpacing = 1.sp
                        )
                    }

                    if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            NoResultsPanel()
                        }
                    }

                    items(searchResults) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = { onDocumentClick(result.document) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

// ── Search Mode Button ───────────────────────────────────────────

@Composable
private fun SearchModeButton(
    label: String,
    isSelected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    HolographicCard(
        borderColor = if (isSelected) color else NexusColors.TextDim,
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) color else NexusColors.TextDim,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.5.sp
        )
    }
}

// ── Searching Animation ──────────────────────────────────────────

@Composable
private fun SearchingAnimation() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        AnimatedRadar(
            isActive = true,
            size = 150.dp,
            color = NexusColors.Cyan
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SCANNING DATABASE...",
            style = MaterialTheme.typography.labelLarge,
            color = NexusColors.Cyan,
            letterSpacing = 3.sp
        )
    }
}

// ── No Results Panel ─────────────────────────────────────────────

@Composable
private fun NoResultsPanel() {
    HolographicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NexusColors.Red
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NO MATCHES FOUND",
                style = MaterialTheme.typography.headlineSmall,
                color = NexusColors.Red,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try different keywords or enable semantic search for natural language queries",
                style = MaterialTheme.typography.bodySmall,
                color = NexusColors.TextDim
            )
        }
    }
}

enum class SearchMode {
    TEXT, SEMANTIC
}
