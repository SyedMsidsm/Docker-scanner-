package com.doclens.scanner.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerFilterChip
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.NormalizedPoint
import com.doclens.scanner.engine.image.ImageEngine

enum class EditorToolTab {
    CROP_ROTATE,
    FILTER,
    ADJUST
}

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    imageEngine: ImageEngine,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(EditorToolTab.CROP_ROTATE) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(false) }

    // Re-render preview bitmap whenever recipe or assetFile changes
    LaunchedEffect(uiState.assetFile, uiState.currentRecipe) {
        val file = uiState.assetFile ?: return@LaunchedEffect
        isRenderingPreview = true
        try {
            val bmp = imageEngine.renderProcessedBitmap(
                sourceFile = file,
                recipe = uiState.currentRecipe,
                maxDimension = 1200
            )
            previewBitmap?.recycle()
            previewBitmap = bmp
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRenderingPreview = false
        }
    }

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ScannerTheme.colors.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) ScannerTheme.colors.onSurface else ScannerTheme.colors.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) ScannerTheme.colors.onSurface else ScannerTheme.colors.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.resetRecipe() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = ScannerTheme.colors.onSurface
                        )
                    }
                }

                ScannerPrimaryButton(
                    text = stringResource(R.string.editor_apply),
                    onClick = { viewModel.applyChanges(onSaved) },
                    isLoading = uiState.isSaving,
                    leadingIcon = Icons.Default.Check
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Neutral Document Canvas (Section 31.1: theme must not recolor the scan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ScannerTheme.colors.canvasBackground),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val imgBmp = previewBitmap!!
                        val aspect = imgBmp.width.toFloat() / imgBmp.height.toFloat()

                        val (viewW, viewH) = if (maxWidth.value / maxHeight.value > aspect) {
                            (maxHeight.value * aspect).dp to maxHeight
                        } else {
                            maxWidth to (maxWidth.value / aspect).dp
                        }

                        Box(
                            modifier = Modifier
                                .size(viewW, viewH)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = imgBmp.asImageBitmap(),
                                contentDescription = "Page Preview",
                                modifier = Modifier.fillMaxSize()
                            )

                            // 4-Point Draggable Crop Quad Overlay when on CROP_ROTATE tab
                            if (selectedTab == EditorToolTab.CROP_ROTATE) {
                                CropOverlay(
                                    cropQuad = uiState.currentRecipe.cropQuad,
                                    onCornerMoved = { index, point ->
                                        viewModel.updateCropCorner(index, point)
                                    }
                                )
                            }
                        }
                    }
                }

                if (isRenderingPreview) {
                    CircularProgressIndicator(
                        color = ScannerTheme.colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom Tools Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScannerTheme.colors.surface)
                    .padding(vertical = 8.dp)
            ) {
                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = ScannerTheme.colors.surface,
                    contentColor = ScannerTheme.colors.primary
                ) {
                    Tab(
                        selected = selectedTab == EditorToolTab.CROP_ROTATE,
                        onClick = { selectedTab = EditorToolTab.CROP_ROTATE },
                        text = { Text("Crop & Rotate") },
                        icon = { Icon(Icons.Default.Crop, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == EditorToolTab.FILTER,
                        onClick = { selectedTab = EditorToolTab.FILTER },
                        text = { Text("Filter") },
                        icon = { Icon(Icons.Default.Filter, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == EditorToolTab.ADJUST,
                        onClick = { selectedTab = EditorToolTab.ADJUST },
                        text = { Text("Adjust") },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        EditorToolTab.CROP_ROTATE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ScannerPrimaryButton(
                                    text = stringResource(R.string.editor_rotate),
                                    onClick = { viewModel.rotate90() },
                                    leadingIcon = Icons.Default.RotateRight
                                )
                            }
                        }

                        EditorToolTab.FILTER -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ScannerFilterChip(
                                    selected = uiState.currentRecipe.filter == ImageFilter.ORIGINAL,
                                    text = stringResource(R.string.filter_original),
                                    onClick = { viewModel.setFilter(ImageFilter.ORIGINAL) }
                                )
                                ScannerFilterChip(
                                    selected = uiState.currentRecipe.filter == ImageFilter.COLOR_BOOST,
                                    text = stringResource(R.string.filter_color),
                                    onClick = { viewModel.setFilter(ImageFilter.COLOR_BOOST) }
                                )
                                ScannerFilterChip(
                                    selected = uiState.currentRecipe.filter == ImageFilter.GRAYSCALE,
                                    text = stringResource(R.string.filter_grayscale),
                                    onClick = { viewModel.setFilter(ImageFilter.GRAYSCALE) }
                                )
                                ScannerFilterChip(
                                    selected = uiState.currentRecipe.filter == ImageFilter.BW_CLEAN,
                                    text = stringResource(R.string.filter_bw),
                                    onClick = { viewModel.setFilter(ImageFilter.BW_CLEAN) }
                                )
                            }
                        }

                        EditorToolTab.ADJUST -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.editor_brightness),
                                        style = ScannerTheme.typography.body,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Slider(
                                        value = uiState.currentRecipe.brightness,
                                        onValueChange = { viewModel.setBrightness(it) },
                                        valueRange = -0.5f..0.5f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = ScannerTheme.colors.primary, activeTrackColor = ScannerTheme.colors.primary)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.editor_contrast),
                                        style = ScannerTheme.typography.body,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Slider(
                                        value = uiState.currentRecipe.contrast,
                                        onValueChange = { viewModel.setContrast(it) },
                                        valueRange = 0.5f..2.0f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = ScannerTheme.colors.primary, activeTrackColor = ScannerTheme.colors.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropOverlay(
    cropQuad: com.doclens.scanner.core.model.CropQuad,
    onCornerMoved: (index: Int, point: NormalizedPoint) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val p0 = Offset(cropQuad.topLeft.x * size.width, cropQuad.topLeft.y * size.height)
            val p1 = Offset(cropQuad.topRight.x * size.width, cropQuad.topRight.y * size.height)
            val p2 = Offset(cropQuad.bottomRight.x * size.width, cropQuad.bottomRight.y * size.height)
            val p3 = Offset(cropQuad.bottomLeft.x * size.width, cropQuad.bottomLeft.y * size.height)

            drawLine(Color(0xFFD9AC46), p0, p1, strokeWidth = 3f)
            drawLine(Color(0xFFD9AC46), p1, p2, strokeWidth = 3f)
            drawLine(Color(0xFFD9AC46), p2, p3, strokeWidth = 3f)
            drawLine(Color(0xFFD9AC46), p3, p0, strokeWidth = 3f)
        }

        // Draggable Handles
        val corners = listOf(
            cropQuad.topLeft,
            cropQuad.topRight,
            cropQuad.bottomRight,
            cropQuad.bottomLeft
        )

        corners.forEachIndexed { index, normPoint ->
            val offsetX = (normPoint.x * w) - 16
            val offsetY = (normPoint.y * h) - 16

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = offsetX.coerceAtLeast(0f).dp, top = offsetY.coerceAtLeast(0f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9AC46))
                    .pointerInput(index) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = ((normPoint.x * w) + dragAmount.x) / w
                            val newY = ((normPoint.y * h) + dragAmount.y) / h
                            onCornerMoved(
                                index,
                                NormalizedPoint(
                                    x = newX.coerceIn(0f, 1f),
                                    y = newY.coerceIn(0f, 1f)
                                )
                            )
                        }
                    }
            )
        }
    }
}
