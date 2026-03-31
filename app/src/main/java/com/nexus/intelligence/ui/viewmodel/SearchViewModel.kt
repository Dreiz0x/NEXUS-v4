package com.nexus.intelligence.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.domain.usecase.SearchDocumentsUseCase
import com.nexus.intelligence.ui.screens.search.SearchMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.TEXT)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        if (_searchQuery.value.isNotBlank()) {
            performSearch()
        }
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            _aiResponse.value = null

            try {
                val results = when (_searchMode.value) {
                    SearchMode.TEXT -> searchDocumentsUseCase.executeText(query)
                    SearchMode.SEMANTIC -> searchDocumentsUseCase.executeSemantic(query)
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
