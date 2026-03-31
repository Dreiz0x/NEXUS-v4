package com.nexus.intelligence.domain.model

// ── Organizer Models ─────────────────────────────────────────────

data class FileCluster(
    val id: String,
    val suggestedFolderName: String,
    val documents: List<DocumentInfo>,
    val isApproved: Boolean = true,
    val customFolderName: String? = null
) {
    val folderName: String get() = customFolderName ?: suggestedFolderName
    val totalSize: Long get() = documents.sumOf { it.fileSize }
    val fileCount: Int get() = documents.size
}

data class OrganizerState(
    val clusters: List<FileCluster> = emptyList(),
    val masterFolder: String = "",
    val isAnalyzing: Boolean = false,
    val isApplying: Boolean = false,
    val analysisProgress: String = "",
    val applyResult: ApplyResult? = null,
    val error: String? = null
)

data class ApplyResult(
    val moved: Int,
    val renamed: Int,
    val failed: Int,
    val failedFiles: List<String> = emptyList()
)

data class GenericFileInfo(
    val document: DocumentInfo,
    val isGenericName: Boolean,
    val suggestedName: String? = null
)
