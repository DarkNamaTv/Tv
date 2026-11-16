package com.darktv.player

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object SubscriptionLock {
    private const val PREFS = "subscription_prefs"
    private const val KEY_SUBSCRIPTION_CODE = "sub_code"
    private const val KEY_SUBSCRIPTION_EXPIRY = "sub_expiry"
    private const val KEY_SUBSCRIPTION_DURATION = "sub_duration"
    private const val KEY_SUBSCRIPTION_DURATION_DAYS = "sub_duration_days"
    private const val KEY_SUBSCRIPTION_UNLIMITED = "sub_unlimited"

    // Duration codes used in PIN format: CODE|DAYS|EXPIRY_TIMESTAMP
    // DAYS: -1 = unlimited, 0 = revoked/invalid, >0 = number of days
    const val CODE_3D   = "3D"
    const val CODE_6D   = "6D"
    const val CODE_16D  = "16D"
    const val CODE_1M   = "1M"
    const val CODE_2M   = "2M"
    const val CODE_3M   = "3M"
    const val CODE_UNL  = "UNL"
    const val CODE_REV   = "REV" // revoked

    private val DURATION_DAYS = mapOf(
        CODE_3D  to 3,
        CODE_6D  to 6,
        CODE_16D to 16,
        CODE_1M  to 30,
        CODE_2M  to 60,
        CODE_3M  to 90,
        CODE_UNL to -1,
        CODE_REV to 0
    )

    private val DURATION_LABELS_FA = mapOf(
        CODE_3D  to "۳ روزه",
        CODE_6D  to "۶ روزه",
        CODE_16D to "۱۶ روزه",
        CODE_1M  to "۱ ماهه",
        CODE_2M  to "۲ ماهه",
        CODE_3M  to "۳ ماهه",
        CODE_UNL to "نامحدود",
        CODE_REV to "لغو شده"
    )

    private val DURATION_LABELS_EN = mapOf(
        CODE_3D  to "3 Days",
        CODE_6D  to "6 Days",
        CODE_16D to "16 Days",
        CODE_1M  to "1 Month",
        CODE_2M  to "2 Months",
        CODE_3M  to "3 Months",
        CODE_UNL to "Unlimited",
        CODE_REV to "Revoked"
    )

    // Admin-managed revocation list (comma-separated expiry timestamps to revoke)
    // When admin wants to revoke, add the user's expiry timestamp to this string
    // stored on GitHub or accessible via webhook
    private const val REVOCATION_LIST_URL = ""

    private fun getPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Parse PIN: format "DURATION|EXPIRY_TIMESTAMP|SIGNATURE"
    // e.g. "1M|1732492800|a1b2c3..."
    fun validatePin(ctx: Context, pin: String): ValidationResult? {
        val parts = pin.trim().split("|")
        if (parts.size < 2) return null

        val code = parts[0].uppercase()
        val expiryStr = parts[1]
        val expiryTimestamp = expiryStr.toLongOrNull() ?: return null

        if (!DURATION_DAYS.containsKey(code)) return null

        val now = System.currentTimeMillis() / 1000
        if (expiryTimestamp < now && DURATION_DAYS[code] != -1L) return ValidationResult.Expired

        // Check revocation (if list URL is configured, fetch it)
        // For now: check local revocation marker
        val prefs = getPrefs(ctx)
        val revokedCode = prefs.getString(KEY_SUBSCRIPTION_CODE, "")
        if (revokedCode == code && prefs.getBoolean("revoked_$code", false)) {
            return ValidationResult.Revoked
        }

        val days = DURATION_DAYS[code] ?: 0
        return ValidationResult.Valid(code = code, expiryTimestamp = expiryTimestamp, days = days)
    }

    fun activateSubscription(ctx: Context, result: ValidationResult.Valid) {
        val prefs = getPrefs(ctx)
        prefs.edit().apply {
            putString(KEY_SUBSCRIPTION_CODE, result.code)
            putLong(KEY_SUBSCRIPTION_EXPIRY, result.expiryTimestamp)
            putInt(KEY_SUBSURATION_DURATION_DAYS, result.days)
            putBoolean(KEY_SUBSCRIPTION_UNLIMITED, result.days == -1)
            apply()
        }
    }

    fun isAccessActive(ctx: Context): Boolean {
        val prefs = getPrefs(ctx)
        if (!prefs.contains(KEY_SUBSCRIPTION_EXPIRY)) return false

        val expiry = prefs.getLong(KEY_SUBSCRIPTION_EXPIRY, 0)
        val unlimited = prefs.getBoolean(KEY_SUBSCRIPTION_UNLIMITED, false)
        val revoked = prefs.getBoolean("revoked_${prefs.getString(KEY_SUBSCRIPTION_CODE,"")}", false)

        if (revoked) return false
        if (unlimited) return true

        val now = System.currentTimeMillis() / 1000
        return now < expiry
    }

    fun getRemainingDays(ctx: Context): Long {
        val prefs = getPrefs(ctx)
        if (!prefs.contains(KEY_SUBSCRIPTION_EXPIRY)) return -1

        val expiry = prefs.getLong(KEY_SUBSCRIPTION_EXPIRY, 0)
        val unlimited = prefs.getBoolean(KEY_SUBSCRIPTION_UNLIMITED, false)
        if (unlimited) return -1

        val now = System.currentTimeMillis() / 1000
        val remaining = expiry - now
        return if (remaining > 0) TimeUnit.SECONDS.toDays(remaining) else -1
    }

    fun getExpiryDate(ctx: Context): String {
        val prefs = getPrefs(ctx)
        val expiry = prefs.getLong(KEY_SUBSCRIPTION_EXPIRY, 0)
        val unlimited = prefs.getBoolean(KEY_SUBSCRIPTION_UNLIMITED, false)
        if (unlimited) return "∞"
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(expiry * 1000))
        return date
    }

    fun getSubscriptionCode(ctx: Context): String =
        getPrefs(ctx).getString(KEY_SUBSCRIPTION_CODE, "") ?: ""

    fun getDurationLabel(ctx: Context, english: Boolean): String {
        val code = getSubscriptionCode(ctx)
        val labels = if (english) DURATION_LABELS_EN else DURATION_LABELS_FA
        return labels[code] ?: ""
    }

    fun clear(ctx: Context) {
        val prefs = getPrefs(ctx)
        prefs.edit().clear().apply()
    }

    fun revokeLocally(ctx: Context, code: String) {
        val prefs = getPrefs(ctx)
        prefs.edit().putBoolean("revoked_$code", true).apply()
    }

    fun tr(fa: String, en: String, english: Boolean) = if (english) en else fa

    fun getDurationInfo(ctx: Context, english: Boolean): Pair<String, String> {
        val code = getSubscriptionCode(ctx)
        val labels = if (english) DURATION_LABELS_EN else DURATION_LABELS_FA
        val label = labels[code] ?: tr("نامشخص", "Unknown")
        val expiry = getExpiryDate(ctx)
        return label to expiry
    }

    sealed class ValidationResult {
        data object Expired : ValidationResult()
        data object Revoked : ValidationResult()
        data class Valid(
            val code: String,
            val expiryTimestamp: Long,
            val days: Int
        ) : ValidationResult()
    }

    // Generate PIN for admin use
    // Admin calls: generatePin(ctx, "1M") -> "1M|1732492800|SIGNATURE"
    fun generatePin(ctx: Context, durationCode: String): String {
        val days = DURATION_DAYS[durationCode] ?: return ""
        val expiry = if (days == -1) Long.MAX_VALUE / 1000
                     else System.currentTimeMillis() / 1000 + (days * 86400L)
        val sig = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest("$durationCode|$expiry".toByteArray())
                .copyOf(8),
            Base64.NO_WRAP
        )
        return "$durationCode|$expiry|$sig"
    }
}
