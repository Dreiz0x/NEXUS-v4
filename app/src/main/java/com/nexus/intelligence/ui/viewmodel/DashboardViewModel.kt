package com.nexus.intelligence.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.domain.model.DashboardStats
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.IndexingProgress
import com.nexus.intelligence.domain.usecase.GetDashboardStatsUseCase
import com.nexus.intelligence.domain.usecase.IndexDocumentsUseCase
import com.nexus.intelligence.domain.usecase.ManageSettingsUseCase
import com.nexus.intelligence.service.indexing.IndexingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val indexDocumentsUseCase: IndexDocumentsUseCase,
    private val manageSettingsUseCase: ManageSettingsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    private val _recentDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentInfo>> = _recentDocuments.asStateFlow()

    val indexingProgress: StateFlow<IndexingProgress> = indexDocumentsUseCase.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IndexingProgress())

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            getDashboardStatsUseCase.getDocumentCount().collect { count ->
                _dashboardStats.update { it.copy(totalDocuments = count) }
            }
        }
        viewModelScope.launch {
            getDashboardStatsUseCase.getRecentDocuments(20).collect { docs ->
                _recentDocuments.value = docs
            }
        }
        viewModelScope.launch {
            getDashboardStatsUseCase.getAllDocuments().collect { docs ->
                val byType = docs.groupBy { it.fileType }.mapValues { it.value.size }
                _dashboardStats.update { it.copy(documentsByType = byType) }
            }
        }
        viewModelScope.launch {
            getDashboardStatsUseCase.getIndexingStats().collect { stats ->
                if (stats != null) {
                    _dashboardStats.update {
                        it.copy(
                            lastScanTime = stats.lastScanTimestamp,
                            isScanning = stats.isCurrentlyScanning
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            _dashboardStats.update { it.copy(apiOnline = getDashboardStatsUseCase.isApiAvailable()) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startScan() {
        viewModelScope.launch {
            // Usa carpetas configuradas por el usuario, NO el root del storage
            val folders = manageSettingsUseCase.getMonitoredFolders()
                .first()
                .filter { it.isEnabled }
                .map { it.path }

            if (folders.isEmpty()) {
                // Sin carpetas configuradas, no hace nada — avisa en UI via stats
                _dashboardStats.update { it.copy(isScanning = false) }
                return@launch
            }

            IndexingService.startScan(context, folders)
        }
    }

    fun refreshApiStatus() {
        viewModelScope.launch {
            _dashboardStats.update { it.copy(apiOnline = getDashboardStatsUseCase.isApiAvailable()) }
        }
    }
}
