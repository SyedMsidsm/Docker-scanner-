package com.doclens.scanner.core.model

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppTextSize(val multiplier: Float, val labelRes: Int) {
    SMALL(0.90f, com.doclens.scanner.R.string.text_size_small),
    DEFAULT(1.00f, com.doclens.scanner.R.string.text_size_default),
    LARGE(1.15f, com.doclens.scanner.R.string.text_size_large),
    EXTRA_LARGE(1.30f, com.doclens.scanner.R.string.text_size_extra_large)
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val textSize: AppTextSize = AppTextSize.DEFAULT
)

enum class ContentNetworkMode {
    LOCAL_ONLY,
    ASK_BEFORE_UPLOAD,
    CLOUD_ENABLED
}

data class PrivacySettings(
    val networkMode: ContentNetworkMode = ContentNetworkMode.LOCAL_ONLY
)
