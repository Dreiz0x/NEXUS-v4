package com.nexus.intelligence.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.usecase.GetFileMapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileMapViewModel @Inject constructor(
    private val getFileMapUseCase: GetFileMapUseCase
) : ViewModel() {

    private val _directories = MutableStateFlow<List<String>>(emptyList())
    val directories: StateFlow<List<String>> = _directories.asStateFlow()

    private val _selectedDirectory = MutableStateFlow<String?>(null)
    val selectedDirectory: StateFlow<String?> = _selectedDirectory.asStateFlow()

    private val _directoryDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val directoryDocuments: StateFlow<List<DocumentInfo>> = _directoryDocuments.asStateFlow()

    private val _expandedDirs = MutableStateFlow<Set<String>>(emptySet())
    val expandedDirs: StateFlow<Set<String>> = _expandedDirs.asStateFlow()

    init {
        viewModelScope.launch {
            getFileMapUseCase.getAllDirectories().collect { dirs ->
                _directories.value = dirs
            }
        }
    }

    fun toggleDirectory(path: String) {
        val current = _expandedDirs.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _expandedDirs.value = current
    }

    fun selectDirectory(path: String) {
        _selectedDirectory.value = path
        viewModelScope.launch {
            getFileMapUseCase.getDocumentsByDirectory(path).collect { docs ->
                _directoryDocuments.value = docs
            }
        }
    }
}
