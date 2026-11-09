package com.doclens.scanner.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.model.CaptureSession
import com.doclens.scanner.core.model.Document
import com.doclens.scanner.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DocumentFilter {
    ALL,
    RECENT,
    NOT_BACKED_UP
}

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val activeSession: CaptureSession? = null,
    val sessionPageCount: Int = 0,
    val selectedFilter: DocumentFilter = DocumentFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class LibraryViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DocumentFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _activeSession = MutableStateFlow<CaptureSession?>(null)
    private val _sessionPageCount = MutableStateFlow(0)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeDocuments(),
        _selectedFilter,
        _searchQuery,
        _activeSession,
        _sessionPageCount
    ) { docs, filter, query, session, pageCount ->
        val filtered = docs
            .filter { doc ->
                if (query.isBlank()) true
                else doc.title.contains(query, ignoreCase = true)
            }
            .filter { doc ->
                when (filter) {
                    DocumentFilter.ALL -> true
                    DocumentFilter.RECENT -> {
                        val oneDayAgo = System.currentTimeMillis() - 86_400_000L
                        doc.updatedAt >= oneDayAgo
                    }
                    DocumentFilter.NOT_BACKED_UP -> {
                        doc.backupState != com.doclens.scanner.core.model.BackupState.BACKED_UP
                    }
                }
            }

        LibraryUiState(
            documents = filtered,
            activeSession = session,
            sessionPageCount = pageCount,
            selectedFilter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LibraryUiState(isLoading = true)
    )

    init {
        refreshActiveSession()
    }

    fun refreshActiveSession() {
        viewModelScope.launch {
            val session = repository.getActiveSession()
            _activeSession.value = session
            if (session != null) {
                val pages = repository.getSessionPages(session.id)
                _sessionPageCount.value = pages.size
            } else {
                _sessionPageCount.value = 0
            }
        }
    }

    fun setFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun discardSession(sessionId: String) {
        viewModelScope.launch {
            repository.discardSession(sessionId)
            refreshActiveSession()
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            repository.softDeleteDocument(documentId)
        }
    }

    fun renameDocument(documentId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateDocumentTitle(documentId, newTitle)
        }
    }

    fun toggleLock(documentId: String, isLocked: Boolean) {
        viewModelScope.launch {
            repository.setDocumentLocked(documentId, isLocked)
        }
    }

    class Factory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository) as T
        }
    }
}
