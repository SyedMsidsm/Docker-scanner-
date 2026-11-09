package com.doclens.scanner.ui.capture

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.model.CaptureSession
import com.doclens.scanner.core.model.SessionPage
import com.doclens.scanner.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

data class CaptureUiState(
    val sessionId: String = "",
    val pages: List<SessionPage> = emptyList(),
    val pageCount: Int = 0,
    val isFlashOn: Boolean = false,
    val isAutoBoundary: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class CaptureViewModel(
    private val repository: DocumentRepository,
    existingSessionId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = if (!existingSessionId.isNullOrBlank()) {
                val existing = repository.getActiveSession()
                if (existing?.id == existingSessionId) existing
                else repository.startNewSession()
            } else {
                repository.getActiveSession() ?: repository.startNewSession()
            }

            _uiState.value = _uiState.value.copy(sessionId = session.id)
            loadSessionPages(session.id)
        }
    }

    private suspend fun loadSessionPages(sessionId: String) {
        val pages = repository.getSessionPages(sessionId)
        _uiState.value = _uiState.value.copy(
            pages = pages,
            pageCount = pages.size
        )
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(isFlashOn = !_uiState.value.isFlashOn)
    }

    fun toggleAutoBoundary() {
        _uiState.value = _uiState.value.copy(isAutoBoundary = !_uiState.value.isAutoBoundary)
    }

    fun onImageCaptured(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val asset = repository.importImageStream(inputStream)
                repository.addAssetToSession(_uiState.value.sessionId, asset)
                loadSessionPages(_uiState.value.sessionId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    fun onUrisImported(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                uris.forEach { uri ->
                    val mime = context.contentResolver.getType(uri)
                    if (mime == "application/pdf") {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            repository.importPdfStream(context, stream, _uiState.value.sessionId)
                        }
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val asset = repository.importImageStream(stream)
                            repository.addAssetToSession(_uiState.value.sessionId, asset)
                        }
                    }
                }
                loadSessionPages(_uiState.value.sessionId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    fun removePage(position: Int) {
        viewModelScope.launch {
            repository.removeSessionPage(_uiState.value.sessionId, position)
            loadSessionPages(_uiState.value.sessionId)
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val existingSessionId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CaptureViewModel(repository, existingSessionId) as T
        }
    }
}
