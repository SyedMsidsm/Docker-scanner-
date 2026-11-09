package com.doclens.scanner.ui.export

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerFilterChip
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.engine.pdf.PaperSize
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    viewModel: ExportViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // SAF Save File Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                viewModel.copyExportToStream(outStream)
            }
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScannerTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.action_export),
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection
            Text(
                text = "Format",
                style = ScannerTheme.typography.buttonLabel,
                color = ScannerTheme.colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScannerFilterChip(
                    selected = uiState.format == ExportFormat.PDF,
                    text = stringResource(R.string.export_format_pdf),
                    onClick = { viewModel.setFormat(ExportFormat.PDF) }
                )
                ScannerFilterChip(
                    selected = uiState.format == ExportFormat.JPG,
                    text = stringResource(R.string.export_format_jpg),
                    onClick = { viewModel.setFormat(ExportFormat.JPG) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paper Size Selection (for PDF)
            if (uiState.format == ExportFormat.PDF) {
                Text(
                    text = "Page Size",
                    style = ScannerTheme.typography.buttonLabel,
                    color = ScannerTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScannerFilterChip(
                        selected = uiState.paperSize == PaperSize.A4,
                        text = stringResource(R.string.export_paper_a4),
                        onClick = { viewModel.setPaperSize(PaperSize.A4) }
                    )
                    ScannerFilterChip(
                        selected = uiState.paperSize == PaperSize.LETTER,
                        text = stringResource(R.string.export_paper_letter),
                        onClick = { viewModel.setPaperSize(PaperSize.LETTER) }
                    )
                    ScannerFilterChip(
                        selected = uiState.paperSize == PaperSize.ORIGINAL,
                        text = stringResource(R.string.export_paper_auto),
                        onClick = { viewModel.setPaperSize(PaperSize.ORIGINAL) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Export Progress / Result
            if (uiState.isExporting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rendering page ${uiState.progressCurrent} of ${uiState.progressTotal}…",
                        style = ScannerTheme.typography.body,
                        color = ScannerTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (uiState.progressTotal > 0) {
                                uiState.progressCurrent.toFloat() / uiState.progressTotal.toFloat()
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = ScannerTheme.colors.primary,
                        trackColor = ScannerTheme.colors.borderSubtle
                    )
                }
            } else if (uiState.exportedFile != null || uiState.exportedJpgs.isNotEmpty()) {
                val sizeKb = uiState.measuredBytes / 1024
                val sizeText = if (sizeKb > 1024) {
                    "%.2f MB".format(sizeKb / 1024.0)
                } else {
                    "$sizeKb KB"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScannerStatusBadge(
                        text = "Ready: $sizeText",
                        color = ScannerTheme.colors.success,
                        icon = Icons.Default.CheckCircle
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Share, Save, Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScannerPrimaryButton(
                        text = stringResource(R.string.action_share),
                        onClick = { viewModel.shareExportedFile(context) },
                        leadingIcon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    )

                    if (uiState.format == ExportFormat.PDF) {
                        ScannerOutlinedButton(
                            text = stringResource(R.string.action_save_to_device),
                            onClick = {
                                val name = "${uiState.documentTitle}.pdf"
                                saveFileLauncher.launch(name)
                            },
                            leadingIcon = Icons.Default.Download,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Button to prepare file
                ScannerPrimaryButton(
                    text = "Prepare File",
                    onClick = { viewModel.prepareExport() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
