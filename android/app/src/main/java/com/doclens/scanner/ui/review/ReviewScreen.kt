package com.doclens.scanner.ui.review

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.Page

import androidx.compose.material.icons.filled.TextFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit,
    onEditPage: (page: Page) -> Unit,
    onOcrPage: (page: Page) -> Unit,
    onAddMorePages: (documentId: String) -> Unit,
    onExportRequested: (documentId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                renameText = uiState.documentTitle
                                isRenaming = true
                            }
                        ) {
                            Text(
                                text = uiState.documentTitle,
                                style = ScannerTheme.typography.cardTitle,
                                color = ScannerTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = ScannerTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "${uiState.pages.size} pages",
                            style = ScannerTheme.typography.supportingLabel,
                            color = ScannerTheme.colors.onSurfaceVariant
                        )
                    }
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
                    if (uiState.pages.isNotEmpty()) {
                        ScannerPrimaryButton(
                            text = stringResource(R.string.action_ready_to_submit),
                            onClick = { onExportRequested(uiState.documentId) },
                            leadingIcon = Icons.Default.Share
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScannerTheme.colors.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddMorePages(uiState.documentId) },
                containerColor = ScannerTheme.colors.primary,
                contentColor = ScannerTheme.colors.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Page")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Save status indicator bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScannerStatusBadge(
                    text = stringResource(R.string.status_saved_locally),
                    color = ScannerTheme.colors.success,
                    icon = Icons.Default.CheckCircle
                )

                Text(
                    text = stringResource(R.string.status_not_backed_up),
                    style = ScannerTheme.typography.supportingLabel,
                    color = ScannerTheme.colors.warningText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ScannerTheme.colors.primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.pages, key = { _, item -> item.id }) { index, page ->
                        PageGridCard(
                            page = page,
                            index = index,
                            totalCount = uiState.pages.size,
                            assetFile = viewModel.getFileForAsset(page.sourceAssetId),
                            onEdit = { onEditPage(page) },
                            onOcr = { onOcrPage(page) },
                            onMoveUp = { viewModel.movePageUp(index) },
                            onMoveDown = { viewModel.movePageDown(index) },
                            onDuplicate = { viewModel.duplicatePage(page.id) },
                            onDelete = { viewModel.deletePage(page.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.updateTitle(renameText.trim())
                        }
                        isRenaming = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm), color = ScannerTheme.colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun PageGridCard(
    page: Page,
    index: Int,
    totalCount: Int,
    assetFile: java.io.File,
    onEdit: () -> Unit,
    onOcr: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    ScannerCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Page Header with Page Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ScannerTheme.colors.surfaceVariant
                ) {
                    Text(
                        text = "Page ${index + 1}",
                        style = ScannerTheme.typography.supportingLabel,
                        color = ScannerTheme.colors.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row {
                    if (index > 0) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move earlier",
                                tint = ScannerTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (index < totalCount - 1) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move later",
                                tint = ScannerTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ScannerTheme.colors.canvasBackground)
                    .border(1.dp, ScannerTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = assetFile),
                    contentDescription = "Page thumbnail",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (Edit, OCR, Duplicate, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit page",
                        tint = ScannerTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onOcr, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "OCR text",
                        tint = ScannerTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate page",
                        tint = ScannerTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete page",
                        tint = ScannerTheme.colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
