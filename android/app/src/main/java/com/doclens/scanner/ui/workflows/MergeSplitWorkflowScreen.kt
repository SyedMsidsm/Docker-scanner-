package com.doclens.scanner.ui.workflows

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.Document
import com.doclens.scanner.data.repository.DocumentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeSplitWorkflowScreen(
    repository: DocumentRepository,
    onBack: () -> Unit,
    onMerged: (newDocId: String) -> Unit,
    onSplitComplete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val documents by repository.observeDocuments().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "Merge Documents" else "Split Document",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ScannerTheme.colors.surface,
                contentColor = ScannerTheme.colors.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Merge Multiple", style = ScannerTheme.typography.buttonLabel) },
                    icon = { Icon(Icons.Default.CallMerge, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Split Document", style = ScannerTheme.typography.buttonLabel) },
                    icon = { Icon(Icons.Default.CallSplit, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                MergeTabContent(
                    documents = documents,
                    onMerge = { selectedDocIds, mergedTitle ->
                        scope.launch {
                            val newDocId = repository.mergeDocuments(selectedDocIds, mergedTitle)
                            onMerged(newDocId)
                        }
                    }
                )
            } else {
                SplitTabContent(
                    documents = documents.filter { it.pageCount > 1 },
                    onSplit = { docId, splitAfter, title1, title2 ->
                        scope.launch {
                            repository.splitDocument(docId, splitAfter, title1, title2)
                            onSplitComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MergeTabContent(
    documents: List<Document>,
    onMerge: (List<String>, String) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var mergedTitle by remember { mutableStateOf("Merged Document") }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = mergedTitle,
            onValueChange = { mergedTitle = it },
            label = { Text("Merged Document Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScannerTheme.colors.primary,
                unfocusedBorderColor = ScannerTheme.colors.borderSubtle
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Select 2 or more documents to merge in order (${selectedIds.size} selected):",
            style = ScannerTheme.typography.compactBody,
            color = ScannerTheme.colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(documents, key = { it.id }) { doc ->
                val isSelected = selectedIds.contains(doc.id)
                ScannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isSelected) selectedIds.remove(doc.id)
                        else selectedIds.add(doc.id)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedIds.add(doc.id)
                                else selectedIds.remove(doc.id)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ScannerTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ScannerTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                style = ScannerTheme.typography.cardTitle,
                                color = ScannerTheme.colors.onSurface
                            )
                            Text(
                                text = "${doc.pageCount} pages",
                                style = ScannerTheme.typography.compactBody,
                                color = ScannerTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isProcessing) {
            CircularProgressIndicator(
                color = ScannerTheme.colors.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            ScannerPrimaryButton(
                text = "Merge ${selectedIds.size} Documents",
                enabled = selectedIds.size >= 2 && mergedTitle.isNotBlank(),
                onClick = {
                    isProcessing = true
                    onMerge(selectedIds.toList(), mergedTitle.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.CallMerge
            )
        }
    }
}

@Composable
private fun SplitTabContent(
    documents: List<Document>,
    onSplit: (String, Int, String, String) -> Unit
) {
    var selectedDoc by remember { mutableStateOf<Document?>(documents.firstOrNull()) }
    var splitAfterPage by remember { mutableIntStateOf(0) }
    var title1 by remember { mutableStateOf("") }
    var title2 by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val currentDoc = selectedDoc

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (documents.isEmpty()) {
            Text(
                text = "No documents with 2 or more pages found in your library to split.",
                style = ScannerTheme.typography.body,
                color = ScannerTheme.colors.onSurfaceVariant
            )
            return
        }

        Text(
            text = "Select Document to Split:",
            style = ScannerTheme.typography.compactBody,
            color = ScannerTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(documents, key = { it.id }) { doc ->
                val isSelected = selectedDoc?.id == doc.id
                ScannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedDoc = doc
                        splitAfterPage = 0
                        title1 = "${doc.title} - Part 1"
                        title2 = "${doc.title} - Part 2"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ScannerTheme.colors.primary)
                        } else {
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${doc.title} (${doc.pageCount} pages)",
                            style = ScannerTheme.typography.cardTitle,
                            color = ScannerTheme.colors.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentDoc != null && currentDoc.pageCount > 1) {
            Text(
                text = "Split point: Split after Page ${splitAfterPage + 1} (Part 1: 1..${splitAfterPage + 1}, Part 2: ${splitAfterPage + 2}..${currentDoc.pageCount})",
                style = ScannerTheme.typography.compactBody,
                color = ScannerTheme.colors.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until (currentDoc.pageCount - 1)) {
                    val isChosen = splitAfterPage == i
                    ScannerCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                splitAfterPage = i
                                if (title1.isBlank()) title1 = "${currentDoc.title} - Part 1"
                                if (title2.isBlank()) title2 = "${currentDoc.title} - Part 2"
                            }
                    ) {
                        Text(
                            text = "Page ${i + 1}",
                            style = ScannerTheme.typography.buttonLabel,
                            color = if (isChosen) ScannerTheme.colors.primary else ScannerTheme.colors.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title1.ifBlank { "${currentDoc.title} - Part 1" },
                onValueChange = { title1 = it },
                label = { Text("Part 1 Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title2.ifBlank { "${currentDoc.title} - Part 2" },
                onValueChange = { title2 = it },
                label = { Text("Part 2 Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isProcessing) {
                CircularProgressIndicator(
                    color = ScannerTheme.colors.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                ScannerPrimaryButton(
                    text = "Split into 2 Documents",
                    onClick = {
                        isProcessing = true
                        val finalT1 = title1.ifBlank { "${currentDoc.title} - Part 1" }
                        val finalT2 = title2.ifBlank { "${currentDoc.title} - Part 2" }
                        onSplit(currentDoc.id, splitAfterPage, finalT1, finalT2)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.CallSplit
                )
            }
        }
    }
}
