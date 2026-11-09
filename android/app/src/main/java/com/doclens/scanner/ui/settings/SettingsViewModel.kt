package com.doclens.scanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.doclens.scanner.core.model.AppearanceSettings
import com.doclens.scanner.core.model.AppTextSize
import com.doclens.scanner.core.model.ContentNetworkMode
import com.doclens.scanner.core.model.PrivacySettings
import com.doclens.scanner.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val privacy: PrivacySettings = PrivacySettings()
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel() as T
        }
    }
}
