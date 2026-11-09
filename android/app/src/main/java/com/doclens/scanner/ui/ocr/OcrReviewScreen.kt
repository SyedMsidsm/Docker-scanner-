package com.doclens.scanner.ui.ocr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    viewModel: OcrReviewViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Split View, 1: Text Only

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OCR Review & Edit",
                        style = ScannerTheme.typography.screenTitle,
                        color = ScannerTheme.colors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ScannerTheme.colors.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reRunRecognition() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-run OCR",
                            tint = ScannerTheme.colors.primary
                        )
                    }

                    ScannerPrimaryButton(
                        text = "Save",
                        onClick = { viewModel.saveReviewedText() },
                        leadingIcon = Icons.Default.Check
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScannerTheme.colors.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ScannerTheme.colors.surface,
                contentColor = ScannerTheme.colors.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Split View") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Full Text Editor") }
                )
            }

            if (uiState.isRecognizing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ScannerTheme.colors.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting text with on-device ML Kit…",
                            style = ScannerTheme.typography.body,
                            color = ScannerTheme.colors.onSurface
                        )
                    }
                }
            } else {
                if (selectedTab == 0) {
                    // Split View: Image Preview on Top + Text on Bottom
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Half: Document Image with Bounding Boxes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(ScannerTheme.colors.canvasBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.assetFile != null) {
                                BoxWithConstraints(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val painter = rememberAsyncImagePainter(model = uiState.assetFile)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            painter = painter,
                                            contentDescription = "Document Scan",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Bounding Box Overlays
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            uiState.ocrResult?.blocks?.forEachIndexed { index, block ->
                                                block.boundingBox?.let { box ->
                                                    val isSelected = uiState.selectedBlockIndex == index
                                                    val color = if (isSelected) Color(0xFFD9AC46) else Color(0x7775B890)
                                                    val stroke = if (isSelected) 3f else 1.5f

                                                    drawRect(
                                                        color = color,
                                                        topLeft = Offset(box.left * size.width, box.top * size.height),
                                                        size = Size((box.right - box.left) * size.width, (box.bottom - box.top) * size.height),
                                                        style = Stroke(width = stroke)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Half: Text Editor & Actions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(ScannerTheme.colors.surface)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recognized Text (${uiState.ocrResult?.blocks?.size ?: 0} blocks)",
                                    style = ScannerTheme.typography.cardTitle,
                                    color = ScannerTheme.colors.onSurface
                                )

                                Row {
                                    IconButton(onClick = { viewModel.copyToClipboard(context) }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = ScannerTheme.colors.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.exportTxtFile(context) }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export TXT",
                                            tint = ScannerTheme.colors.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.editableText,
                                onValueChange = { viewModel.updateText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = ScannerTheme.typography.body,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScannerTheme.colors.primary,
                                    unfocusedBorderColor = ScannerTheme.colors.borderSubtle
                                )
                            )
                        }
                    }
                } else {
                    // Full Text Editor Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScannerOutlinedButton(
                                text = "Copy to Clipboard",
                                onClick = { viewModel.copyToClipboard(context) },
                                leadingIcon = Icons.Default.ContentCopy
                            )

                            ScannerOutlinedButton(
                                text = "Export as .TXT",
                                onClick = { viewModel.exportTxtFile(context) },
                                leadingIcon = Icons.Default.Share
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.editableText,
                            onValueChange = { viewModel.updateText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = ScannerTheme.typography.body,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScannerTheme.colors.primary,
                                unfocusedBorderColor = ScannerTheme.colors.borderSubtle
                            )
                        )
                    }
                }
            }
        }
    }
}
