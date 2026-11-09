package com.doclens.scanner.ui.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.model.OcrBlock
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.core.model.PageOcrResult
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepository
import com.doclens.scanner.engine.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class OcrReviewUiState(
    val pageId: String = "",
    val documentId: String = "",
    val assetFile: File? = null,
    val isRecognizing: Boolean = false,
    val ocrResult: PageOcrResult? = null,
    val editableText: String = "",
    val selectedBlockIndex: Int? = null,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class OcrReviewViewModel(
    private val repository: DocumentRepository,
    private val ocrEngine: OcrEngine,
    private val assetStore: AssetStore,
    private val pageId: String,
    private val documentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OcrReviewUiState(pageId = pageId, documentId = documentId)
    )
    val uiState: StateFlow<OcrReviewUiState> = _uiState.asStateFlow()

    init {
        loadAndRecognize()
    }

    private fun loadAndRecognize() {
        viewModelScope.launch {
            val pages = repository.getPages(documentId)
            val page = pages.find { it.id == pageId } ?: return@launch
            val assetFile = assetStore.getAssetFile(page.sourceAssetId)

            _uiState.value = _uiState.value.copy(assetFile = assetFile)

            // Check if existing OCR result exists in Room
            val existing = repository.getOcrResult(pageId)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    ocrResult = existing,
                    editableText = existing.effectiveText
                )
            } else {
                // Run recognition
                runRecognition(page, assetFile)
            }
        }
    }

    fun reRunRecognition() {
        viewModelScope.launch {
            val pages = repository.getPages(documentId)
            val page = pages.find { it.id == pageId } ?: return@launch
            val assetFile = _uiState.value.assetFile ?: return@launch
            runRecognition(page, assetFile)
        }
    }

    private suspend fun runRecognition(page: Page, assetFile: File) {
        _uiState.value = _uiState.value.copy(isRecognizing = true, errorMessage = null)
        try {
            val result = ocrEngine.recognizeText(
                imageFile = assetFile,
                pageId = page.id,
                revision = page.revision
            )
            repository.saveOcrResult(documentId, result)
            _uiState.value = _uiState.value.copy(
                ocrResult = result,
                editableText = result.effectiveText,
                isRecognizing = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isRecognizing = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun updateText(newText: String) {
        _uiState.value = _uiState.value.copy(editableText = newText, isSaved = false)
    }

    fun selectBlock(index: Int?) {
        _uiState.value = _uiState.value.copy(selectedBlockIndex = index)
    }

    fun saveReviewedText() {
        viewModelScope.launch {
            repository.updateUserReviewedText(pageId, _uiState.value.editableText)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Recognized Text", _uiState.value.editableText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun exportTxtFile(context: Context) {
        viewModelScope.launch {
            val txtFile = assetStore.createTemporaryExportFile("page_${pageId.take(6)}_text.txt")
            FileOutputStream(txtFile).use { out ->
                out.write(_uiState.value.editableText.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                txtFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export TXT"))
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val ocrEngine: OcrEngine,
        private val assetStore: AssetStore,
        private val pageId: String,
        private val documentId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OcrReviewViewModel(repository, ocrEngine, assetStore, pageId, documentId) as T
        }
    }
}
