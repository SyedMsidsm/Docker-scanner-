package com.doclens.scanner.ui.workflows

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doclens.scanner.R
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowsScreen(
    onBack: () -> Unit,
    onOpenIdCopy: () -> Unit,
    onOpenMergeSplit: () -> Unit,
    onStartScan: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_tools),
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
                text = "Guided Task Workflows",
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.primary
            )

            // ID Copy Workflow Card
            WorkflowActionCard(
                title = "ID Card Copy (Front & Back)",
                description = "Combines both sides of an ID or license onto a single printable page with an optional security purpose watermark.",
                icon = Icons.Default.CreditCard,
                onClick = onOpenIdCopy
            )

            // Merge & Split Documents (PDF-03)
            WorkflowActionCard(
                title = "Merge & Split Scans",
                description = "Combine multiple scan documents into a unified multi-page document or split an existing document into multiple parts.",
                icon = Icons.Default.CallMerge,
                onClick = onOpenMergeSplit
            )

            // Sign & Return Workflow Card
            WorkflowActionCard(
                title = "Sign & Return",
                description = "Import or scan any agreement, draw an electronic signature with transparent backing, and export for return.",
                icon = Icons.Default.Draw,
                onClick = onStartScan
            )

            // Receipt Workflow Card
            WorkflowActionCard(
                title = "Receipt & Expense Scanner",
                description = "Scan faint paper receipts with B&W contrast boost and extract line items and totals with on-device OCR.",
                icon = Icons.Default.Receipt,
                onClick = onStartScan
            )

            // Search in Document Text
            WorkflowActionCard(
                title = "Full-Text Document Search",
                description = "Search across recognized text from all stored scans and jump directly to matching pages.",
                icon = Icons.Default.Search,
                onClick = onOpenSearch
            )
        }
    }
}

@Composable
private fun WorkflowActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ScannerCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ScannerTheme.colors.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ScannerTheme.typography.cardTitle,
                    color = ScannerTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = ScannerTheme.typography.body,
                    color = ScannerTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
