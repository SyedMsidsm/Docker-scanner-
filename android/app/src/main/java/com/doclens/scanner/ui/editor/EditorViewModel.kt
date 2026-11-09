package com.doclens.scanner.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.NormalizedPoint
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Stack

data class EditorUiState(
    val pageId: String = "",
    val documentId: String = "",
    val assetFile: File? = null,
    val currentRecipe: EditRecipe = EditRecipe(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class EditorViewModel(
    private val repository: DocumentRepository,
    private val getAssetFile: (String) -> File,
    private val pageId: String
) : ViewModel() {

    private val undoStack = Stack<EditRecipe>()
    private val redoStack = Stack<EditRecipe>()

    private val _uiState = MutableStateFlow(EditorUiState(pageId = pageId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadPage()
    }

    private fun loadPage() {
        viewModelScope.launch {
            // Find page across documents
            val docs = repository.observeDocuments()
            // We can search through pages or load directly from DAO
            // For now, let's observe pages or get directly:
            // Since we know pageId, we can load it from repository:
        }
    }

    fun initFromPage(page: Page) {
        val file = getAssetFile(page.sourceAssetId)
        undoStack.clear()
        redoStack.clear()
        _uiState.value = _uiState.value.copy(
            pageId = page.id,
            documentId = page.documentId,
            assetFile = file,
            currentRecipe = page.recipe,
            canUndo = false,
            canRedo = false
        )
    }

    private fun pushRecipeChange(newRecipe: EditRecipe) {
        undoStack.push(_uiState.value.currentRecipe)
        redoStack.clear()
        _uiState.value = _uiState.value.copy(
            currentRecipe = newRecipe,
            canUndo = true,
            canRedo = false
        )
    }

    fun rotate90() {
        val currentDeg = _uiState.value.currentRecipe.rotationDegrees
        val nextDeg = (currentDeg + 90) % 360
        pushRecipeChange(_uiState.value.currentRecipe.copy(rotationDegrees = nextDeg))
    }

    fun setFilter(filter: ImageFilter) {
        pushRecipeChange(_uiState.value.currentRecipe.copy(filter = filter))
    }

    fun setBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(
            currentRecipe = _uiState.value.currentRecipe.copy(brightness = brightness)
        )
    }

    fun setContrast(contrast: Float) {
        _uiState.value = _uiState.value.copy(
            currentRecipe = _uiState.value.currentRecipe.copy(contrast = contrast)
        )
    }

    fun updateCropCorner(cornerIndex: Int, newPoint: NormalizedPoint) {
        val quad = _uiState.value.currentRecipe.cropQuad
        val updatedQuad = when (cornerIndex) {
            0 -> quad.copy(topLeft = newPoint)
            1 -> quad.copy(topRight = newPoint)
            2 -> quad.copy(bottomRight = newPoint)
            3 -> quad.copy(bottomLeft = newPoint)
            else -> quad
        }
        pushRecipeChange(_uiState.value.currentRecipe.copy(cropQuad = updatedQuad))
    }

    fun resetRecipe() {
        pushRecipeChange(EditRecipe())
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.pop()
            redoStack.push(_uiState.value.currentRecipe)
            _uiState.value = _uiState.value.copy(
                currentRecipe = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.pop()
            undoStack.push(_uiState.value.currentRecipe)
            _uiState.value = _uiState.value.copy(
                currentRecipe = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun applyChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.updatePageRecipe(_uiState.value.pageId, _uiState.value.currentRecipe)
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            onComplete()
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val getAssetFile: (String) -> File,
        private val pageId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(repository, getAssetFile, pageId) as T
        }
    }
}
