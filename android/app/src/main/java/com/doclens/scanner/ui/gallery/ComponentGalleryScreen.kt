package com.doclens.scanner.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerDestructiveButton
import com.doclens.scanner.core.designsystem.ScannerFilterChip
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerStatusBadge
import com.doclens.scanner.core.designsystem.ScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var chipSelected by remember { mutableStateOf(true) }
    var testInput by remember { mutableStateOf("DocLens Scanner Verification") }

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Component Gallery (Design System)",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Typography Section
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Typography Hierarchy", style = ScannerTheme.typography.sectionTitle, color = ScannerTheme.colors.primary)
                    Text("Screen Title (30sp)", style = ScannerTheme.typography.screenTitle)
                    Text("Section Title (22sp)", style = ScannerTheme.typography.sectionTitle)
                    Text("Card Title (20sp)", style = ScannerTheme.typography.cardTitle)
                    Text("Body Text (16sp) - The quick brown fox jumps over the lazy dog.", style = ScannerTheme.typography.body)
                    Text("Compact Body (14sp) - Supporting descriptive content.", style = ScannerTheme.typography.compactBody)
                    Text("Button Label (14sp Medium)", style = ScannerTheme.typography.buttonLabel)
                    Text("Supporting Label (12sp)", style = ScannerTheme.typography.supportingLabel, color = ScannerTheme.colors.onSurfaceVariant)
                }
            }

            // Buttons Section
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Interactive Controls & Buttons", style = ScannerTheme.typography.sectionTitle, color = ScannerTheme.colors.primary)

                    ScannerPrimaryButton(
                        text = "Primary Gold Button (onPrimary #111411)",
                        onClick = {},
                        leadingIcon = Icons.Default.CameraAlt,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ScannerPrimaryButton(
                        text = "Loading Primary Button",
                        onClick = {},
                        isLoading = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ScannerOutlinedButton(
                        text = "Secondary Outlined Button",
                        onClick = {},
                        leadingIcon = Icons.Default.FolderOpen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ScannerDestructiveButton(
                        text = "Destructive Error Button",
                        onClick = {},
                        leadingIcon = Icons.Default.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Status Badges (Color Contrast & Semantic Indicators)
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Status Badges (WCAG Compliant Ratios)", style = ScannerTheme.typography.sectionTitle, color = ScannerTheme.colors.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScannerStatusBadge(
                            text = "Saved on this phone",
                            color = ScannerTheme.colors.success,
                            icon = Icons.Default.CheckCircle
                        )
                        ScannerStatusBadge(
                            text = "Latest changes pending",
                            color = ScannerTheme.colors.warningAccent,
                            icon = Icons.Default.Warning
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScannerStatusBadge(
                            text = "Not backed up (Warning text)",
                            color = ScannerTheme.colors.warningText,
                            icon = Icons.Default.Warning
                        )
                        ScannerStatusBadge(
                            text = "Operation failed",
                            color = ScannerTheme.colors.error,
                            icon = Icons.Default.Warning
                        )
                    }
                }
            }

            // Form Inputs & Progress
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Form Inputs & Progress Indicators", style = ScannerTheme.typography.sectionTitle, color = ScannerTheme.colors.primary)

                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScannerFilterChip(
                            selected = chipSelected,
                            text = "Active Filter Chip",
                            onClick = { chipSelected = !chipSelected }
                        )
                        ScannerFilterChip(
                            selected = !chipSelected,
                            text = "Inactive Filter Chip",
                            onClick = { chipSelected = !chipSelected }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Determinate Progress (75%)", style = ScannerTheme.typography.supportingLabel)
                    LinearProgressIndicator(
                        progress = { 0.75f },
                        color = ScannerTheme.colors.primary,
                        trackColor = ScannerTheme.colors.borderSubtle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
