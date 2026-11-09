package com.doclens.scanner.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.model.Document
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ReviewUiState(
    val documentId: String = "",
    val documentTitle: String = "",
    val pages: List<Page> = emptyList(),
    val isLoading: Boolean = false,
    val selectedPageId: String? = null
)

class ReviewViewModel(
    private val repository: DocumentRepository,
    private val getAssetFile: (String) -> File,
    private val documentId: String?,
    private val sessionId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState(isLoading = true))
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            if (!documentId.isNullOrBlank()) {
                val doc = repository.getDocument(documentId)
                if (doc != null) {
                    _uiState.value = _uiState.value.copy(
                        documentId = doc.id,
                        documentTitle = doc.title
                    )
                    repository.observePages(doc.id).collect { pageList ->
                        _uiState.value = _uiState.value.copy(
                            pages = pageList,
                            isLoading = false
                        )
                    }
                }
            } else if (!sessionId.isNullOrBlank()) {
                // If coming directly from active session, commit it into a document or review session pages
                val session = repository.getActiveSession()
                val sessionPages = repository.getSessionPages(sessionId)
                if (session != null && sessionPages.isNotEmpty()) {
                    val doc = repository.commitSessionToDocument(sessionId, "Scan ${System.currentTimeMillis()}")
                    _uiState.value = _uiState.value.copy(
                        documentId = doc.id,
                        documentTitle = doc.title
                    )
                    repository.observePages(doc.id).collect { pageList ->
                        _uiState.value = _uiState.value.copy(
                            pages = pageList,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun movePageUp(index: Int) {
        if (index <= 0) return
        val pages = _uiState.value.pages.toMutableList()
        val temp = pages[index]
        pages[index] = pages[index - 1]
        pages[index - 1] = temp

        viewModelScope.launch {
            repository.reorderPages(_uiState.value.documentId, pages.map { it.id })
        }
    }

    fun movePageDown(index: Int) {
        val pages = _uiState.value.pages.toMutableList()
        if (index >= pages.size - 1) return
        val temp = pages[index]
        pages[index] = pages[index + 1]
        pages[index + 1] = temp

        viewModelScope.launch {
            repository.reorderPages(_uiState.value.documentId, pages.map { it.id })
        }
    }

    fun duplicatePage(pageId: String) {
        viewModelScope.launch {
            repository.duplicatePage(pageId)
        }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch {
            repository.deletePage(pageId)
        }
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch {
            repository.updateDocumentTitle(_uiState.value.documentId, newTitle)
            _uiState.value = _uiState.value.copy(documentTitle = newTitle)
        }
    }

    fun getFileForAsset(assetId: String): File = getAssetFile(assetId)

    class Factory(
        private val repository: DocumentRepository,
        private val getAssetFile: (String) -> File,
        private val documentId: String?,
        private val sessionId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(repository, getAssetFile, documentId, sessionId) as T
        }
    }
}
