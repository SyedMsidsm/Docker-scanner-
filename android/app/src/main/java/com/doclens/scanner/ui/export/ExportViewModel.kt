package com.doclens.scanner.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepository
import com.doclens.scanner.engine.pdf.PaperSize
import com.doclens.scanner.engine.pdf.PdfEngine
import com.doclens.scanner.engine.pdf.PdfExportConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

enum class ExportFormat {
    PDF,
    JPG
}

data class ExportUiState(
    val documentId: String = "",
    val documentTitle: String = "",
    val format: ExportFormat = ExportFormat.PDF,
    val paperSize: PaperSize = PaperSize.A4,
    val isExporting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val exportedFile: File? = null,
    val exportedJpgs: List<File> = emptyList(),
    val measuredBytes: Long = 0,
    val errorMessage: String? = null
)

class ExportViewModel(
    private val repository: DocumentRepository,
    private val pdfEngine: PdfEngine,
    private val assetStore: AssetStore,
    private val documentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState(documentId = documentId))
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId)
            if (doc != null) {
                _uiState.value = _uiState.value.copy(documentTitle = doc.title)
            }
        }
    }

    fun setFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(format = format, exportedFile = null)
    }

    fun setPaperSize(size: PaperSize) {
        _uiState.value = _uiState.value.copy(paperSize = size, exportedFile = null)
    }

    fun prepareExport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            val pages = repository.getPages(documentId)
            if (pages.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "No pages to export"
                )
                return@launch
            }

            val sanitizedTitle = _uiState.value.documentTitle
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .ifBlank { "scan" }

            val config = PdfExportConfig(
                title = _uiState.value.documentTitle,
                paperSize = _uiState.value.paperSize
            )

            try {
                if (_uiState.value.format == ExportFormat.PDF) {
                    val outFile = assetStore.createTemporaryExportFile("${sanitizedTitle}.pdf")
                    val result = pdfEngine.exportPdf(
                        pages = pages,
                        outputFile = outFile,
                        config = config,
                        onProgress = { cur, tot ->
                            _uiState.value = _uiState.value.copy(progressCurrent = cur, progressTotal = tot)
                        }
                    )
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedFile = result,
                        measuredBytes = result.length()
                    )
                } else {
                    val outDir = assetStore.createTemporaryExportFile(sanitizedTitle)
                    val results = pdfEngine.exportJpgImages(
                        pages = pages,
                        outputDirectory = outDir,
                        baseName = sanitizedTitle,
                        config = config,
                        onProgress = { cur, tot ->
                            _uiState.value = _uiState.value.copy(progressCurrent = cur, progressTotal = tot)
                        }
                    )
                    val totalBytes = results.sumOf { it.length() }
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedJpgs = results,
                        measuredBytes = totalBytes
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun shareExportedFile(context: Context) {
        val file = _uiState.value.exportedFile
        if (file != null && file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
        } else if (_uiState.value.exportedJpgs.isNotEmpty()) {
            val uris = ArrayList<Uri>()
            _uiState.value.exportedJpgs.forEach { jpg ->
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        jpg
                    )
                )
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Document Pages"))
        }
    }

    fun copyExportToStream(outputStream: OutputStream) {
        val file = _uiState.value.exportedFile
        if (file != null && file.exists()) {
            file.inputStream().use { input ->
                input.copyTo(outputStream)
            }
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val pdfEngine: PdfEngine,
        private val assetStore: AssetStore,
        private val documentId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExportViewModel(repository, pdfEngine, assetStore, documentId) as T
        }
    }
}
