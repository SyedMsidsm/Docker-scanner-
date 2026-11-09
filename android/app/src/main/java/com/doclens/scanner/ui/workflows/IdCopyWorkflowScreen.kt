package com.doclens.scanner.ui.workflows

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.Asset
import com.doclens.scanner.core.model.IdCardLayoutConfig
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepository
import com.doclens.scanner.engine.pdf.PdfEngine
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCopyWorkflowScreen(
    repository: DocumentRepository,
    assetStore: AssetStore,
    pdfEngine: PdfEngine,
    onBack: () -> Unit,
    onCompleted: (exportedPdf: File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var frontAsset by remember { mutableStateOf<Asset?>(null) }
    var backAsset by remember { mutableStateOf<Asset?>(null) }
    var watermarkText by remember { mutableStateOf("FOR OFFICIAL IDENTIFICATION ONLY") }
    var isProcessing by remember { mutableStateOf(false) }

    // Front Card Picker
    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    frontAsset = repository.importImageStream(stream)
                }
            }
        }
    }

    // Back Card Picker
    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    backAsset = repository.importImageStream(stream)
                }
            }
        }
    }

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ID Card Copy (Front & Back)",
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Assemble both sides of your National ID, Driver's License, or Card onto a single printable A4 page with an optional purpose watermark to protect your privacy.",
                style = ScannerTheme.typography.body,
                color = ScannerTheme.colors.onSurfaceVariant
            )

            // Front Card Box
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "1. Front of ID Card",
                        style = ScannerTheme.typography.cardTitle,
                        color = ScannerTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (frontAsset != null) {
                        val file = assetStore.getAssetFile(frontAsset!!.id)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.58f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScannerTheme.colors.canvasBackground)
                                .border(1.dp, ScannerTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    frontPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = file),
                                contentDescription = "Front of ID",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        ScannerOutlinedButton(
                            text = "Select Front of Card",
                            onClick = {
                                frontPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = Icons.Default.CameraAlt,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Back Card Box
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "2. Back of ID Card",
                        style = ScannerTheme.typography.cardTitle,
                        color = ScannerTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (backAsset != null) {
                        val file = assetStore.getAssetFile(backAsset!!.id)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.58f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScannerTheme.colors.canvasBackground)
                                .border(1.dp, ScannerTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    backPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = file),
                                contentDescription = "Back of ID",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        ScannerOutlinedButton(
                            text = "Select Back of Card",
                            onClick = {
                                backPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = Icons.Default.CameraAlt,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Purpose Watermark (Section 28.7: ID copy purpose watermark)
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "3. Security Purpose Watermark (Optional)",
                        style = ScannerTheme.typography.cardTitle,
                        color = ScannerTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        placeholder = { Text("e.g. ONLY FOR BANK VERIFICATION") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerTheme.colors.primary,
                            unfocusedBorderColor = ScannerTheme.colors.borderSubtle
                        )
                    )
                }
            }

            // Generate Button
            val canGenerate = frontAsset != null && backAsset != null && !isProcessing
            ScannerPrimaryButton(
                text = if (isProcessing) "Generating Printable A4 Layout…" else "Create 1-Page Printable ID Copy",
                onClick = {
                    if (canGenerate) {
                        isProcessing = true
                        coroutineScope.launch {
                            val outFile = assetStore.createTemporaryExportFile("ID_Card_Copy_${System.currentTimeMillis()}.pdf")
                            val config = IdCardLayoutConfig(
                                frontAssetId = frontAsset!!.id,
                                backAssetId = backAsset!!.id,
                                purposeWatermark = watermarkText.ifBlank { null }
                            )
                            val result = pdfEngine.exportIdCardLayout(config, outFile)
                            isProcessing = false
                            onCompleted(result)
                        }
                    }
                },
                enabled = canGenerate,
                isLoading = isProcessing,
                leadingIcon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
