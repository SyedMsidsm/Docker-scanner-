package com.doclens.scanner.core.security

import android.content.Context
import com.doclens.scanner.core.model.ContentNetworkMode

object PrivacyManager {
    private const val PREFS_NAME = "doclens_privacy"
    private const val KEY_NETWORK_MODE = "network_mode"

    fun getNetworkMode(context: Context): ContentNetworkMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_NETWORK_MODE, ContentNetworkMode.LOCAL_ONLY.name)
        return try {
            ContentNetworkMode.valueOf(modeStr ?: ContentNetworkMode.LOCAL_ONLY.name)
        } catch (e: Exception) {
            ContentNetworkMode.LOCAL_ONLY
        }
    }

    fun setNetworkMode(context: Context, mode: ContentNetworkMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NETWORK_MODE, mode.name).apply()
    }

    fun isLocalOnly(context: Context): Boolean {
        return getNetworkMode(context) == ContentNetworkMode.LOCAL_ONLY
    }
}
