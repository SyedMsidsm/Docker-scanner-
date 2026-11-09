package com.doclens.scanner.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerFilterChip
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.BackupState
import com.doclens.scanner.core.model.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onStartScan: () -> Unit,
    onContinueSession: (sessionId: String) -> Unit,
    onReviewSession: (sessionId: String) -> Unit,
    onOpenDocument: (documentId: String) -> Unit,
    onImportGallery: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    onPromptUnlock: ((documentId: String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showImportMenu by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var newDocTitle by remember { mutableStateOf("") }

    Scaffold(
        containerColor = ScannerTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = ScannerTheme.typography.screenTitle,
                            color = ScannerTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onOpenTools) {
                            Icon(Icons.Default.Build, contentDescription = "Tools", tint = ScannerTheme.colors.primary)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ScannerTheme.colors.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = "${uiState.documents.size} documents",
                        style = ScannerTheme.typography.compactBody,
                        color = ScannerTheme.colors.onSurfaceVariant
                    )
                }

                // Action Buttons: Dominant Gold Scan & Secondary Import
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        ScannerOutlinedButton(
                            text = stringResource(R.string.action_import),
                            onClick = { showImportMenu = true },
                            leadingIcon = Icons.Default.FolderOpen
                        )
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import_gallery)) },
                                onClick = {
                                    showImportMenu = false
                                    onImportGallery()
                                },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import_pdf)) },
                                onClick = {
                                    showImportMenu = false
                                    onImportPdf()
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ScannerPrimaryButton(
                        text = stringResource(R.string.action_scan),
                        onClick = onStartScan,
                        leadingIcon = Icons.Default.CameraAlt
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.action_search),
                        style = ScannerTheme.typography.body,
                        color = ScannerTheme.colors.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ScannerTheme.colors.onSurfaceVariant
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScannerTheme.colors.primary,
                    unfocusedBorderColor = ScannerTheme.colors.borderSubtle,
                    focusedTextColor = ScannerTheme.colors.onSurface,
                    unfocusedTextColor = ScannerTheme.colors.onSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    ScannerFilterChip(
                        selected = uiState.selectedFilter == DocumentFilter.ALL,
                        text = stringResource(R.string.action_filter_all),
                        onClick = { viewModel.setFilter(DocumentFilter.ALL) }
                    )
                }
                item {
                    ScannerFilterChip(
                        selected = uiState.selectedFilter == DocumentFilter.RECENT,
                        text = stringResource(R.string.action_filter_recent),
                        onClick = { viewModel.setFilter(DocumentFilter.RECENT) }
                    )
                }
                item {
                    ScannerFilterChip(
                        selected = uiState.selectedFilter == DocumentFilter.NOT_BACKED_UP,
                        text = stringResource(R.string.action_filter_not_backed_up),
                        onClick = { viewModel.setFilter(DocumentFilter.NOT_BACKED_UP) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session Recovery Card (Section 28.1 & 31)
            uiState.activeSession?.let { session ->
                ScannerCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = ScannerTheme.colors.warningAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.session_recovered_title),
                                style = ScannerTheme.typography.cardTitle,
                                color = ScannerTheme.colors.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.session_recovered_msg, uiState.sessionPageCount),
                            style = ScannerTheme.typography.body,
                            color = ScannerTheme.colors.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.discardSession(session.id) }
                            ) {
                                Text(
                                    text = stringResource(R.string.session_discard),
                                    style = ScannerTheme.typography.buttonLabel,
                                    color = ScannerTheme.colors.error
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            ScannerOutlinedButton(
                                text = stringResource(R.string.session_continue),
                                onClick = { onContinueSession(session.id) }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            ScannerPrimaryButton(
                                text = stringResource(R.string.session_review),
                                onClick = { onReviewSession(session.id) }
                            )
                        }
                    }
                }
            }

            // Documents List or Empty State
            if (uiState.documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ScannerTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_documents_title),
                            style = ScannerTheme.typography.cardTitle,
                            color = ScannerTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_documents_desc),
                            style = ScannerTheme.typography.body,
                            color = ScannerTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.documents, key = { it.id }) { document ->
                        DocumentItemCard(
                            document = document,
                            onClick = {
                                if (document.isLocked && onPromptUnlock != null) {
                                    onPromptUnlock(document.id)
                                } else {
                                    onOpenDocument(document.id)
                                }
                            },
                            onRename = {
                                documentToRename = document
                                newDocTitle = document.title
                            },
                            onToggleLock = {
                                viewModel.toggleLock(document.id, !document.isLocked)
                            },
                            onDelete = { viewModel.deleteDocument(document.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Rename Dialog
    documentToRename?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = {
                Text(
                    text = stringResource(R.string.action_rename),
                    style = ScannerTheme.typography.cardTitle
                )
            },
            text = {
                OutlinedTextField(
                    value = newDocTitle,
                    onValueChange = { newDocTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newDocTitle.isNotBlank()) {
                            viewModel.renameDocument(doc.id, newDocTitle.trim())
                        }
                        documentToRename = null
                    }
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = ScannerTheme.colors.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = ScannerTheme.colors.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun DocumentItemCard(
    document: Document,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(document.updatedAt) { dateFormat.format(Date(document.updatedAt)) }

    ScannerCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (document.isLocked) Icons.Default.Lock else Icons.Default.Description,
                contentDescription = if (document.isLocked) "Locked Document" else null,
                tint = if (document.isLocked) ScannerTheme.colors.warningAccent else ScannerTheme.colors.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (document.isLocked) "${document.title} 🔒" else document.title,
                    style = ScannerTheme.typography.cardTitle,
                    color = ScannerTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${document.pageCount} pages • $formattedDate",
                    style = ScannerTheme.typography.supportingLabel,
                    color = ScannerTheme.colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Accurate Local & Backup Status Badges (Section 28.1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (document.isLocked) {
                        ScannerStatusBadge(
                            text = "Locked (Biometric Protected)",
                            color = ScannerTheme.colors.warningAccent,
                            icon = Icons.Default.Lock
                        )
                    } else {
                        ScannerStatusBadge(
                            text = stringResource(R.string.status_saved_locally),
                            color = ScannerTheme.colors.success,
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    when (document.backupState) {
                        BackupState.BACKED_UP -> {
                            ScannerStatusBadge(
                                text = stringResource(R.string.status_backed_up),
                                color = ScannerTheme.colors.success,
                                icon = Icons.Default.CloudQueue
                            )
                        }
                        BackupState.BACKUP_PENDING -> {
                            ScannerStatusBadge(
                                text = stringResource(R.string.status_backup_pending),
                                color = ScannerTheme.colors.warningAccent,
                                icon = Icons.Default.Warning
                            )
                        }
                        BackupState.BACKUP_FAILED -> {
                            ScannerStatusBadge(
                                text = stringResource(R.string.status_backup_failed),
                                color = ScannerTheme.colors.error,
                                icon = Icons.Default.Warning
                            )
                        }
                        else -> {
                            ScannerStatusBadge(
                                text = stringResource(R.string.status_not_backed_up),
                                color = ScannerTheme.colors.warningText,
                                icon = Icons.Default.CloudQueue
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = ScannerTheme.colors.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (document.isLocked) "Unlock document" else "Lock with Biometrics") },
                        onClick = {
                            showMenu = false
                            onToggleLock()
                        },
                        leadingIcon = {
                            Icon(
                                if (document.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = ScannerTheme.colors.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = ScannerTheme.colors.error
                            )
                        }
                    )
                }
            }
        }
    }
}
