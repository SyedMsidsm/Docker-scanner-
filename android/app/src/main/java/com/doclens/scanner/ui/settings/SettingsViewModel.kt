package com.doclens.scanner.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doclens.scanner.core.billing.BillingManager
import com.doclens.scanner.core.billing.SubscriptionPlan
import com.doclens.scanner.core.billing.UserEntitlements
import com.doclens.scanner.core.model.AppearanceSettings
import com.doclens.scanner.core.model.AppTextSize
import com.doclens.scanner.core.model.ContentNetworkMode
import com.doclens.scanner.core.model.PrivacySettings
import com.doclens.scanner.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val entitlements: UserEntitlements = UserEntitlements()
)

class SettingsViewModel(
    private val billingManager: BillingManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        billingManager?.let { bm ->
            viewModelScope.launch {
                bm.entitlements.collect { ent ->
                    _uiState.value = _uiState.value.copy(entitlements = ent)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(
            appearance = _uiState.value.appearance.copy(themeMode = mode)
        )
    }

    fun setTextSize(size: AppTextSize) {
        _uiState.value = _uiState.value.copy(
            appearance = _uiState.value.appearance.copy(textSize = size)
        )
    }

    fun setContentNetworkMode(mode: ContentNetworkMode) {
        _uiState.value = _uiState.value.copy(
            privacy = _uiState.value.privacy.copy(networkMode = mode)
        )
    }

    fun upgradeToPro() {
        billingManager?.purchasePro()
    }

    fun downgradeToFree() {
        billingManager?.setPlan(SubscriptionPlan.FREE)
    }

    fun restorePurchases() {
        billingManager?.restorePurchases()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val billing = BillingManager(context.applicationContext)
            return SettingsViewModel(billing) as T
        }
    }
}
