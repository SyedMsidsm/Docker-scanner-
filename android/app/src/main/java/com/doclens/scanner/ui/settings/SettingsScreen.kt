package com.doclens.scanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doclens.scanner.R
import com.doclens.scanner.core.billing.SubscriptionPlan
import com.doclens.scanner.core.designsystem.ScannerCard
import com.doclens.scanner.core.designsystem.ScannerFilterChip
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerTheme
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Restore
import com.doclens.scanner.core.model.AppTextSize
import com.doclens.scanner.core.model.ContentNetworkMode
import com.doclens.scanner.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenComponentGallery: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = ScannerTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
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
                .padding(16.dp)
        ) {
            // Theme Mode
            Text(
                text = stringResource(R.string.settings_theme),
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScannerFilterChip(
                    selected = uiState.appearance.themeMode == ThemeMode.DARK,
                    text = stringResource(R.string.theme_dark),
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
                ScannerFilterChip(
                    selected = uiState.appearance.themeMode == ThemeMode.LIGHT,
                    text = stringResource(R.string.theme_light),
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                ScannerFilterChip(
                    selected = uiState.appearance.themeMode == ThemeMode.SYSTEM,
                    text = stringResource(R.string.theme_system),
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Text Sizing (Section 30.5)
            Text(
                text = stringResource(R.string.settings_text_size),
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTextSize.values().forEach { size ->
                    ScannerFilterChip(
                        selected = uiState.appearance.textSize == size,
                        text = stringResource(size.labelRes),
                        onClick = { viewModel.setTextSize(size) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Typography Preview Card
            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Live Typography Preview (${(uiState.appearance.textSize.multiplier * 100).toInt()}%)",
                        style = ScannerTheme.typography.cardTitle,
                        color = ScannerTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This preview scales smoothly with your selection and honors Android system font scaling. Document pages will never be altered by UI font size changes.",
                        style = ScannerTheme.typography.body,
                        color = ScannerTheme.colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy & Content Network Gate (IMP-05)
            Text(
                text = stringResource(R.string.settings_privacy),
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScannerFilterChip(
                    selected = uiState.privacy.networkMode == ContentNetworkMode.LOCAL_ONLY,
                    text = stringResource(R.string.privacy_local_only),
                    onClick = { viewModel.setContentNetworkMode(ContentNetworkMode.LOCAL_ONLY) }
                )
                ScannerFilterChip(
                    selected = uiState.privacy.networkMode == ContentNetworkMode.ASK_BEFORE_UPLOAD,
                    text = stringResource(R.string.privacy_ask_before_upload),
                    onClick = { viewModel.setContentNetworkMode(ContentNetworkMode.ASK_BEFORE_UPLOAD) }
                )
                ScannerFilterChip(
                    selected = uiState.privacy.networkMode == ContentNetworkMode.CLOUD_ENABLED,
                    text = stringResource(R.string.privacy_cloud_enabled),
                    onClick = { viewModel.setContentNetworkMode(ContentNetworkMode.CLOUD_ENABLED) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan & Entitlements (Phase 14 & IMP-09)
            Text(
                text = "Plan & Entitlements",
                style = ScannerTheme.typography.sectionTitle,
                color = ScannerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScannerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.entitlements.plan.title,
                            style = ScannerTheme.typography.cardTitle,
                            color = ScannerTheme.colors.primary
                        )
                        Text(
                            text = uiState.entitlements.plan.priceString,
                            style = ScannerTheme.typography.buttonLabel,
                            color = ScannerTheme.colors.onSurfaceVariant
                        )
                    }

                    Divider(color = ScannerTheme.colors.borderSubtle, thickness = 1.dp)

                    Text(
                        text = "• Local Scans, OCR & Export: Unlimited (Always Free)",
                        style = ScannerTheme.typography.compactBody,
                        color = ScannerTheme.colors.onSurface
                    )
                    Text(
                        text = "• Cloud Storage: ${uiState.entitlements.cloudStorageLimitBytes / 1_000_000_000} GB included",
                        style = ScannerTheme.typography.compactBody,
                        color = ScannerTheme.colors.onSurface
                    )
                    Text(
                        text = "• Cloud Format Conversions: ${uiState.entitlements.serverConversionQuotaPerMonth} / month",
                        style = ScannerTheme.typography.compactBody,
                        color = ScannerTheme.colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Honest Ownership: Local scanning and exports are never locked or held hostage upon plan change.",
                        style = ScannerTheme.typography.compactBody,
                        color = ScannerTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.entitlements.plan == SubscriptionPlan.FREE) {
                        ScannerPrimaryButton(
                            text = "Upgrade to Pro ($4.99/mo)",
                            onClick = { viewModel.upgradeToPro() },
                            leadingIcon = Icons.Default.Star,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ScannerOutlinedButton(
                            text = "Revert to Free Plan",
                            onClick = { viewModel.downgradeToFree() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ScannerOutlinedButton(
                        text = "Restore Purchases",
                        onClick = { viewModel.restorePurchases() },
                        leadingIcon = Icons.Default.Restore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Component Gallery (for Design-System Gate Verification)
            ScannerOutlinedButton(
                text = "View Component Gallery (Design System QA)",
                onClick = onOpenComponentGallery,
                leadingIcon = Icons.Default.Widgets,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
