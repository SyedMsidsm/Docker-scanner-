package com.doclens.scanner.core.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubscriptionPlan(val title: String, val priceString: String) {
    FREE("Free Plan", "Free Forever"),
    PRO("DocLens Pro", "$4.99 / month"),
    TEAM("DocLens Team", "$14.99 / month")
}

data class UserEntitlements(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val ocrUnlimited: Boolean = true,
    val cloudStorageLimitBytes: Long = 2_000_000_000L, // 2 GB Free, 50 GB Pro
    val serverConversionQuotaPerMonth: Int = 10,       // 10 Free, 200 Pro
    val teamSharing: Boolean = false,
    val isAutoRenewing: Boolean = false,
    val expirationTimestamp: Long? = null
)

/**
 * Transparent Billing & Entitlements Manager (Phase 14 & IMP-09).
 *
 * Guarantees:
 * - Local-first document ownership: local scanning, local OCR, local PDF/JPG export,
 *   merging, splitting, and signing are ALWAYS 100% free and never restricted by subscription status.
 * - Downgrade or subscription expiry NEVER locks, deletes, or disables access to local documents.
 * - Entitlement gates apply strictly to server-side resources (cloud storage limits and cloud format conversions).
 */
class BillingManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("doclens_billing", Context.MODE_PRIVATE)

    private val _entitlements = MutableStateFlow(loadStoredEntitlements())
    val entitlements: StateFlow<UserEntitlements> = _entitlements.asStateFlow()

    private fun loadStoredEntitlements(): UserEntitlements {
        val planName = prefs.getString("user_plan", SubscriptionPlan.FREE.name) ?: SubscriptionPlan.FREE.name
        val plan = try {
            SubscriptionPlan.valueOf(planName)
        } catch (e: Exception) {
            SubscriptionPlan.FREE
        }

        return when (plan) {
            SubscriptionPlan.PRO -> UserEntitlements(
                plan = SubscriptionPlan.PRO,
                ocrUnlimited = true,
                cloudStorageLimitBytes = 50_000_000_000L, // 50 GB
                serverConversionQuotaPerMonth = 200,
                teamSharing = true,
                isAutoRenewing = true,
                expirationTimestamp = prefs.getLong("plan_expires_at", System.currentTimeMillis() + 30L * 86400_000L)
            )
            SubscriptionPlan.TEAM -> UserEntitlements(
                plan = SubscriptionPlan.TEAM,
                ocrUnlimited = true,
                cloudStorageLimitBytes = 500_000_000_000L, // 500 GB
                serverConversionQuotaPerMonth = 1000,
                teamSharing = true,
                isAutoRenewing = true,
                expirationTimestamp = prefs.getLong("plan_expires_at", System.currentTimeMillis() + 30L * 86400_000L)
            )
            SubscriptionPlan.FREE -> UserEntitlements(
                plan = SubscriptionPlan.FREE,
                ocrUnlimited = true,
                cloudStorageLimitBytes = 2_000_000_000L, // 2 GB
                serverConversionQuotaPerMonth = 10,
                teamSharing = false,
                isAutoRenewing = false,
                expirationTimestamp = null
            )
        }
    }

    /**
     * Updates the user's plan and synchronizes state.
     */
    fun setPlan(plan: SubscriptionPlan) {
        val now = System.currentTimeMillis()
        val expiresAt = if (plan != SubscriptionPlan.FREE) now + 30L * 86400_000L else 0L

        prefs.edit()
            .putString("user_plan", plan.name)
            .putLong("plan_expires_at", expiresAt)
            .apply()

        _entitlements.value = loadStoredEntitlements()
    }

    /**
     * Simulates store purchase flow verification.
     */
    fun purchasePro(): Boolean {
        setPlan(SubscriptionPlan.PRO)
        return true
    }

    /**
     * Restores previous purchases.
     */
    fun restorePurchases(): UserEntitlements {
        _entitlements.value = loadStoredEntitlements()
        return _entitlements.value
    }

    /**
     * Verifies whether cloud upload is allowed based on storage limits.
     * Note: Local document saving is ALWAYS allowed regardless of this check.
     */
    fun canUploadCloudBytes(currentUsedBytes: Long, newBytes: Long): Boolean {
        return (currentUsedBytes + newBytes) <= _entitlements.value.cloudStorageLimitBytes
    }

    /**
     * Verifies whether server-side format conversion is allowed.
     */
    fun canPerformServerConversion(currentMonthConversions: Int): Boolean {
        return currentMonthConversions < _entitlements.value.serverConversionQuotaPerMonth
    }
}
