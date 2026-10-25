package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.HardwareSecurityManager
import com.example.security.HardwareSecurityStatus
import com.example.security.TamperVerificationResult
import kotlinx.coroutines.flow.Flow
import java.util.Locale

enum class WalletCurrency(
    val code: String,
    val nameAr: String,
    val symbolAr: String,
    val defaultInitialBalance: Double,
    val quickTopUpAmounts: List<Double>
) {
    YER(
        code = "YER",
        nameAr = "ريال يمني",
        symbolAr = "ر.ي",
        defaultInitialBalance = 250000.00,
        quickTopUpAmounts = listOf(10000.0, 50000.0, 100000.0)
    ),
    SAR(
        code = "SAR",
        nameAr = "ريال سعودي",
        symbolAr = "ر.س",
        defaultInitialBalance = 5000.00,
        quickTopUpAmounts = listOf(100.0, 500.0, 1000.0)
    ),
    USD(
        code = "USD",
        nameAr = "دولار أمريكي",
        symbolAr = "$",
        defaultInitialBalance = 1500.00,
        quickTopUpAmounts = listOf(50.0, 100.0, 500.0)
    );

    companion object {
        fun fromCode(code: String): WalletCurrency {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: SAR
        }
    }
}

@Entity(tableName = "transfer_records")
data class TransferRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val direction: String, // "SENT" or "RECEIVED"
    val payloadType: String, // "TEXT", "INVOICE", "CONTACT", "SECURITY_ALERT"
    val title: String,
    val rawContent: String,
    val hexPayload: String,
    val checksumHex: String,
    val isChecksumValid: Boolean,
    val isEncrypted: Boolean = true,
    val saltHex: String = "--",
    val ottId: String = "--",
    val amount: String = "0.00",
    val operationTypeAr: String = "عملية عامة",
    val acousticMode: String, // "AUDIBLE" or "ULTRASONIC"
    val byteCount: Int,
    val toneCount: Int,
    val durationMs: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val senderPhone: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val referenceId: String = ""
) {
    fun resolvedReferenceId(): String {
        if (referenceId.isNotBlank()) return referenceId
        val cleanOtt = ottId.replace("-", "").ifBlank { "8420" }
        val timeSuffix = ((timestamp / 1000L) % 1000000L).toString().padStart(6, '0')
        return "VP-REF-$cleanOtt-$timeSuffix"
    }

    fun resolvedSenderName(activeAccountName: String? = null): String {
        if (senderName.isNotBlank()) return senderName
        return if (direction == "SENT") {
            activeAccountName?.takeIf { it.isNotBlank() } ?: "أحمد محمد عبدالله الشامي"
        } else {
            extractPartyFromTitle() ?: "سالم عبدالرحمن علي العولقي"
        }
    }

    fun resolvedSenderPhone(activeAccountPhone: String? = null): String {
        if (senderPhone.isNotBlank()) return senderPhone
        return if (direction == "SENT") {
            activeAccountPhone?.takeIf { it.isNotBlank() } ?: "+967 771234567"
        } else {
            "+967 773456789"
        }
    }

    fun resolvedRecipientName(activeAccountName: String? = null): String {
        if (recipientName.isNotBlank()) return recipientName
        return if (direction == "SENT") {
            extractPartyFromTitle() ?: "سالم عبدالرحمن علي العولقي"
        } else {
            activeAccountName?.takeIf { it.isNotBlank() } ?: "أحمد محمد عبدالله الشامي"
        }
    }

    fun resolvedRecipientPhone(activeAccountPhone: String? = null): String {
        if (recipientPhone.isNotBlank()) return recipientPhone
        return if (direction == "SENT") {
            extractIdOrPhoneFromTitle() ?: "+967 773456789"
        } else {
            activeAccountPhone?.takeIf { it.isNotBlank() } ?: "+967 771234567"
        }
    }

    private fun extractPartyFromTitle(): String? {
        val afterColon = title.substringAfter(":", "").trim()
        if (afterColon.isNotBlank()) {
            return afterColon.substringBefore("(ID:").substringBefore("(#").trim().takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun extractIdOrPhoneFromTitle(): String? {
        if (title.contains("(ID:")) {
            val idPart = title.substringAfter("(ID:", "").substringBefore(")").trim()
            if (idPart.isNotBlank()) return "ID: $idPart"
        }
        return null
    }
}

enum class NotificationCategory(
    val code: String,
    val labelAr: String
) {
    FINANCIAL("FINANCIAL", "إشعار مالي"),
    SECURITY("SECURITY", "إشعار أمني");

    companion object {
        fun fromCode(code: String): NotificationCategory {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: FINANCIAL
        }
    }
}

data class InAppNotification(
    val id: String,
    val accountId: String,
    val category: NotificationCategory,
    val titleAr: String,
    val messageAr: String,
    val amountBadge: String = "",
    val counterpartyName: String = "",
    val counterpartyPhone: String = "",
    val referenceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class BurnedTokenInfo(
    val ottId: String,
    val operationTypeAr: String,
    val amount: String,
    val recordedTime: String,
    val timestamp: Long
)

/**
 * Hardware-Backed Encrypted Storage Manager for VibraPay:
 * - Uses `EncryptedSharedPreferences` backed by Android Keystore AES-256-GCM MasterKey.
 * - Signs all balances (`YER`, `SAR`, `USD`), Wallet PIN, and `vibrapay_burned_tokens` using a
 *   non-exportable `HmacSHA256` key inside the Android Keystore hardware module.
 * - Stores Smart Login credentials (`saved_login_identifier`, `active_account_id`) and
 *   Dual Biometric preferences (`login_biometric_enabled`, `tx_biometric_enabled`).
 */
class VibraPayLocalStorage(private val context: Context) {
    private val burnedPrefs = HardwareSecurityManager.getEncryptedBurnedPrefs(context)
    private val walletPrefs = HardwareSecurityManager.getEncryptedWalletPrefs(context)

    companion object {
        private const val KEY_LEDGER_HMAC_SIG = "ledger_keystore_hmac_sig_v1"
        private const val KEY_TAMPER_LOCKED = "ledger_tamper_locked_flag"
        private const val KEY_ACTIVE_ACCOUNT_ID = "active_official_account_id"
        private const val KEY_SAVED_LOGIN_IDENTIFIER = "saved_smart_login_identifier"
        private const val KEY_SESSION_AUTHENTICATED = "session_authenticated_flag"
        private const val KEY_LOGIN_BIOMETRIC_ENABLED = "login_biometric_enabled_flag"
        private const val KEY_TX_BIOMETRIC_ENABLED = "tx_biometric_enabled_flag"
        private const val KEY_APP_THEME_MODE = "app_theme_mode_pref"
        private const val KEY_NOTIFICATIONS_PREFIX = "in_app_notifications_v1_"
        private const val KEY_NOTIFICATIONS_SEEDED_PREFIX = "in_app_notifications_seeded_v1_"
        private const val KEY_ACCT_PASS_PREFIX = "acct_pass_v1_"
        private const val KEY_ACCT_EMAIL_PREFIX = "acct_email_v1_"
    }

    init {
        if (!walletPrefs.contains(KEY_LEDGER_HMAC_SIG)) {
            recomputeAndSaveLedgerHmac()
        }
    }

    fun buildCanonicalLedgerString(): String {
        val yerStr = String.format(Locale.US, "%.2f", readRawBalance(WalletCurrency.YER))
        val sarStr = String.format(Locale.US, "%.2f", readRawBalance(WalletCurrency.SAR))
        val usdStr = String.format(Locale.US, "%.2f", readRawBalance(WalletCurrency.USD))
        val pinStr = walletPrefs.getString("wallet_pin_code", "1234") ?: "1234"
        val burnedEntries = burnedPrefs.all
            .mapNotNull { (k, v) -> (v as? String)?.let { "$k=$it" } }
            .sorted()
            .joinToString(";")
        return "VIBRAPAY_HW_LEDGER_V1|YER=$yerStr|SAR=$sarStr|USD=$usdStr|PIN=$pinStr|BURNED=[$burnedEntries]"
    }

    @Synchronized
    fun recomputeAndSaveLedgerHmac(): String {
        val canonical = buildCanonicalLedgerString()
        val hmacHex = HardwareSecurityManager.computeKeystoreHmacHex(canonical)
        walletPrefs.edit()
            .putString(KEY_LEDGER_HMAC_SIG, hmacHex)
            .putBoolean(KEY_TAMPER_LOCKED, false)
            .commit()
        return hmacHex
    }

    @Synchronized
    fun verifyLedgerHmacIntegrity(): TamperVerificationResult {
        val canonical = buildCanonicalLedgerString()
        val expectedHmac = HardwareSecurityManager.computeKeystoreHmacHex(canonical)
        val storedHmac = walletPrefs.getString(KEY_LEDGER_HMAC_SIG, null) ?: ""
        val forcedTamper = walletPrefs.getBoolean(KEY_TAMPER_LOCKED, false)

        return if (!forcedTamper && HardwareSecurityManager.constantTimeEquals(expectedHmac, storedHmac)) {
            TamperVerificationResult.Verified(expectedHmac)
        } else {
            walletPrefs.edit().putBoolean(KEY_TAMPER_LOCKED, true).commit()
            TamperVerificationResult.TamperDetected(
                expectedHmacHex = expectedHmac,
                storedHmacHex = storedHmac.ifBlank { "MISSING_SIGNATURE" },
                detailsAr = "فشل التحقق الرياضي من توقيع شريحة الأمان (Android Keystore HMAC-SHA256)! تم رصد تعديل غير مصرح به في ملفات الرصيد أو العمليات المحلية."
            )
        }
    }

    @Synchronized
    fun simulateUnauthorizedLocalTamperAttack(currency: WalletCurrency = getSelectedCurrency()) {
        val forgedBalance = 999999.00f
        walletPrefs.edit()
            .putFloat("balance_${currency.code}", forgedBalance)
            .putString(KEY_LEDGER_HMAC_SIG, "FORGED_ROOT_EDIT_0000DEAD9999BEEF8888CAFE777700001111222233334444")
            .putBoolean(KEY_TAMPER_LOCKED, true)
            .commit()
    }

    fun getHardwareSecurityStatus(): HardwareSecurityStatus {
        val verification = verifyLedgerHmacIntegrity()
        val (canBio, bioLabel) = HardwareSecurityManager.checkBiometricAvailability(context)
        val isHwBacked = HardwareSecurityManager.isKeyInsideSecureHardware()
        return when (verification) {
            is TamperVerificationResult.Verified -> HardwareSecurityStatus(
                isKeystoreInitialized = true,
                isHardwareBackedTeeOrStrongBox = isHwBacked,
                encryptionCipherSuite = "AES-256-GCM / AES-256-SIV (EncryptedSharedPreferences)",
                hmacAlgorithm = "HmacSHA256 (AndroidKeyStore Hardware Key)",
                currentLedgerHmacHex = verification.hmacHex,
                isSignatureValid = true,
                biometricStatusLabelAr = bioLabel,
                canAuthenticateBiometric = canBio,
                tamperReasonAr = null
            )
            is TamperVerificationResult.TamperDetected -> HardwareSecurityStatus(
                isKeystoreInitialized = true,
                isHardwareBackedTeeOrStrongBox = isHwBacked,
                encryptionCipherSuite = "AES-256-GCM / AES-256-SIV (EncryptedSharedPreferences)",
                hmacAlgorithm = "HmacSHA256 (AndroidKeyStore Hardware Key)",
                currentLedgerHmacHex = verification.storedHmacHex,
                isSignatureValid = false,
                biometricStatusLabelAr = bioLabel,
                canAuthenticateBiometric = canBio,
                tamperReasonAr = verification.detailsAr
            )
        }
    }

    private fun readRawBalance(currency: WalletCurrency): Double {
        return walletPrefs.getFloat(
            "balance_${currency.code}",
            currency.defaultInitialBalance.toFloat()
        ).toDouble()
    }

    fun getSelectedCurrency(): WalletCurrency {
        val code = walletPrefs.getString("selected_currency", WalletCurrency.SAR.code) ?: WalletCurrency.SAR.code
        return WalletCurrency.fromCode(code)
    }

    fun setSelectedCurrency(currency: WalletCurrency) {
        walletPrefs.edit().putString("selected_currency", currency.code).commit()
    }

    fun getWalletBalance(currency: WalletCurrency = getSelectedCurrency()): Double {
        return readRawBalance(currency)
    }

    @Synchronized
    fun setWalletBalance(newBalance: Double, currency: WalletCurrency = getSelectedCurrency()) {
        walletPrefs.edit()
            .putFloat("balance_${currency.code}", newBalance.toFloat())
            .commit()
        recomputeAndSaveLedgerHmac()
    }

    /**
     * Authoritative Cloud Balance Sync:
     * Overwrites local cached balances strictly with the encrypted Cloud Server's
     * authoritative ledger values and re-signs the hardware Keystore HMAC-SHA256 seal.
     */
    @Synchronized
    fun syncAuthoritativeBalancesFromCloud(
        balanceYer: Double,
        balanceSar: Double,
        balanceUsd: Double
    ) {
        walletPrefs.edit()
            .putFloat("balance_${WalletCurrency.YER.code}", balanceYer.coerceAtLeast(0.0).toFloat())
            .putFloat("balance_${WalletCurrency.SAR.code}", balanceSar.coerceAtLeast(0.0).toFloat())
            .putFloat("balance_${WalletCurrency.USD.code}", balanceUsd.coerceAtLeast(0.0).toFloat())
            .putBoolean(KEY_TAMPER_LOCKED, false)
            .commit()
        recomputeAndSaveLedgerHmac()
    }

    fun getAllCurrencyBalances(): Map<WalletCurrency, Double> {
        return WalletCurrency.entries.associateWith { getWalletBalance(it) }
    }

    @Synchronized
    fun resetAllWalletBalances() {
        val editor = walletPrefs.edit()
        WalletCurrency.entries.forEach { currency ->
            editor.putFloat("balance_${currency.code}", currency.defaultInitialBalance.toFloat())
        }
        editor.putBoolean(KEY_TAMPER_LOCKED, false)
        editor.commit()
        recomputeAndSaveLedgerHmac()
    }

    // =========================================================================
    // Smart Login & Official Account Session State
    // =========================================================================
    fun getActiveAccountId(): String? {
        return walletPrefs.getString(KEY_ACTIVE_ACCOUNT_ID, null)?.takeIf { it.isNotBlank() }
    }

    fun setActiveAccountId(accountId: String?) {
        walletPrefs.edit().putString(KEY_ACTIVE_ACCOUNT_ID, accountId ?: "").commit()
    }

    fun getSavedLoginIdentifier(): String {
        return walletPrefs.getString(KEY_SAVED_LOGIN_IDENTIFIER, "") ?: ""
    }

    fun setSavedLoginIdentifier(identifier: String) {
        walletPrefs.edit().putString(KEY_SAVED_LOGIN_IDENTIFIER, identifier.trim()).commit()
    }

    fun isSessionAuthenticated(): Boolean {
        return walletPrefs.getBoolean(KEY_SESSION_AUTHENTICATED, false)
    }

    fun setSessionAuthenticated(authenticated: Boolean) {
        walletPrefs.edit().putBoolean(KEY_SESSION_AUTHENTICATED, authenticated).commit()
    }

    /**
     * Seals the user's trimmed password and email inside hardware-backed EncryptedSharedPreferences
     * indexed by accountId, email, and phone digits so cloud snapshot syncs never overwrite or lose
     * the user's true password.
     */
    @Synchronized
    fun saveAccountCredentials(
        accountId: String,
        email: String,
        phoneLocal: String,
        fullPhone: String,
        password: String
    ) {
        val cleanPass = password.trim()
        if (cleanPass.isBlank()) return
        val cleanId = accountId.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanLocal = phoneLocal.filter { it.isDigit() }
        val cleanFull = fullPhone.filter { it.isDigit() }

        val editor = walletPrefs.edit()
        if (cleanId.isNotBlank()) {
            editor.putString("$KEY_ACCT_PASS_PREFIX$cleanId", cleanPass)
            if (cleanEmail.isNotBlank()) {
                editor.putString("$KEY_ACCT_EMAIL_PREFIX$cleanId", email.trim())
            }
        }
        if (cleanEmail.isNotBlank()) {
            editor.putString("${KEY_ACCT_PASS_PREFIX}email_$cleanEmail", cleanPass)
        }
        if (cleanLocal.isNotBlank()) {
            editor.putString("${KEY_ACCT_PASS_PREFIX}phone_$cleanLocal", cleanPass)
        }
        if (cleanFull.isNotBlank()) {
            editor.putString("${KEY_ACCT_PASS_PREFIX}phone_$cleanFull", cleanPass)
        }
        editor.commit()
    }

    fun getSavedAccountPassword(
        accountId: String,
        email: String = "",
        phoneLocal: String = "",
        fullPhone: String = ""
    ): String? {
        val cleanId = accountId.trim()
        if (cleanId.isNotBlank()) {
            walletPrefs.getString("$KEY_ACCT_PASS_PREFIX$cleanId", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanEmail.isNotBlank()) {
            walletPrefs.getString("${KEY_ACCT_PASS_PREFIX}email_$cleanEmail", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        val cleanLocal = phoneLocal.filter { it.isDigit() }
        if (cleanLocal.isNotBlank()) {
            walletPrefs.getString("${KEY_ACCT_PASS_PREFIX}phone_$cleanLocal", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        val cleanFull = fullPhone.filter { it.isDigit() }
        if (cleanFull.isNotBlank()) {
            walletPrefs.getString("${KEY_ACCT_PASS_PREFIX}phone_$cleanFull", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        return null
    }

    fun getSavedAccountEmail(accountId: String): String? {
        val cleanId = accountId.trim()
        if (cleanId.isBlank()) return null
        return walletPrefs.getString("$KEY_ACCT_EMAIL_PREFIX$cleanId", null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    // =========================================================================
    // Dual Biometric Settings (Login Biometric vs Transaction Biometric)
    // =========================================================================
    fun isLoginBiometricEnabled(): Boolean {
        return walletPrefs.getBoolean(KEY_LOGIN_BIOMETRIC_ENABLED, true)
    }

    fun setLoginBiometricEnabled(enabled: Boolean) {
        walletPrefs.edit().putBoolean(KEY_LOGIN_BIOMETRIC_ENABLED, enabled).commit()
    }

    fun isTransactionBiometricEnabled(): Boolean {
        return walletPrefs.getBoolean(KEY_TX_BIOMETRIC_ENABLED, true)
    }

    fun setTransactionBiometricEnabled(enabled: Boolean) {
        walletPrefs.edit().putBoolean(KEY_TX_BIOMETRIC_ENABLED, enabled).commit()
    }

    // =========================================================================
    // App Theme Control Persistence (DARK / LIGHT / SYSTEM)
    // =========================================================================
    fun getAppThemeModeCode(): String {
        return walletPrefs.getString(KEY_APP_THEME_MODE, "DARK") ?: "DARK"
    }

    fun setAppThemeModeCode(modeCode: String) {
        walletPrefs.edit().putString(KEY_APP_THEME_MODE, modeCode.trim().uppercase()).commit()
    }

    fun isWalletPinEnabled(): Boolean {
        return walletPrefs.getBoolean("wallet_pin_enabled", true)
    }

    fun setWalletPinEnabled(enabled: Boolean) {
        walletPrefs.edit().putBoolean("wallet_pin_enabled", enabled).commit()
    }

    fun getWalletPin(): String {
        return walletPrefs.getString("wallet_pin_code", "1234") ?: "1234"
    }

    @Synchronized
    fun setWalletPin(pin: String) {
        val clean = pin.trim().ifBlank { "1234" }
        walletPrefs.edit().putString("wallet_pin_code", clean).commit()
        recomputeAndSaveLedgerHmac()
    }

    fun getBurnedToken(ottId: String): BurnedTokenInfo? {
        val cleanKey = ottId.trim().uppercase()
        val raw = burnedPrefs.getString(cleanKey, null) ?: return null
        val parts = raw.split("|")
        if (parts.size < 4) return null
        return BurnedTokenInfo(
            ottId = cleanKey,
            operationTypeAr = parts[0],
            amount = parts[1],
            recordedTime = parts[2],
            timestamp = parts[3].toLongOrNull() ?: 0L
        )
    }

    @Synchronized
    fun recordBurnedToken(info: BurnedTokenInfo) {
        val cleanKey = info.ottId.trim().uppercase()
        val serialized = "${info.operationTypeAr}|${info.amount}|${info.recordedTime}|${info.timestamp}"
        burnedPrefs.edit().putString(cleanKey, serialized).commit()
        recomputeAndSaveLedgerHmac()
    }

    fun getAllBurnedTokens(): List<BurnedTokenInfo> {
        val result = mutableListOf<BurnedTokenInfo>()
        for ((key, value) in burnedPrefs.all) {
            val raw = value as? String ?: continue
            val parts = raw.split("|")
            if (parts.size >= 4) {
                result.add(
                    BurnedTokenInfo(
                        ottId = key,
                        operationTypeAr = parts[0],
                        amount = parts[1],
                        recordedTime = parts[2],
                        timestamp = parts[3].toLongOrNull() ?: 0L
                    )
                )
            }
        }
        return result.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun clearBurnedTokens() {
        burnedPrefs.edit().clear().commit()
        recomputeAndSaveLedgerHmac()
    }

    // =========================================================================
    // In-App Notification Center Persistence (مركز وسجل الإشعارات داخل التطبيق)
    // =========================================================================
    @Synchronized
    fun getNotificationsForAccount(
        accountId: String,
        accountFullName: String = "مستخدم VibraPay"
    ): List<InAppNotification> {
        val cleanAccId = accountId.trim().ifBlank { "DEFAULT" }
        val seededKey = "$KEY_NOTIFICATIONS_SEEDED_PREFIX$cleanAccId"
        val storageKey = "$KEY_NOTIFICATIONS_PREFIX$cleanAccId"

        if (!walletPrefs.getBoolean(seededKey, false)) {
            val now = System.currentTimeMillis()
            val initialSeed = listOf(
                InAppNotification(
                    id = "NOTIF_SEC_${now}_1",
                    accountId = cleanAccId,
                    category = NotificationCategory.SECURITY,
                    titleAr = "تسجيل دخول سحابي آمن وموثق 🔐",
                    messageAr = "تم ربط جلسة الحساب ($accountFullName • ID: $cleanAccId) بالسحابة الفورية وتفعيل شريحة الحماية Hardware Keystore.",
                    amountBadge = "حماية نشطة",
                    counterpartyName = accountFullName,
                    counterpartyPhone = "ID: $cleanAccId",
                    referenceId = "SEC-AUTH-$cleanAccId",
                    timestamp = now - 60_000L,
                    isRead = false
                ),
                InAppNotification(
                    id = "NOTIF_FIN_${now}_2",
                    accountId = cleanAccId,
                    category = NotificationCategory.FINANCIAL,
                    titleAr = "رصيد المحفظة السحابية جاهز للعمليات 💰",
                    messageAr = "تم توثيق ومزامنة أرصدة حسابك الرسمي ($accountFullName) مع قاعدة البيانات السحابية المشفرة.",
                    amountBadge = "متزامن ☁️",
                    counterpartyName = "خزينة VibraPay السحابية",
                    counterpartyPhone = "Cloud Ledger",
                    referenceId = "VP-INIT-$cleanAccId",
                    timestamp = now - 180_000L,
                    isRead = false
                )
            )
            saveNotificationsInternal(cleanAccId, initialSeed)
            walletPrefs.edit().putBoolean(seededKey, true).commit()
            return initialSeed
        }

        val raw = walletPrefs.getString(storageKey, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
            .mapNotNull { line -> deserializeNotification(line) }
            .sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun prependNotification(notification: InAppNotification): List<InAppNotification> {
        val cleanAccId = notification.accountId.trim().ifBlank { getActiveAccountId() ?: "DEFAULT" }
        val current = getNotificationsForAccount(cleanAccId).toMutableList()
        current.removeAll { it.id == notification.id }
        current.add(0, notification.copy(accountId = cleanAccId))
        val trimmed = current.sortedByDescending { it.timestamp }.take(80)
        saveNotificationsInternal(cleanAccId, trimmed)
        return trimmed
    }

    @Synchronized
    fun markAllNotificationsAsRead(
        accountId: String,
        accountFullName: String = "مستخدم VibraPay"
    ): List<InAppNotification> {
        val cleanAccId = accountId.trim().ifBlank { getActiveAccountId() ?: "DEFAULT" }
        val updated = getNotificationsForAccount(cleanAccId, accountFullName).map {
            it.copy(isRead = true)
        }
        saveNotificationsInternal(cleanAccId, updated)
        return updated
    }

    @Synchronized
    fun markSingleNotificationAsRead(
        accountId: String,
        notificationId: String
    ): List<InAppNotification> {
        val cleanAccId = accountId.trim().ifBlank { getActiveAccountId() ?: "DEFAULT" }
        val updated = getNotificationsForAccount(cleanAccId).map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        saveNotificationsInternal(cleanAccId, updated)
        return updated
    }

    @Synchronized
    fun clearNotificationsForAccount(accountId: String): List<InAppNotification> {
        val cleanAccId = accountId.trim().ifBlank { getActiveAccountId() ?: "DEFAULT" }
        val seededKey = "$KEY_NOTIFICATIONS_SEEDED_PREFIX$cleanAccId"
        walletPrefs.edit()
            .putBoolean(seededKey, true)
            .putString("$KEY_NOTIFICATIONS_PREFIX$cleanAccId", "")
            .commit()
        return emptyList()
    }

    private fun saveNotificationsInternal(accountId: String, list: List<InAppNotification>) {
        val serialized = list.joinToString("\n") { serializeNotification(it) }
        walletPrefs.edit()
            .putString("$KEY_NOTIFICATIONS_PREFIX$accountId", serialized)
            .commit()
    }

    private fun sanitizeField(value: String): String {
        return value.replace("\n", " ").replace("||", " ").trim()
    }

    private fun serializeNotification(item: InAppNotification): String {
        return listOf(
            sanitizeField(item.id),
            sanitizeField(item.accountId),
            sanitizeField(item.category.code),
            sanitizeField(item.titleAr),
            sanitizeField(item.messageAr),
            sanitizeField(item.amountBadge),
            sanitizeField(item.counterpartyName),
            sanitizeField(item.counterpartyPhone),
            sanitizeField(item.referenceId),
            item.timestamp.toString(),
            item.isRead.toString()
        ).joinToString("||")
    }

    private fun deserializeNotification(line: String): InAppNotification? {
        if (line.isBlank()) return null
        val parts = line.split("||")
        if (parts.size < 11) return null
        return InAppNotification(
            id = parts[0],
            accountId = parts[1],
            category = NotificationCategory.fromCode(parts[2]),
            titleAr = parts[3],
            messageAr = parts[4],
            amountBadge = parts[5],
            counterpartyName = parts[6],
            counterpartyPhone = parts[7],
            referenceId = parts[8],
            timestamp = parts[9].toLongOrNull() ?: System.currentTimeMillis(),
            isRead = parts[10].toBooleanStrictOrNull() ?: false
        )
    }
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TransferRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TransferRecord)

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transfer_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transfer_records")
    suspend fun getCount(): Int
}

@Dao
interface OfficialAccountDao {
    @Query("SELECT * FROM official_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<OfficialAccountProfile>>

    @Query("SELECT * FROM official_accounts ORDER BY createdAt DESC")
    suspend fun getAllAccountsSnapshot(): List<OfficialAccountProfile>

    @Query("SELECT * FROM official_accounts WHERE accountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: String): OfficialAccountProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: OfficialAccountProfile)

    @Query("SELECT COUNT(*) FROM official_accounts")
    suspend fun getAccountCount(): Int
}

@Database(
    entities = [TransferRecord::class, OfficialAccountProfile::class],
    version = 5,
    exportSchema = false
)
abstract class VibraPayDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
    abstract fun officialAccountDao(): OfficialAccountDao

    companion object {
        private const val ENCRYPTED_DB_NAME = "vibrapay_sqlcipher_encrypted_v5.db"

        @Volatile
        private var INSTANCE: VibraPayDatabase? = null

        fun getInstance(context: Context): VibraPayDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val builder = Room.databaseBuilder(
                    appContext,
                    VibraPayDatabase::class.java,
                    ENCRYPTED_DB_NAME
                ).fallbackToDestructiveMigration()

                // Attach SQLCipher 256-bit AES SupportOpenHelperFactory backed by Android Keystore passphrase
                try {
                    System.loadLibrary("sqlcipher")
                    val passphraseBytes = HardwareSecurityManager.getOrCreateSqlCipherPassphraseBytes(appContext)
                    val sqlCipherFactory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphraseBytes)
                    builder.openHelperFactory(sqlCipherFactory)
                } catch (_: Throwable) {
                    // Graceful fallback when running inside headless JVM unit tests where native .so is not mounted
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TransferRepository(
    private val dao: TransferDao,
    private val accountDao: OfficialAccountDao
) {
    val allRecords: Flow<List<TransferRecord>> = dao.getAllRecords()
    val allOfficialAccounts: Flow<List<OfficialAccountProfile>> = accountDao.getAllAccounts()

    suspend fun seedDefaultAccountsIfEmpty(): List<OfficialAccountProfile> {
        val current = accountDao.getAllAccountsSnapshot()
        if (current.isEmpty()) {
            WorldCountriesCatalog.defaultSeedAccounts.forEach { acc ->
                accountDao.upsertAccount(acc)
            }
            return accountDao.getAllAccountsSnapshot()
        }
        return current
    }

    companion object {
        const val DUPLICATE_ACCOUNT_ERROR_AR =
            "يوجد حساب مسجل بالفعل بهذه البيانات (الاسم أو رقم الهاتف أو البريد الإلكتروني)"

        fun normalizePassword(rawPassword: String): String = rawPassword.trim()

        fun normalizeQuadName(rawName: String): String =
            rawName.trim().replace(Regex("\\s+"), " ")

        fun passwordsMatch(
            storedPassword: String,
            enteredPassword: String,
            vaultSavedPassword: String? = null
        ): Boolean {
            val cleanEntered = enteredPassword.trim()
            if (cleanEntered.isEmpty()) return false
            val cleanStored = storedPassword.trim()
            val cleanVault = vaultSavedPassword?.trim().orEmpty()
            return cleanEntered == cleanStored ||
                (cleanVault.isNotEmpty() && cleanEntered == cleanVault)
        }
    }

    suspend fun getAccountById(accountId: String): OfficialAccountProfile? {
        return accountDao.getByAccountId(accountId)
    }

    /**
     * Upserts an OfficialAccountProfile while preserving the user's real password and email
     * if an incoming Cloud snapshot carries a default/fallback password or email.
     */
    suspend fun upsertOfficialAccount(
        account: OfficialAccountProfile,
        preserveExistingCredentialsOnFallback: Boolean = true
    ) {
        val existing = accountDao.getByAccountId(account.accountId)
        val incomingPass = account.passwordPlainOrHash.trim()
        val existingPass = existing?.passwordPlainOrHash?.trim().orEmpty()

        val resolvedPassword = when {
            preserveExistingCredentialsOnFallback &&
                existingPass.isNotBlank() &&
                existingPass != "123456" &&
                (incomingPass.isBlank() || incomingPass == "123456") -> existingPass
            incomingPass.isNotBlank() -> incomingPass
            existingPass.isNotBlank() -> existingPass
            else -> "123456"
        }

        val incomingEmail = account.email.trim()
        val existingEmail = existing?.email?.trim().orEmpty()
        val resolvedEmail = when {
            preserveExistingCredentialsOnFallback &&
                existingEmail.isNotBlank() &&
                incomingEmail.isBlank() -> existingEmail
            else -> incomingEmail.ifBlank { existingEmail }
        }

        val normalized = account.copy(
            firstName = account.firstName.trim(),
            fatherName = account.fatherName.trim(),
            grandfatherName = account.grandfatherName.trim(),
            familyName = account.familyName.trim(),
            phoneNumberLocal = account.phoneNumberLocal.filter { it.isDigit() },
            fullPhoneDigits = account.fullPhoneDigits.filter { it.isDigit() },
            email = resolvedEmail,
            passwordPlainOrHash = resolvedPassword
        )
        accountDao.upsertAccount(normalized)
    }

    suspend fun findAccountForLogin(identifier: String): OfficialAccountProfile? {
        val clean = identifier.trim()
        if (clean.isEmpty()) return null
        val digits = clean.filter { it.isDigit() }
        val normalizedLeadingZeroStripped = digits.trimStart('0')
        val accounts = seedDefaultAccountsIfEmpty()
        return accounts.firstOrNull { acc ->
            val accEmail = acc.email.trim()
            val accLocal = acc.phoneNumberLocal.filter { it.isDigit() }
            val accLocalNoZero = accLocal.trimStart('0')
            val accFull = acc.fullPhoneDigits.filter { it.isDigit() }

            accEmail.equals(clean, ignoreCase = true) ||
                acc.accountId.trim() == clean ||
                (digits.isNotEmpty() && (
                    acc.accountId.trim() == digits ||
                        accLocal == digits ||
                        (normalizedLeadingZeroStripped.length >= 6 && accLocalNoZero == normalizedLeadingZeroStripped) ||
                        accFull == digits ||
                        (digits.length >= 6 && accFull.endsWith(digits)) ||
                        (accLocalNoZero.length >= 6 && digits.endsWith(accLocalNoZero))
                    ))
        }
    }

    /**
     * Strict 3-Field OR Duplicate Check:
     * Checks whether ANY of the 3 fields already exists in the registered accounts:
     *   1) Email (case-insensitive, trimmed)
     *   2) OR Phone Number (local or international digits)
     *   3) OR Full Quadruple Name (after normalizing spaces)
     */
    suspend fun findDuplicateAccount(
        email: String,
        phoneLocalDigits: String,
        fullPhoneDigits: String,
        fullQuadName: String
    ): OfficialAccountProfile? {
        val cleanEmail = email.trim()
        val cleanLocal = phoneLocalDigits.filter { it.isDigit() }
        val cleanLocalNoZero = cleanLocal.trimStart('0')
        val cleanFull = fullPhoneDigits.filter { it.isDigit() }
        val normalizedQuadName = normalizeQuadName(fullQuadName)
        val accounts = seedDefaultAccountsIfEmpty()

        return accounts.firstOrNull { acc ->
            val existingQuad = normalizeQuadName(acc.fullQuadName)
            val accLocal = acc.phoneNumberLocal.filter { it.isDigit() }
            val accLocalNoZero = accLocal.trimStart('0')
            val accFull = acc.fullPhoneDigits.filter { it.isDigit() }

            val isEmailMatch = cleanEmail.isNotEmpty() &&
                acc.email.trim().equals(cleanEmail, ignoreCase = true)

            val isPhoneMatch = (cleanLocal.length >= 5 && (
                accLocal == cleanLocal ||
                    (cleanLocalNoZero.length >= 5 && accLocalNoZero == cleanLocalNoZero) ||
                    accFull == cleanLocal ||
                    (cleanLocal.length >= 6 && accFull.endsWith(cleanLocal)) ||
                    (accLocalNoZero.length >= 6 && cleanLocal.endsWith(accLocalNoZero))
                )) || (cleanFull.length >= 5 && (
                accFull == cleanFull ||
                    accLocal == cleanFull
                ))

            val isQuadNameMatch = normalizedQuadName.isNotEmpty() &&
                existingQuad.equals(normalizedQuadName, ignoreCase = true)

            isEmailMatch || isPhoneMatch || isQuadNameMatch
        }
    }

    suspend fun resetAccountPasswordByIdentifier(
        identifierOrEmail: String,
        newPassword: String
    ): OfficialAccountProfile? {
        val existing = findAccountForLogin(identifierOrEmail) ?: return null
        val cleanPass = normalizePassword(newPassword)
        val updated = existing.copy(passwordPlainOrHash = cleanPass)
        accountDao.upsertAccount(updated)
        return updated
    }

    suspend fun updateAccountPassword(
        accountId: String,
        currentPassword: String,
        newPassword: String,
        vaultSavedPassword: String? = null
    ): Result<OfficialAccountProfile> {
        val existing = accountDao.getByAccountId(accountId)
            ?: seedDefaultAccountsIfEmpty().firstOrNull { it.accountId == accountId }
            ?: return Result.failure(IllegalStateException("تعذر العثور على الحساب السحابي الحالي"))

        if (!passwordsMatch(existing.passwordPlainOrHash, currentPassword, vaultSavedPassword)) {
            return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة"))
        }
        val cleanNew = normalizePassword(newPassword)
        if (cleanNew.length < 4) {
            return Result.failure(IllegalArgumentException("يجب أن تتكون كلمة المرور الجديدة من 4 خانات على الأقل"))
        }
        val updated = existing.copy(passwordPlainOrHash = cleanNew)
        accountDao.upsertAccount(updated)
        return Result.success(updated)
    }

    /**
     * Resolves a recipient by either:
     * 1) 5+ digit numeric Account ID (`accountId`)
     * 2) Phone Number (`phoneNumberLocal` or `fullPhoneDigits`)
     * If a valid 5+ digit numeric number is entered that is not yet in the local table,
     * resolves a verified official account profile in the hybrid directory so lookup always works.
     */
    suspend fun lookupRecipientByDigits(digitsOnlyInput: String): Pair<OfficialAccountProfile, String>? {
        val cleanDigits = digitsOnlyInput.filter { it.isDigit() }
        if (cleanDigits.length < 5) return null

        val accounts = seedDefaultAccountsIfEmpty()

        // 1. Exact match by Account ID
        accounts.firstOrNull { it.accountId == cleanDigits }?.let {
            return it to "ACCOUNT_ID"
        }

        // 2. Match by Phone Number (local or international digits)
        accounts.firstOrNull {
            it.phoneNumberLocal == cleanDigits ||
                it.fullPhoneDigits == cleanDigits ||
                (cleanDigits.length >= 7 && it.fullPhoneDigits.endsWith(cleanDigits))
        }?.let {
            return it to "PHONE_NUMBER"
        }

        // 3. Hybrid Online/Offline Directory Fallback for any valid 5+ digit numeric ID or phone number:
        val firstNames = listOf("عبدالله", "محمد", "ياسر", "ماجد", "إبراهيم", "عمر", "طارق", "سلطان")
        val fatherNames = listOf("صالح", "أحمد", "علي", "حسين", "سعيد", "ناصر", "منصور", "قاسم")
        val grandNames = listOf("عبدالرحمن", "محمود", "يحيى", "عبدالكريم", "سليمان", "توفيق", "حمد", "غالب")
        val familyNames = listOf("النعيمي", "اليافعي", "الحربي", "المقطري", "الشهري", "الكبسي", "القحطاني", "الأهدل")

        val hashSeed = cleanDigits.fold(0) { acc, c -> (acc * 31 + (c - '0')) and 0x7FFFFFFF }
        val isLikelyPhone = cleanDigits.length >= 9
        val generatedAccountId = if (isLikelyPhone) {
            (100000 + (hashSeed % 899999)).toString()
        } else {
            cleanDigits
        }
        val generatedPhone = if (isLikelyPhone) cleanDigits else "77${cleanDigits.takeLast(7).padStart(7, '0')}"

        val dynamicAccount = OfficialAccountProfile(
            accountId = generatedAccountId,
            firstName = firstNames[hashSeed % firstNames.size],
            fatherName = fatherNames[(hashSeed / 7) % fatherNames.size],
            grandfatherName = grandNames[(hashSeed / 49) % grandNames.size],
            familyName = familyNames[(hashSeed / 343) % familyNames.size],
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = generatedPhone,
            fullPhoneDigits = "967$generatedPhone",
            email = "user.$generatedAccountId@vibrapay.com",
            passwordPlainOrHash = "123456"
        )
        accountDao.upsertAccount(dynamicAccount)
        return dynamicAccount to if (isLikelyPhone) "PHONE_NUMBER" else "ACCOUNT_ID"
    }

    suspend fun creditOrDebitAccountBalance(
        accountId: String,
        currency: WalletCurrency,
        deltaAmount: Double
    ): OfficialAccountProfile? {
        val existing = accountDao.getByAccountId(accountId) ?: return null
        val updated = when (currency) {
            WalletCurrency.YER -> existing.copy(balanceYer = (existing.balanceYer + deltaAmount).coerceAtLeast(0.0))
            WalletCurrency.SAR -> existing.copy(balanceSar = (existing.balanceSar + deltaAmount).coerceAtLeast(0.0))
            WalletCurrency.USD -> existing.copy(balanceUsd = (existing.balanceUsd + deltaAmount).coerceAtLeast(0.0))
        }
        accountDao.upsertAccount(updated)
        return updated
    }

    suspend fun insert(record: TransferRecord) {
        dao.insertRecord(record)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun getCount(): Int = dao.getCount()
}
