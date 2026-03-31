package com.nexus.intelligence.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.FileCluster
import com.nexus.intelligence.domain.model.OrganizerState
import com.nexus.intelligence.domain.usecase.FileManagerUseCase
import com.nexus.intelligence.domain.usecase.FileOrganizerUseCase
import com.nexus.intelligence.domain.usecase.GetDashboardStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizerViewModel @Inject constructor(
    private val organizerUseCase: FileOrganizerUseCase,
    private val fileManagerUseCase: FileManagerUseCase,
    private val dashboardStatsUseCase: GetDashboardStatsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OrganizerState())
    val state: StateFlow<OrganizerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            organizerUseCase.progress.collectLatest { msg ->
                _state.update { it.copy(analysisProgress = msg) }
            }
        }
    }

    fun analyzeAllDocuments() {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, error = null, clusters = emptyList()) }
            try {
                val docs = dashboardStatsUseCase.getAllDocuments()
                    .let { flow ->
                        var result = emptyList<DocumentInfo>()
                        flow.collectLatest { result = it }
                        result
                    }

                if (docs.isEmpty()) {
                    _state.update {
                        it.copy(isAnalyzing = false, error = "No hay documentos indexados aún")
                    }
                    return@launch
                }

                val clusters = organizerUseCase.analyzeDocuments(docs)
                _state.update {
                    it.copy(isAnalyzing = false, clusters = clusters, analysisProgress = "")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isAnalyzing = false, error = e.message ?: "Error al analizar")
                }
            }
        }
    }

    fun setMasterFolder(path: String) {
        _state.update { it.copy(masterFolder = path) }
    }

    fun toggleClusterApproval(clusterId: String) {
        _state.update { state ->
            state.copy(
                clusters = state.clusters.map { cluster ->
                    if (cluster.id == clusterId) cluster.copy(isApproved = !cluster.isApproved)
                    else cluster
                }
            )
        }
    }

    fun renameCluster(clusterId: String, newName: String) {
        _state.update { state ->
            state.copy(
                clusters = state.clusters.map { cluster ->
                    if (cluster.id == clusterId) cluster.copy(customFolderName = newName.ifBlank { null })
                    else cluster
                }
            )
        }
    }

    fun removeDocumentFromCluster(clusterId: String, docId: Long) {
        _state.update { state ->
            state.copy(
                clusters = state.clusters.mapNotNull { cluster ->
                    if (cluster.id != clusterId) return@mapNotNull cluster
                    val updated = cluster.documents.filter { it.id != docId }
                    if (updated.isEmpty()) null else cluster.copy(documents = updated)
                }
            )
        }
    }

    fun applyOrganization() {
        val currentState = _state.value
        if (currentState.masterFolder.isBlank()) {
            _state.update { it.copy(error = "Define una carpeta maestra primero") }
            return
        }

        val approvedClusters = currentState.clusters.filter { it.isApproved }
        if (approvedClusters.isEmpty()) {
            _state.update { it.copy(error = "No hay grupos aprobados para aplicar") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isApplying = true, error = null) }
            try {
                val result = fileManagerUseCase.applyOrganization(
                    approvedClusters,
                    currentState.masterFolder
                )
                _state.update {
                    it.copy(isApplying = false, applyResult = result)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isApplying = false, error = e.message ?: "Error al aplicar")
                }
            }
        }
    }

    fun clearResult() {
        _state.update { it.copy(applyResult = null, error = null) }
    }
}
