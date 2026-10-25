package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ReceiverStatus
import com.example.audio.SonicTransceiverEngine
import com.example.audio.SpectrumFrame
import com.example.cloud.CloudNotificationHelper
import com.example.cloud.GoogleAuthManager
import com.example.cloud.NetworkConnectivityObserver
import com.example.cloud.RealEmailOtpService
import com.example.cloud.VibraPayCloudRepository
import com.example.data.BurnedTokenInfo
import com.example.data.InAppNotification
import com.example.data.NotificationCategory
import com.example.data.OfficialAccountProfile
import com.example.data.PendingTransferVerification
import com.example.data.TransferRecord
import com.example.data.TransferRepository
import com.example.data.VibraPayDatabase
import com.example.data.VibraPayLocalStorage
import com.example.data.WalletCurrency
import com.example.data.WorldCountriesCatalog
import com.example.data.WorldCountry
import com.example.protocol.AcousticMode
import com.example.protocol.DecodedPacketResult
import com.example.protocol.EncodedPacket
import com.example.protocol.MfskProtocol
import com.example.protocol.MicroOpType
import com.example.protocol.ScheduledTone
import com.example.protocol.StructuredPayload
import com.example.protocol.XorStreamCipher
import com.example.qrcode.QrScannedRecipient
import com.example.security.HardwareSecurityManager
import com.example.security.HardwareSecurityStatus
import com.example.security.TamperVerificationResult
import com.example.ui.theme.AppThemeMode
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class TemplateTab(val titleAr: String) {
    QUICK_INVOICE("دفعة مالية / فاتورة"),
    FREE_TEXT("رسالة نصية"),
    CONTACT_CARD("بطاقة تواصل")
}

enum class TransceiverWorkspaceMode(val titleAr: String) {
    SEND("تحويل / طلب دفعة"),
    RECEIVE("استلام بالتلامس")
}

enum class MainNavTab(val titleAr: String) {
    RADAR_TRANSCEIVER("الرئيسية"),
    WALLET("المحفظة"),
    HISTORY("العمليات"),
    SETTINGS("الإعدادات")
}

data class PendingPinProtectedAction(
    val titleAr: String,
    val execute: () -> Unit
)

class VibraPayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransferRepository
    private val localStorage = VibraPayLocalStorage(application)
    private val connectivityObserver = NetworkConnectivityObserver(application)
    private val cloudRepository = VibraPayCloudRepository(application)

    val historyRecords: StateFlow<List<TransferRecord>>
    val directoryAccounts: StateFlow<List<OfficialAccountProfile>>

    // Real-Time Network Connectivity & Cloud Sync States
    val isNetworkOnline: StateFlow<Boolean> = connectivityObserver.isOnline
    val networkStatusLabelAr: StateFlow<String> = connectivityObserver.networkLabelAr

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(GoogleAuthManager.currentUser())
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _cloudSyncStatusAr = MutableStateFlow("جاهز للمزامنة السحابية")
    val cloudSyncStatusAr: StateFlow<String> = _cloudSyncStatusAr.asStateFlow()

    private val _isCloudLookupInProgress = MutableStateFlow(false)
    val isCloudLookupInProgress: StateFlow<Boolean> = _isCloudLookupInProgress.asStateFlow()

    private val _pendingOfflineSyncCount = MutableStateFlow(0)
    val pendingOfflineSyncCount: StateFlow<Int> = _pendingOfflineSyncCount.asStateFlow()

    private var cloudAccountsListenerJob: Job? = null
    private var cloudTransfersListenerJob: Job? = null
    private val processedCloudTransferIds = mutableSetOf<String>()

    // Hardware Keystore & HMAC-SHA256 Security Status
    private val _hardwareSecurityStatus = MutableStateFlow(localStorage.getHardwareSecurityStatus())
    val hardwareSecurityStatus: StateFlow<HardwareSecurityStatus> = _hardwareSecurityStatus.asStateFlow()

    // Official Account, Registration & Smart Login State
    private val _activeAccount = MutableStateFlow<OfficialAccountProfile?>(
        WorldCountriesCatalog.defaultSeedAccounts.first()
    )
    val activeAccount: StateFlow<OfficialAccountProfile?> = _activeAccount.asStateFlow()

    private val _savedLoginIdentifier = MutableStateFlow(
        localStorage.getSavedLoginIdentifier().ifBlank {
            WorldCountriesCatalog.defaultSeedAccounts.first().phoneNumberLocal
        }
    )
    val savedLoginIdentifier: StateFlow<String> = _savedLoginIdentifier.asStateFlow()

    private val _showAuthModal = MutableStateFlow(!localStorage.isSessionAuthenticated())
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    private val _authModalInitialRegister = MutableStateFlow(false)
    val authModalInitialRegister: StateFlow<Boolean> = _authModalInitialRegister.asStateFlow()

    private val _isSessionLockedForSmartLogin = MutableStateFlow(!localStorage.isSessionAuthenticated())
    val isSessionLockedForSmartLogin: StateFlow<Boolean> = _isSessionLockedForSmartLogin.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _newlyRegisteredAccount = MutableStateFlow<OfficialAccountProfile?>(null)
    val newlyRegisteredAccount: StateFlow<OfficialAccountProfile?> = _newlyRegisteredAccount.asStateFlow()

    // Dual Biometric Settings (Independent Login Biometric vs Transaction Biometric)
    private val _isLoginBiometricEnabled = MutableStateFlow(localStorage.isLoginBiometricEnabled())
    val isLoginBiometricEnabled: StateFlow<Boolean> = _isLoginBiometricEnabled.asStateFlow()

    private val _isTransactionBiometricEnabled = MutableStateFlow(localStorage.isTransactionBiometricEnabled())
    val isTransactionBiometricEnabled: StateFlow<Boolean> = _isTransactionBiometricEnabled.asStateFlow()

    // App Theme Mode State (DARK / LIGHT / SYSTEM) — Persisted locally
    private val _appThemeMode = MutableStateFlow(AppThemeMode.fromCode(localStorage.getAppThemeModeCode()))
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _showThemeSelectorDialog = MutableStateFlow(false)
    val showThemeSelectorDialog: StateFlow<Boolean> = _showThemeSelectorDialog.asStateFlow()

    private val _showChangePasswordDialog = MutableStateFlow(false)
    val showChangePasswordDialog: StateFlow<Boolean> = _showChangePasswordDialog.asStateFlow()

    // QR Code System States ("رمزي" Personal QR Dialog & "مسح رمز QR" Scanner Dialog)
    private val _showMyQrDialog = MutableStateFlow(false)
    val showMyQrDialog: StateFlow<Boolean> = _showMyQrDialog.asStateFlow()

    private val _showQrScannerDialog = MutableStateFlow(false)
    val showQrScannerDialog: StateFlow<Boolean> = _showQrScannerDialog.asStateFlow()

    // In-App Notification Center States (مركز وسجل الإشعارات داخل التطبيق)
    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    // Multi-Currency & Wallet State (YER, SAR, USD)
    private val _selectedCurrency = MutableStateFlow(localStorage.getSelectedCurrency())
    val selectedCurrency: StateFlow<WalletCurrency> = _selectedCurrency.asStateFlow()

    private val _walletBalance = MutableStateFlow(localStorage.getWalletBalance(_selectedCurrency.value))
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _allCurrencyBalances = MutableStateFlow(localStorage.getAllCurrencyBalances())
    val allCurrencyBalances: StateFlow<Map<WalletCurrency, Double>> = _allCurrencyBalances.asStateFlow()

    private val _lastTopUpBanner = MutableStateFlow<String?>(null)
    val lastTopUpBanner: StateFlow<String?> = _lastTopUpBanner.asStateFlow()

    // Mandatory Biometric / Hardware PIN Lock State (Enabled by default)
    private val _isWalletPinEnabled = MutableStateFlow(localStorage.isWalletPinEnabled())
    val isWalletPinEnabled: StateFlow<Boolean> = _isWalletPinEnabled.asStateFlow()

    private val _walletPin = MutableStateFlow(localStorage.getWalletPin())
    val walletPin: StateFlow<String> = _walletPin.asStateFlow()

    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    private val _pendingPinProtectedAction = MutableStateFlow<PendingPinProtectedAction?>(null)
    val pendingPinProtectedAction: StateFlow<PendingPinProtectedAction?> = _pendingPinProtectedAction.asStateFlow()

    private val _pinAuthError = MutableStateFlow<String?>(null)
    val pinAuthError: StateFlow<String?> = _pinAuthError.asStateFlow()

    private val _burnedTokensList = MutableStateFlow(localStorage.getAllBurnedTokens())
    val burnedTokensList: StateFlow<List<BurnedTokenInfo>> = _burnedTokensList.asStateFlow()

    private val _selectedReceiptRecord = MutableStateFlow<TransferRecord?>(null)
    val selectedReceiptRecord: StateFlow<TransferRecord?> = _selectedReceiptRecord.asStateFlow()

    // Pending Buyer Approval Modal when REQ|Amount|OTT_ID is captured via Acoustic NFC
    private val _pendingBuyerApproval = MutableStateFlow<StructuredPayload.Invoice?>(null)
    val pendingBuyerApproval: StateFlow<StructuredPayload.Invoice?> = _pendingBuyerApproval.asStateFlow()

    // Official Transfer & Live Cloud Recipient Search State
    private val _recipientDigitsInput = MutableStateFlow("620145")
    val recipientDigitsInput: StateFlow<String> = _recipientDigitsInput.asStateFlow()

    private val _liveCloudSearchRecipient = MutableStateFlow<OfficialAccountProfile?>(null)
    val liveCloudSearchRecipient: StateFlow<OfficialAccountProfile?> = _liveCloudSearchRecipient.asStateFlow()

    private var liveRecipientSearchJob: Job? = null

    private val _recipientLookupError = MutableStateFlow<String?>(null)
    val recipientLookupError: StateFlow<String?> = _recipientLookupError.asStateFlow()

    private val _pendingTransferVerification = MutableStateFlow<PendingTransferVerification?>(null)
    val pendingTransferVerification: StateFlow<PendingTransferVerification?> =
        _pendingTransferVerification.asStateFlow()

    private val _selectedNavTab = MutableStateFlow(MainNavTab.RADAR_TRANSCEIVER)
    val selectedNavTab: StateFlow<MainNavTab> = _selectedNavTab.asStateFlow()

    private val _workspaceMode = MutableStateFlow(TransceiverWorkspaceMode.SEND)
    val workspaceMode: StateFlow<TransceiverWorkspaceMode> = _workspaceMode.asStateFlow()

    // Strictly Ultrasonic Device-to-Device Vibration Mode by default (17.0 kHz – 19.95 kHz)
    private val _acousticMode = MutableStateFlow(AcousticMode.ULTRASONIC)
    val acousticMode: StateFlow<AcousticMode> = _acousticMode.asStateFlow()

    // Acoustic NFC Proximity Mode (< 10cm, Gain = 0.35, Threshold >= 205) — Enabled by default
    private val _nfcProximityMode = MutableStateFlow(true)
    val nfcProximityMode: StateFlow<Boolean> = _nfcProximityMode.asStateFlow()

    private val _threshold = MutableStateFlow(MfskProtocol.NFC_PROXIMITY_THRESHOLD)
    val threshold: StateFlow<Int> = _threshold.asStateFlow()

    // 20-Second Safety Auto-Timeout Countdown
    private val _listeningRemainingSeconds = MutableStateFlow(0)
    val listeningRemainingSeconds: StateFlow<Int> = _listeningRemainingSeconds.asStateFlow()
    private var autoTimeoutJob: Job? = null

    private val _selectedTemplate = MutableStateFlow(TemplateTab.QUICK_INVOICE)
    val selectedTemplate: StateFlow<TemplateTab> = _selectedTemplate.asStateFlow()

    // Automated E2EE Stream Cipher State (No manual PIN test field!)
    private val _isEncryptionEnabled = MutableStateFlow(true)
    val isEncryptionEnabled: StateFlow<Boolean> = _isEncryptionEnabled.asStateFlow()

    private val _secretPin = MutableStateFlow(XorStreamCipher.DEFAULT_FAST_PIN)
    val secretPin: StateFlow<String> = _secretPin.asStateFlow()

    private val _previewSalt = MutableStateFlow(Random.nextInt(1, 256))

    // Template 1: Free Text
    private val _freeTextInput = MutableStateFlow("مرحبا")
    val freeTextInput: StateFlow<String> = _freeTextInput.asStateFlow()

    // Template 2: Official Direct Payment (`PAYMENT`) by default
    private val _microOpType = MutableStateFlow(MicroOpType.PAYMENT)
    val microOpType: StateFlow<MicroOpType> = _microOpType.asStateFlow()

    private val _invoiceStoreName = MutableStateFlow("سالم عبدالرحمن علي العولقي")
    val invoiceStoreName: StateFlow<String> = _invoiceStoreName.asStateFlow()

    private val _invoiceAmount = MutableStateFlow("500.00")
    val invoiceAmount: StateFlow<String> = _invoiceAmount.asStateFlow()

    private val _invoiceCurrency = MutableStateFlow(_selectedCurrency.value.code)
    val invoiceCurrency: StateFlow<String> = _invoiceCurrency.asStateFlow()

    private val _invoiceOttId = MutableStateFlow(MfskProtocol.generateOttId())
    val invoiceOttId: StateFlow<String> = _invoiceOttId.asStateFlow()

    // Template 3: Contact Card
    private val _contactName = MutableStateFlow("أحمد محمد عبدالله الشامي")
    val contactName: StateFlow<String> = _contactName.asStateFlow()

    private val _contactPhone = MutableStateFlow("+967771234567")
    val contactPhone: StateFlow<String> = _contactPhone.asStateFlow()

    private val _contactOrg = MutableStateFlow("VibraPay FinTech")
    val contactOrg: StateFlow<String> = _contactOrg.asStateFlow()

    private val _encodedPreview = MutableStateFlow(
        MfskProtocol.encodePayload(
            text = "PAY|500.00|${_invoiceOttId.value}",
            mode = AcousticMode.ULTRASONIC,
            encrypt = true,
            secretPin = XorStreamCipher.DEFAULT_FAST_PIN,
            explicitSalt = _previewSalt.value
        )
    )
    val encodedPreview: StateFlow<EncodedPacket> = _encodedPreview.asStateFlow()

    private val _lastDecodedPacket = MutableStateFlow<DecodedPacketResult?>(null)
    val lastDecodedPacket: StateFlow<DecodedPacketResult?> = _lastDecodedPacket.asStateFlow()

    private val _loopbackSelfTestEnabled = MutableStateFlow(false)
    val loopbackSelfTestEnabled: StateFlow<Boolean> = _loopbackSelfTestEnabled.asStateFlow()

    val transceiverEngine = SonicTransceiverEngine(
        scope = viewModelScope,
        onPacketDecoded = { decoded, mode ->
            handlePacketDecoded(decoded, mode)
        }
    )

    private val _isAppForegroundActive = MutableStateFlow(true)
    val isAppForegroundActive: StateFlow<Boolean> = _isAppForegroundActive.asStateFlow()

    val spectrumFrame: StateFlow<SpectrumFrame> = transceiverEngine.spectrumFrame
    val receiverStatus: StateFlow<ReceiverStatus> = transceiverEngine.receiverStatus
    val isListening: StateFlow<Boolean> = transceiverEngine.isListening
    val singleRequestLockActive: StateFlow<Boolean> = transceiverEngine.singleRequestLockActive
    val blockedCrowdSignalsCount: StateFlow<Int> = transceiverEngine.blockedCrowdSignalsCount
    val lastBlockedCrowdAlert: StateFlow<String?> = transceiverEngine.lastBlockedCrowdAlert
    private val _highPassFilterEnabled = MutableStateFlow(true)
    val highPassFilterEnabled: StateFlow<Boolean> = _highPassFilterEnabled.asStateFlow()

    private val _smartAutoRetryEnabled = MutableStateFlow(true)
    val smartAutoRetryEnabled: StateFlow<Boolean> = _smartAutoRetryEnabled.asStateFlow()
    val currentBurstIndex: StateFlow<Int> = transceiverEngine.currentBurstIndex
    val totalPlannedBursts: StateFlow<Int> = transceiverEngine.totalPlannedBursts
    val burstStatusTextAr: StateFlow<String?> = transceiverEngine.burstStatusTextAr
    val isTransmitting: StateFlow<Boolean> = transceiverEngine.isTransmitting
    val transmitProgress: StateFlow<Float> = transceiverEngine.transmitProgress
    val activeTransmitTone: StateFlow<ScheduledTone?> = transceiverEngine.activeTransmitTone
    val liveAccumulatedHex: StateFlow<String> = transceiverEngine.liveAccumulatedHex
    val livePreviewText: StateFlow<String> = transceiverEngine.livePreviewText
    val liveDetectedSymbols: StateFlow<List<String>> = transceiverEngine.liveDetectedSymbols

    init {
        val db = VibraPayDatabase.getInstance(application)
        repository = TransferRepository(db.transferDao(), db.officialAccountDao())
        historyRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        directoryAccounts = repository.allOfficialAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorldCountriesCatalog.defaultSeedAccounts
        )
        viewModelScope.launch {
            val allAccs = repository.seedDefaultAccountsIfEmpty()
            val storedId = localStorage.getActiveAccountId()
            val active = allAccs.firstOrNull { it.accountId == storedId } ?: allAccs.firstOrNull()
            if (active != null) {
                _activeAccount.value = active
                localStorage.setActiveAccountId(active.accountId)
                if (localStorage.getSavedLoginIdentifier().isBlank()) {
                    localStorage.setSavedLoginIdentifier(active.phoneNumberLocal)
                    _savedLoginIdentifier.value = active.phoneNumberLocal
                }
                refreshInAppNotificationsForActiveAccount(active)
            }
        }
        observeCloudAuthAndConnectivity()
        syncEngineSettings()
        refreshEncodedPreview()
        verifyHardwareHmacOrFreeze()
    }

    private fun refreshInAppNotificationsForActiveAccount(account: OfficialAccountProfile? = _activeAccount.value) {
        val accId = account?.accountId ?: localStorage.getActiveAccountId() ?: "584920"
        val accName = account?.fullQuadName ?: "أحمد محمد عبدالله الشامي"
        val list = localStorage.getNotificationsForAccount(accId, accName)
        _inAppNotifications.value = list
        _unreadNotificationsCount.value = list.count { !it.isRead }
    }

    private fun pushInAppNotification(
        category: NotificationCategory,
        titleAr: String,
        messageAr: String,
        amountBadge: String = "",
        counterpartyName: String = "",
        counterpartyPhone: String = "",
        referenceId: String = ""
    ) {
        val acc = _activeAccount.value
        val accId = acc?.accountId ?: localStorage.getActiveAccountId() ?: "584920"
        val now = System.currentTimeMillis()
        val notif = InAppNotification(
            id = "NOTIF_${category.code}_${now}_${Random.nextInt(100, 999)}",
            accountId = accId,
            category = category,
            titleAr = titleAr,
            messageAr = messageAr,
            amountBadge = amountBadge,
            counterpartyName = counterpartyName,
            counterpartyPhone = counterpartyPhone,
            referenceId = referenceId,
            timestamp = now,
            isRead = false
        )
        val updated = localStorage.prependNotification(notif)
        _inAppNotifications.value = updated
        _unreadNotificationsCount.value = updated.count { !it.isRead }
    }

    fun markAllNotificationsAsRead() {
        val acc = _activeAccount.value
        val accId = acc?.accountId ?: localStorage.getActiveAccountId() ?: "584920"
        val accName = acc?.fullQuadName ?: "مستخدم VibraPay"
        val updated = localStorage.markAllNotificationsAsRead(accId, accName)
        _inAppNotifications.value = updated
        _unreadNotificationsCount.value = 0
    }

    fun markSingleNotificationAsRead(notificationId: String) {
        val accId = _activeAccount.value?.accountId ?: localStorage.getActiveAccountId() ?: "584920"
        val updated = localStorage.markSingleNotificationAsRead(accId, notificationId)
        _inAppNotifications.value = updated
        _unreadNotificationsCount.value = updated.count { !it.isRead }
    }

    fun clearAllNotifications() {
        val accId = _activeAccount.value?.accountId ?: localStorage.getActiveAccountId() ?: "584920"
        val cleared = localStorage.clearNotificationsForAccount(accId)
        _inAppNotifications.value = cleared
        _unreadNotificationsCount.value = 0
    }

    fun openMyQrDialog() {
        _showMyQrDialog.value = true
    }

    fun closeMyQrDialog() {
        _showMyQrDialog.value = false
    }

    fun openQrScannerDialog() {
        _showQrScannerDialog.value = true
    }

    fun closeQrScannerDialog() {
        _showQrScannerDialog.value = false
    }

    /**
     * Triggered immediately when a QR Code is scanned via Camera or decoded from Gallery:
     * Automatically populates the recipient's Phone/Account ID and Full Quadruple Name
     * into the transfer screen so the user only needs to enter the amount and confirm!
     */
    fun onQrCodeRecipientScanned(scanned: QrScannedRecipient) {
        _showQrScannerDialog.value = false
        _showMyQrDialog.value = false
        _selectedNavTab.value = MainNavTab.RADAR_TRANSCEIVER
        _workspaceMode.value = TransceiverWorkspaceMode.SEND
        _selectedTemplate.value = TemplateTab.QUICK_INVOICE
        _microOpType.value = MicroOpType.PAYMENT

        val cleanDigits = scanned.phoneNumberLocal.filter { it.isDigit() }
            .ifBlank { scanned.accountId.filter { it.isDigit() } }
        _recipientDigitsInput.value = cleanDigits
        _recipientLookupError.value = null
        _invoiceStoreName.value = scanned.fullQuadName

        viewModelScope.launch {
            val matched = repository.lookupRecipientByDigits(cleanDigits)?.first
                ?: directoryAccounts.value.firstOrNull {
                    it.accountId == scanned.accountId || it.phoneNumberLocal == scanned.phoneNumberLocal
                }
            if (matched != null) {
                _liveCloudSearchRecipient.value = matched
                _invoiceStoreName.value = matched.fullQuadName
            }
        }

        _lastTopUpBanner.value =
            "تم مسح رمز QR بنجاح وتعبئة بيانات المستلم (${scanned.fullQuadName} • #${scanned.accountId}) تلقائياً ✓"
        transceiverEngine.playFinancialSuccessChime()
        triggerSuccessPaymentHaptics()
    }

    // =========================================================================
    // 0. Real-Time Cloud Sync, Google Auth & Smart Hybrid Sync (Online/Offline)
    // =========================================================================
    private fun observeCloudAuthAndConnectivity() {
        // 1. Observe Firebase Auth state (via Google Sign-In CredentialManager)
        viewModelScope.launch {
            GoogleAuthManager.authStateFlow().collectLatest { user ->
                _firebaseUser.value = user
                if (user != null) {
                    _cloudSyncStatusAr.value = "متصل بالسحابة الفورية (${user.email ?: "Google Cloud"}) ✓"
                    startRealTimeCloudListeners(user)
                    syncOfflineBalancesAndTransfersToCloud()
                } else {
                    stopRealTimeCloudListeners()
                    _cloudSyncStatusAr.value = if (isNetworkOnline.value) {
                        "متصل بالإنترنت • اربط حساب Google لتفعيل السحابة المشتركة"
                    } else {
                        "وضع الأوفلاين (الموجات الصوتية Ultrasonic نشطة)"
                    }
                }
            }
        }

        // 2. Observe live network connectivity transitions for Smart Hybrid Sync
        viewModelScope.launch {
            isNetworkOnline.collectLatest { online ->
                if (online) {
                    if (_firebaseUser.value != null) {
                        _cloudSyncStatusAr.value = "متصل بالسحابة الفورية • جاري مزامنة الأرصدة..."
                        syncOfflineBalancesAndTransfersToCloud()
                    } else {
                        _cloudSyncStatusAr.value = "متصل بالإنترنت (Online) • جاهز للمزامنة السحابية"
                    }
                } else {
                    _cloudSyncStatusAr.value = "وضع الأوفلاين (Offline Ultrasonic) • ستتم المزامنة فور عودة الإنترنت"
                }
            }
        }
    }

    private fun startRealTimeCloudListeners(user: FirebaseUser) {
        stopRealTimeCloudListeners()

        // Listener A: Live Cloud Accounts Snapshot Listener
        cloudAccountsListenerJob = viewModelScope.launch {
            cloudRepository.observeMyCloudAccounts(user.uid).collectLatest { cloudAccounts ->
                if (cloudAccounts.isEmpty()) return@collectLatest
                val profiles = cloudAccounts.map { doc ->
                    val savedEmail = localStorage.getSavedAccountEmail(doc.accountId).orEmpty()
                    val savedPass = localStorage.getSavedAccountPassword(
                        accountId = doc.accountId,
                        email = doc.email.ifBlank { savedEmail },
                        phoneLocal = doc.phoneNumberLocal,
                        fullPhone = doc.fullPhoneDigits
                    ).orEmpty()
                    val baseProfile = doc.toOfficialAccountProfile(savedEmail)
                    if (savedPass.isNotBlank() && (baseProfile.passwordPlainOrHash.isBlank() || baseProfile.passwordPlainOrHash == "123456")) {
                        baseProfile.copy(passwordPlainOrHash = savedPass)
                    } else {
                        baseProfile
                    }
                }
                profiles.forEach { cloudAcc ->
                    repository.upsertOfficialAccount(cloudAcc, preserveExistingCredentialsOnFallback = true)
                }
                val currentActiveId = _activeAccount.value?.accountId
                if (currentActiveId != null) {
                    val updatedActive = repository.getAccountById(currentActiveId)
                        ?: profiles.firstOrNull { it.accountId == currentActiveId }
                    if (updatedActive != null) {
                        _activeAccount.value = updatedActive
                        localStorage.setWalletBalance(updatedActive.balanceYer, WalletCurrency.YER)
                        localStorage.setWalletBalance(updatedActive.balanceSar, WalletCurrency.SAR)
                        localStorage.setWalletBalance(updatedActive.balanceUsd, WalletCurrency.USD)
                        _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
                        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
                        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
                    }
                }
            }
        }

        // Listener B: Live Incoming & Outgoing Cloud Transfers Listener (with Instant Notification!)
        cloudTransfersListenerJob = viewModelScope.launch {
            cloudRepository.observeParticipantTransfers(user.uid).collectLatest { transfers ->
                val myActiveAccountId = _activeAccount.value?.accountId
                transfers.forEach { cloudTx ->
                    val isNewSessionEvent = processedCloudTransferIds.add(cloudTx.transferId)
                    if (isNewSessionEvent && myActiveAccountId != null &&
                        cloudTx.recipientAccountId == myActiveAccountId &&
                        cloudTx.senderAccountId != myActiveAccountId
                    ) {
                        val formattedAmt = String.format(Locale.US, "%,.2f", cloudTx.amount)
                        val incomingRefId = "VP-REF-${cloudTx.ottId.ifBlank { "9012" }}-${((System.currentTimeMillis() / 1000L) % 1000000L).toString().padStart(6, '0')}"
                        CloudNotificationHelper.notifyIncomingCloudTransfer(
                            context = getApplication(),
                            senderQuadName = cloudTx.senderQuadName,
                            amountFormatted = formattedAmt,
                            currencyCode = cloudTx.currency,
                            ottId = cloudTx.ottId.ifBlank { cloudTx.transferId }
                        )
                        pushInAppNotification(
                            category = NotificationCategory.FINANCIAL,
                            titleAr = "استلمت حوالة مالية سحابية فورية ↙",
                            messageAr = "تم إيداع مبلغ $formattedAmt ${cloudTx.currency} في رصيد محفظتك السحابية من المرسل (${cloudTx.senderQuadName}).",
                            amountBadge = "+$formattedAmt ${cloudTx.currency}",
                            counterpartyName = cloudTx.senderQuadName,
                            counterpartyPhone = "ID: ${cloudTx.senderAccountId}",
                            referenceId = incomingRefId
                        )
                        _lastTopUpBanner.value =
                            "إشعار سحابي فوري: استلمت $formattedAmt ${cloudTx.currency} من ${cloudTx.senderQuadName} ✓"
                        transceiverEngine.playFinancialSuccessChime()
                        triggerSuccessPaymentHaptics()
                    }
                }
            }
        }
    }

    private fun stopRealTimeCloudListeners() {
        cloudAccountsListenerJob?.cancel()
        cloudAccountsListenerJob = null
        cloudTransfersListenerJob?.cancel()
        cloudTransfersListenerJob = null
    }

    /**
     * Smart Hybrid Sync (المزامنة الذكية بين الأونلاين والأوفلاين):
     * Automatically pushes local accounts and any offline ultrasonic balance changes
     * to Firebase Firestore as soon as internet connectivity and auth are available.
     */
    fun syncOfflineBalancesAndTransfersToCloud() {
        val user = _firebaseUser.value ?: return
        if (!isNetworkOnline.value) return

        viewModelScope.launch {
            val allLocalAccounts = repository.seedDefaultAccountsIfEmpty()
            val active = _activeAccount.value ?: allLocalAccounts.firstOrNull()
            if (active != null) {
                val syncedActive = active.copy(
                    balanceYer = localStorage.getWalletBalance(WalletCurrency.YER),
                    balanceSar = localStorage.getWalletBalance(WalletCurrency.SAR),
                    balanceUsd = localStorage.getWalletBalance(WalletCurrency.USD)
                )
                repository.upsertOfficialAccount(syncedActive)
                _activeAccount.value = syncedActive

                cloudRepository.saveUserPrivateProfile(
                    accountId = syncedActive.accountId,
                    email = syncedActive.email.ifBlank { user.email ?: "" },
                    fullQuadName = syncedActive.fullQuadName
                )
            }

            var syncedCount = 0
            for (acc in repository.seedDefaultAccountsIfEmpty()) {
                if (cloudRepository.registerOrSyncCloudAccount(acc).isSuccess) {
                    syncedCount++
                }
            }
            if (_pendingOfflineSyncCount.value > 0) {
                _lastTopUpBanner.value =
                    "تمت المزامنة الذكية لـ ${_pendingOfflineSyncCount.value} عملية أوفلاين وتوحيد الأرصدة مع السحابة ✓"
                _pendingOfflineSyncCount.value = 0
            }
            _cloudSyncStatusAr.value = "متزامن لحظياً مع السحابة (${syncedCount} حسابات نشطة) ✓"
        }
    }

    /**
     * 1) Dedicated Programmatic Function for Email Verification OTP:
     * First executes a strict 3-field OR duplicate check across Local & Cloud Databases
     * (Email OR Phone Number OR Normalized Quadruple Name).
     * If any of the 3 fields already exists, stops immediately and returns:
     * "يوجد حساب مسجل بالفعل بهذه البيانات (الاسم أو رقم الهاتف أو البريد الإلكتروني)".
     * Otherwise sends the OTP via EmailJS (`BuildConfig.EMAILJS_TEMPLATE_ID`).
     */
    fun sendRealEmailOtp(
        email: String,
        recipientName: String,
        phoneLocalDigits: String = "",
        fullPhoneDigits: String = "",
        fullQuadName: String = "",
        onResult: (otpCode: String, statusMessageAr: String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            val cleanLocalPhone = phoneLocalDigits.filter { it.isDigit() }
            val cleanFullPhone = fullPhoneDigits.filter { it.isDigit() }.ifBlank { cleanLocalPhone }
            val normalizedQuadName = TransferRepository.normalizeQuadName(
                fullQuadName.ifBlank { recipientName }
            )

            // 1. Check duplicate in Local Database (OR condition: Email OR Phone OR Quadruple Name)
            val localDuplicate = repository.findDuplicateAccount(
                email = cleanEmail,
                phoneLocalDigits = cleanLocalPhone,
                fullPhoneDigits = cleanFullPhone,
                fullQuadName = normalizedQuadName
            )

            // 2. Check duplicate in Cloud Database (OR condition: Email OR Phone OR Quadruple Name)
            val cloudDuplicate = if (localDuplicate == null && isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.hasDuplicateAccountInCloud(
                    email = cleanEmail,
                    phoneLocalDigits = cleanLocalPhone,
                    fullPhoneDigits = cleanFullPhone,
                    fullQuadName = normalizedQuadName
                ).getOrDefault(false)
            } else {
                false
            }

            if (localDuplicate != null || cloudDuplicate) {
                _authErrorMessage.value = TransferRepository.DUPLICATE_ACCOUNT_ERROR_AR
                onResult("", TransferRepository.DUPLICATE_ACCOUNT_ERROR_AR)
                triggerWarningHaptics()
                return@launch
            }

            _authErrorMessage.value = null
            val result = RealEmailOtpService.sendVerificationOtpToEmail(
                context = getApplication(),
                recipientEmail = cleanEmail,
                recipientFullName = normalizedQuadName.ifBlank { recipientName }
            )
            if (!result.deliveredViaSmtpApi || result.otpCode.isBlank()) {
                triggerWarningHaptics()
                onResult("", result.statusMessageAr)
            } else {
                onResult(result.otpCode, result.statusMessageAr)
            }
        }
    }

    /**
     * 2) Dedicated Programmatic Function for Forgot Password / Password Reset OTP:
     * Verifies that the entered email/identifier belongs to an actual registered account,
     * then calls [RealEmailOtpService.sendPasswordResetOtpToEmail] using the separate Password Reset
     * Template ID (`BuildConfig.EMAILJS_RESET_TEMPLATE_ID` / `template_xxxx`) to send the OTP exclusively
     * to the account's real registered email inbox.
     */
    fun sendPasswordResetEmailOtp(
        identifierOrEmail: String,
        onResult: (otpCode: String, resolvedEmail: String, statusMessageAr: String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanInput = identifierOrEmail.trim()
            if (cleanInput.isBlank()) {
                onResult(
                    "",
                    "",
                    "يرجى إدخال البريد الإلكتروني المسجل أو رقم الهاتف المرتبط بحسابك لإرسال كود استعادة كلمة المرور"
                )
                triggerWarningHaptics()
                return@launch
            }

            var matchedAccount = repository.findAccountForLogin(cleanInput)
            if (matchedAccount == null && isNetworkOnline.value && _firebaseUser.value != null) {
                val cloudMatch = cloudRepository.findAccountInCloudForLogin(cleanInput).getOrNull()
                if (cloudMatch != null) {
                    repository.upsertOfficialAccount(cloudMatch, preserveExistingCredentialsOnFallback = true)
                    matchedAccount = repository.getAccountById(cloudMatch.accountId) ?: cloudMatch
                }
            }

            val resolvedEmailFromVault = matchedAccount?.let {
                it.email.trim().ifBlank { localStorage.getSavedAccountEmail(it.accountId).orEmpty() }
            }.orEmpty()

            val finalEmail = when {
                resolvedEmailFromVault.contains("@") -> resolvedEmailFromVault
                cleanInput.contains("@") && matchedAccount != null -> cleanInput
                else -> ""
            }

            if (matchedAccount == null || finalEmail.isBlank() || !finalEmail.contains("@")) {
                onResult(
                    "",
                    "",
                    "لا يوجد حساب رسمي مسجل مرتبط بـ ($cleanInput) • يرجى إدخال البريد الإلكتروني الحقيقي المسجل في حسابك"
                )
                triggerWarningHaptics()
                return@launch
            }

            val recipientName = matchedAccount.fullQuadName

            val result = RealEmailOtpService.sendPasswordResetOtpToEmail(
                context = getApplication(),
                recipientEmail = finalEmail,
                recipientFullName = recipientName
            )
            if (!result.deliveredViaSmtpApi || result.otpCode.isBlank()) {
                triggerWarningHaptics()
                onResult("", finalEmail, result.statusMessageAr)
            } else {
                onResult(result.otpCode, finalEmail, result.statusMessageAr)
            }
        }
    }

    /**
     * Completes Password Reset after the user verifies the 6-digit Password Reset OTP
     * received in their personal Gmail inbox.
     * Updates the password in Room, Hardware Encrypted Credential Vault, AND Cloud Database (`Firestore`),
     * so the user can immediately log in with their new password without any conflict.
     */
    fun resetPasswordWithVerifiedOtp(
        identifierOrEmail: String,
        newPassword: String,
        onResult: (success: Boolean, messageAr: String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanIdentifier = identifierOrEmail.trim()
            val cleanNewPassword = TransferRepository.normalizePassword(newPassword)
            if (cleanNewPassword.length < 4) {
                onResult(false, "يرجى إدخال كلمة مرور جديدة صحيحة (4 خانات على الأقل)")
                triggerWarningHaptics()
                return@launch
            }

            // Ensure account is present locally if it was only in the cloud
            var targetAccount = repository.findAccountForLogin(cleanIdentifier)
            if (targetAccount == null && isNetworkOnline.value && _firebaseUser.value != null) {
                val cloudMatch = cloudRepository.findAccountInCloudForLogin(cleanIdentifier).getOrNull()
                if (cloudMatch != null) {
                    repository.upsertOfficialAccount(cloudMatch, preserveExistingCredentialsOnFallback = false)
                    targetAccount = cloudMatch
                }
            }

            val updated = repository.resetAccountPasswordByIdentifier(cleanIdentifier, cleanNewPassword)
                ?: targetAccount?.copy(passwordPlainOrHash = cleanNewPassword)?.also {
                    repository.upsertOfficialAccount(it, preserveExistingCredentialsOnFallback = false)
                }

            if (updated == null) {
                onResult(
                    false,
                    "لم يتم العثور على حساب مطابق لـ ($cleanIdentifier) لتحديث كلمة المرور"
                )
                triggerWarningHaptics()
                return@launch
            }

            // Seal new password in hardware-backed credential store so it is never lost
            localStorage.saveAccountCredentials(
                accountId = updated.accountId,
                email = updated.email,
                phoneLocal = updated.phoneNumberLocal,
                fullPhone = updated.fullPhoneDigits,
                password = cleanNewPassword
            )
            localStorage.setSavedLoginIdentifier(cleanIdentifier.ifBlank { updated.phoneNumberLocal })
            _savedLoginIdentifier.value = localStorage.getSavedLoginIdentifier()

            if (_activeAccount.value?.accountId == updated.accountId) {
                _activeAccount.value = updated
            }

            // Immediately update password in Cloud Database (Firestore)
            if (isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.updateAccountPasswordInCloud(updated.accountId, cleanNewPassword)
                cloudRepository.registerOrSyncCloudAccount(updated)
            }

            _authErrorMessage.value = null
            _lastTopUpBanner.value =
                "تم تحديث كلمة المرور في قاعدة البيانات السحابية لحساب (${updated.fullQuadName}) بنجاح ✓"
            pushInAppNotification(
                category = NotificationCategory.SECURITY,
                titleAr = "تم إعادة تعيين كلمة مرور الحساب السحابي 🔒",
                messageAr = "تم التحقق من رمز البريد الإلكتروني وتحديث كلمة المرور الرسمية لحساب (${updated.fullQuadName}) في السحابة بنجاح.",
                amountBadge = "تحديث أمني",
                counterpartyName = updated.fullQuadName,
                counterpartyPhone = updated.formattedInternationalPhone,
                referenceId = "SEC-PWD-${System.currentTimeMillis() % 100000}"
            )
            transceiverEngine.playFinancialSuccessChime()
            triggerSuccessPaymentHaptics()
            onResult(
                true,
                "تم تحديث كلمة المرور في قاعدة البيانات السحابية بنجاح ✓ يمكنك الآن تسجيل الدخول بكلمة المرور الجديدة فوراً"
            )
        }
    }

    // =========================================================================
    // 1. Official Registration & Smart Login Methods
    // =========================================================================
    fun openOfficialAuthModal(registerMode: Boolean) {
        _authErrorMessage.value = null
        _authModalInitialRegister.value = registerMode
        _showAuthModal.value = true
    }

    fun clearAuthErrorMessage() {
        _authErrorMessage.value = null
    }

    fun closeOfficialAuthModal() {
        if (!_isSessionLockedForSmartLogin.value) {
            _showAuthModal.value = false
            _authErrorMessage.value = null
        }
    }

    fun dismissNewlyRegisteredModal() {
        _newlyRegisteredAccount.value = null
    }

    fun logoutToSmartLoginScreen() {
        stopListening()
        stopTransmitting()
        _activeAccount.value?.let { acc ->
            // Save the user's phone or email so it appears pre-filled automatically on Smart Login!
            localStorage.setSavedLoginIdentifier(acc.phoneNumberLocal)
            _savedLoginIdentifier.value = acc.phoneNumberLocal
        }
        localStorage.setSessionAuthenticated(false)
        HardwareSecurityManager.clearEncryptedCloudSession(getApplication())
        _authErrorMessage.value = null
        _authModalInitialRegister.value = false
        _isSessionLockedForSmartLogin.value = true
        _showAuthModal.value = true
    }

    fun loginWithPassword(identifier: String, password: String) {
        viewModelScope.launch {
            val cleanIdentifier = identifier.trim()
            val cleanPassword = TransferRepository.normalizePassword(password)
            if (cleanIdentifier.isBlank() || cleanPassword.isBlank()) {
                _authErrorMessage.value = "يرجى إدخال البريد الإلكتروني أو رقم الهاتف مع كلمة المرور"
                triggerWarningHaptics()
                return@launch
            }

            var matched = repository.findAccountForLogin(cleanIdentifier)

            // Also check Cloud Database if not found locally OR to fetch latest cloud password
            if (isNetworkOnline.value && _firebaseUser.value != null) {
                val cloudMatch = cloudRepository.findAccountInCloudForLogin(cleanIdentifier).getOrNull()
                if (cloudMatch != null) {
                    if (matched == null) {
                        repository.upsertOfficialAccount(cloudMatch, preserveExistingCredentialsOnFallback = true)
                        matched = repository.getAccountById(cloudMatch.accountId) ?: cloudMatch
                    } else if (cloudMatch.passwordPlainOrHash.trim().isNotBlank() &&
                        cloudMatch.passwordPlainOrHash.trim() != "123456" &&
                        cloudMatch.passwordPlainOrHash.trim() == cleanPassword
                    ) {
                        val syncedWithCloudPass = matched.copy(
                            passwordPlainOrHash = cloudMatch.passwordPlainOrHash.trim(),
                            email = cloudMatch.email.trim().ifBlank { matched.email }
                        )
                        repository.upsertOfficialAccount(syncedWithCloudPass, preserveExistingCredentialsOnFallback = false)
                        matched = syncedWithCloudPass
                    }
                }
            }

            if (matched == null) {
                _authErrorMessage.value = "لم يتم العثور على حساب مسجل بهذا البريد الإلكتروني أو رقم الهاتف"
                triggerWarningHaptics()
                return@launch
            }

            val vaultSavedPassword = localStorage.getSavedAccountPassword(
                accountId = matched.accountId,
                email = matched.email.ifBlank { cleanIdentifier },
                phoneLocal = matched.phoneNumberLocal,
                fullPhone = matched.fullPhoneDigits
            )

            val isPasswordCorrect = TransferRepository.passwordsMatch(
                storedPassword = matched.passwordPlainOrHash,
                enteredPassword = cleanPassword,
                vaultSavedPassword = vaultSavedPassword
            )

            if (!isPasswordCorrect) {
                _authErrorMessage.value = "كلمة المرور غير صحيحة! يرجى التأكد من كلمة المرور والمحاولة مرة أخرى"
                triggerWarningHaptics()
                return@launch
            }

            // Ensure normalized password and email are persisted consistently
            val verifiedAccount = matched.copy(passwordPlainOrHash = cleanPassword)
            repository.upsertOfficialAccount(verifiedAccount, preserveExistingCredentialsOnFallback = false)
            localStorage.saveAccountCredentials(
                accountId = verifiedAccount.accountId,
                email = verifiedAccount.email,
                phoneLocal = verifiedAccount.phoneNumberLocal,
                fullPhone = verifiedAccount.fullPhoneDigits,
                password = cleanPassword
            )

            completeSmartLogin(verifiedAccount, cleanIdentifier)
        }
    }

    fun loginWithBiometricVerified(identifier: String) {
        viewModelScope.launch {
            val matched = repository.findAccountForLogin(identifier.trim())
                ?: repository.seedDefaultAccountsIfEmpty().firstOrNull()
            if (matched != null) {
                completeSmartLogin(matched, matched.phoneNumberLocal)
            }
        }
    }

    private fun completeSmartLogin(account: OfficialAccountProfile, usedIdentifier: String) {
        _activeAccount.value = account
        localStorage.setActiveAccountId(account.accountId)
        localStorage.setSavedLoginIdentifier(usedIdentifier.trim().ifBlank { account.phoneNumberLocal })
        localStorage.setSessionAuthenticated(true)
        _savedLoginIdentifier.value = localStorage.getSavedLoginIdentifier()

        // Seal authenticated user session inside Android Keystore EncryptedSharedPreferences
        HardwareSecurityManager.saveEncryptedCloudSession(
            context = getApplication(),
            sessionUid = _firebaseUser.value?.uid ?: "CLOUD_SESSION_${account.accountId}",
            accountId = account.accountId,
            email = account.email
        )

        // Sync active account balances into Hardware Keystore Protected Wallet
        localStorage.syncAuthoritativeBalancesFromCloud(
            balanceYer = account.balanceYer,
            balanceSar = account.balanceSar,
            balanceUsd = account.balanceUsd
        )
        _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

        _isSessionLockedForSmartLogin.value = false
        _showAuthModal.value = false
        _authErrorMessage.value = null
        _cloudSyncStatusAr.value =
            "مرتبط بالسحابة تلقائياً وبشكل دائم • حساب (${account.accountId}) متزامن حياً ✓"
        refreshInAppNotificationsForActiveAccount(account)
        pushInAppNotification(
            category = NotificationCategory.SECURITY,
            titleAr = "تسجيل دخول جديد إلى حسابك السحابي 🔐",
            messageAr = "تم تسجيل الدخول بنجاح إلى حساب (${account.fullQuadName}) وتفعيل الربط السحابي المشفر.",
            amountBadge = "دخول آمن",
            counterpartyName = account.fullQuadName,
            counterpartyPhone = account.formattedInternationalPhone,
            referenceId = "SEC-LOGIN-${System.currentTimeMillis() % 100000}"
        )
        transceiverEngine.playFinancialSuccessChime()
        triggerSuccessPaymentHaptics()

        // Immediately bind to Cloud Database and load authoritative balances & transactions
        viewModelScope.launch {
            if (isNetworkOnline.value && _firebaseUser.value != null) {
                val authoritativeCloudAcc =
                    cloudRepository.fetchAuthoritativeCloudAccount(account.accountId).getOrNull()
                if (authoritativeCloudAcc != null) {
                    val merged = account.copy(
                        balanceYer = authoritativeCloudAcc.balanceYer,
                        balanceSar = authoritativeCloudAcc.balanceSar,
                        balanceUsd = authoritativeCloudAcc.balanceUsd
                    )
                    repository.upsertOfficialAccount(merged, preserveExistingCredentialsOnFallback = true)
                    _activeAccount.value = merged
                    localStorage.syncAuthoritativeBalancesFromCloud(
                        balanceYer = merged.balanceYer,
                        balanceSar = merged.balanceSar,
                        balanceUsd = merged.balanceUsd
                    )
                    _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
                    _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
                    _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
                }
                syncOfflineBalancesAndTransfersToCloud()
            }
        }
    }

    /**
     * Official Registration Flow:
     * Enforces strict 3-field OR duplicate checks (Email OR Phone Number OR Full Quadruple Name) across
     * Cloud and Local DB before generating a unique 6-digit numeric Account ID.
     */
    fun registerNewOfficialAccount(
        firstName: String,
        fatherName: String,
        grandfatherName: String,
        familyName: String,
        country: WorldCountry,
        phoneLocalDigits: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            val dialDigits = country.dialCode.filter { it.isDigit() }
            val cleanLocalPhone = phoneLocalDigits.filter { it.isDigit() }
            val fullPhoneDigits = "$dialDigits$cleanLocalPhone"
            val cleanEmail = email.trim()
            val cleanPassword = TransferRepository.normalizePassword(password)
            val candidateQuadName = TransferRepository.normalizeQuadName(
                "${firstName.trim()} ${fatherName.trim()} ${grandfatherName.trim()} ${familyName.trim()}"
            )

            // 1. Check duplicate in local repository (OR condition: Email OR Phone Number OR Quadruple Name)
            val localDuplicate = repository.findDuplicateAccount(
                email = cleanEmail,
                phoneLocalDigits = cleanLocalPhone,
                fullPhoneDigits = fullPhoneDigits,
                fullQuadName = candidateQuadName
            )

            // 2. Check duplicate in Cloud Database if online (OR condition: Email OR Phone Number OR Quadruple Name)
            val cloudDuplicate = if (localDuplicate == null && isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.hasDuplicateAccountInCloud(
                    email = cleanEmail,
                    phoneLocalDigits = cleanLocalPhone,
                    fullPhoneDigits = fullPhoneDigits,
                    fullQuadName = candidateQuadName
                ).getOrDefault(false)
            } else {
                false
            }

            if (localDuplicate != null || cloudDuplicate) {
                _authErrorMessage.value = TransferRepository.DUPLICATE_ACCOUNT_ERROR_AR
                triggerWarningHaptics()
                return@launch
            }

            val existingAccounts = repository.seedDefaultAccountsIfEmpty()
            val existingIds = existingAccounts.map { it.accountId }.toSet()

            // Generate a unique 6-digit numeric-only Account ID (5+ digits, numbers only, no letters)
            var generatedNumericId: String
            do {
                generatedNumericId = Random.nextInt(100000, 999999).toString()
            } while (generatedNumericId in existingIds)

            val newProfile = OfficialAccountProfile(
                accountId = generatedNumericId,
                firstName = firstName.trim(),
                fatherName = fatherName.trim(),
                grandfatherName = grandfatherName.trim(),
                familyName = familyName.trim(),
                countryDialCode = country.dialCode,
                countryNameAr = country.nameAr,
                countryFlag = country.flag,
                phoneNumberLocal = cleanLocalPhone,
                fullPhoneDigits = fullPhoneDigits,
                email = cleanEmail,
                passwordPlainOrHash = cleanPassword,
                balanceYer = WalletCurrency.YER.defaultInitialBalance,
                balanceSar = WalletCurrency.SAR.defaultInitialBalance,
                balanceUsd = WalletCurrency.USD.defaultInitialBalance
            )

            localStorage.saveAccountCredentials(
                accountId = newProfile.accountId,
                email = cleanEmail,
                phoneLocal = cleanLocalPhone,
                fullPhone = fullPhoneDigits,
                password = cleanPassword
            )
            repository.upsertOfficialAccount(newProfile, preserveExistingCredentialsOnFallback = false)
            completeSmartLogin(newProfile, cleanLocalPhone)
            _newlyRegisteredAccount.value = newProfile
            _lastTopUpBanner.value =
                "تم إنشاء الحساب الرسمي (${newProfile.fullQuadName}) وربطه بالسحابة • رقم الحساب المالي: ${newProfile.accountId} ✓"

            // Immediately sync newly registered official account to Firebase Firestore if online
            if (isNetworkOnline.value && _firebaseUser.value != null) {
                val synced = cloudRepository.registerOrSyncCloudAccount(newProfile).isSuccess
                cloudRepository.saveUserPrivateProfile(
                    accountId = newProfile.accountId,
                    email = newProfile.email,
                    fullQuadName = newProfile.fullQuadName
                )
                if (synced) {
                    _cloudSyncStatusAr.value =
                        "تم تسجيل الحساب الرسمي (${newProfile.accountId}) وربطه الدائم بالسحابة الفورية ✓"
                }
            } else {
                _pendingOfflineSyncCount.value += 1
            }
        }
    }

    // =========================================================================
    // 2. Dual Biometric Settings, Theme Control & Cloud Password Update
    // =========================================================================
    fun setLoginBiometricEnabled(enabled: Boolean) {
        localStorage.setLoginBiometricEnabled(enabled)
        _isLoginBiometricEnabled.value = enabled
        _lastTopUpBanner.value = if (enabled) {
            "تم تفعيل استخدام البصمة لتسجيل الدخول عبر BiometricPrompt ✓"
        } else {
            "تم إيقاف استخدام البصمة لتسجيل الدخول"
        }
    }

    fun setTransactionBiometricEnabled(enabled: Boolean) {
        localStorage.setTransactionBiometricEnabled(enabled)
        _isTransactionBiometricEnabled.value = enabled
        _lastTopUpBanner.value = if (enabled) {
            "تم تفعيل استخدام البصمة لتأكيد العمليات المالية عبر BiometricPrompt ✓"
        } else {
            "تم إيقاف استخدام البصمة لتأكيد العمليات المالية"
        }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        localStorage.setAppThemeModeCode(mode.code)
        _appThemeMode.value = mode
        _showThemeSelectorDialog.value = false
        _lastTopUpBanner.value = "تم تفعيل ${mode.titleAr} وحفظه محلياً بشكل دائم ✓"
    }

    fun openThemeSelectorDialog() {
        _showThemeSelectorDialog.value = true
    }

    fun closeThemeSelectorDialog() {
        _showThemeSelectorDialog.value = false
    }

    fun openChangePasswordDialog() {
        _showChangePasswordDialog.value = true
    }

    fun closeChangePasswordDialog() {
        _showChangePasswordDialog.value = false
    }

    fun changeActiveAccountPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val currentAcc = _activeAccount.value
        if (currentAcc == null) {
            onResult(false, "لا يوجد حساب سحابي نشط حالياً")
            return
        }
        if (currentPassword.isBlank()) {
            onResult(false, "يرجى إدخال كلمة المرور الحالية")
            return
        }
        val cleanNew = TransferRepository.normalizePassword(newPassword)
        val cleanConfirm = TransferRepository.normalizePassword(confirmPassword)
        if (cleanNew.length < 4) {
            onResult(false, "يجب أن تتكون كلمة المرور الجديدة من 4 خانات على الأقل")
            return
        }
        if (cleanNew != cleanConfirm) {
            onResult(false, "كلمة المرور الجديدة وتأكيدها غير متطابقين")
            return
        }

        viewModelScope.launch {
            val vaultPass = localStorage.getSavedAccountPassword(
                accountId = currentAcc.accountId,
                email = currentAcc.email,
                phoneLocal = currentAcc.phoneNumberLocal,
                fullPhone = currentAcc.fullPhoneDigits
            )
            val result = repository.updateAccountPassword(
                accountId = currentAcc.accountId,
                currentPassword = currentPassword,
                newPassword = cleanNew,
                vaultSavedPassword = vaultPass
            )
            result.onSuccess { updatedProfile ->
                localStorage.saveAccountCredentials(
                    accountId = updatedProfile.accountId,
                    email = updatedProfile.email,
                    phoneLocal = updatedProfile.phoneNumberLocal,
                    fullPhone = updatedProfile.fullPhoneDigits,
                    password = cleanNew
                )
                _activeAccount.value = updatedProfile
                if (isNetworkOnline.value && _firebaseUser.value != null) {
                    cloudRepository.updateAccountPasswordInCloud(updatedProfile.accountId, cleanNew)
                    cloudRepository.registerOrSyncCloudAccount(updatedProfile)
                }
                _showChangePasswordDialog.value = false
                _lastTopUpBanner.value = "تم تحديث كلمة مرور الحساب السحابي (${updatedProfile.accountId}) بنجاح ✓"
                pushInAppNotification(
                    category = NotificationCategory.SECURITY,
                    titleAr = "تم تغيير كلمة مرور الحساب السحابي 🔒",
                    messageAr = "تم تحديث كلمة المرور الخاصة بحسابك السحابي (${updatedProfile.fullQuadName}) ومزامنتها فوراً.",
                    amountBadge = "حماية الحساب",
                    counterpartyName = updatedProfile.fullQuadName,
                    counterpartyPhone = updatedProfile.formattedInternationalPhone,
                    referenceId = "SEC-PASS-${System.currentTimeMillis() % 100000}"
                )
                transceiverEngine.playFinancialSuccessChime()
                triggerSuccessPaymentHaptics()
                onResult(true, "تم تحديث كلمة مرور الحساب السحابي بنجاح ✓")
            }.onFailure { err ->
                triggerWarningHaptics()
                onResult(false, err.message ?: "تعذر تحديث كلمة المرور")
            }
        }
    }

    // =========================================================================
    // 3. Official Transfer & Live Cloud Search Flow (آلية التحويل المالي السحابي المباشر)
    // =========================================================================
    fun updateRecipientDigitsInput(rawInput: String) {
        // Strictly accept digits ONLY (numbers only, no letters)
        val digitsOnly = rawInput.filter { it.isDigit() }.take(16)
        _recipientDigitsInput.value = digitsOnly
        _recipientLookupError.value = null
        triggerLiveCloudRecipientSearch(digitsOnly)
    }

    private fun triggerLiveCloudRecipientSearch(cleanDigits: String) {
        liveRecipientSearchJob?.cancel()
        if (cleanDigits.length < 5) {
            _liveCloudSearchRecipient.value = null
            return
        }
        liveRecipientSearchJob = viewModelScope.launch {
            delay(180L)
            val cloudMatch = if (isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.lookupRecipientInCloud(cleanDigits).getOrNull()
            } else {
                null
            }
            val resolved = cloudMatch ?: repository.lookupRecipientByDigits(cleanDigits)?.first
            _liveCloudSearchRecipient.value = resolved
            if (resolved != null) {
                _invoiceStoreName.value = resolved.fullQuadName
            }
        }
    }

    fun updateTransferAmountInput(rawAmount: String) {
        val clean = rawAmount.filter { it.isDigit() || it == '.' }
        _invoiceAmount.value = clean
        _recipientLookupError.value = null
        refreshEncodedPreview()
    }

    /**
     * Triggered when user clicks [تحويل]:
     * Looks up the recipient by Phone Number or 5+ digit numeric Account ID,
     * and opens the Recipient Verification Modal (نافذة مراجعة المستلم) showing
     * the recipient's full Quadruple Name, amount, and [إتمام] / [إلغاء] buttons.
     */
    fun initiateOfficialTransferLookup() {
        if (!verifyHardwareHmacOrFreeze()) return
        val cleanDigits = _recipientDigitsInput.value.filter { it.isDigit() }
        if (cleanDigits.length < 5) {
            _recipientLookupError.value =
                "يرجى إدخال رقم هاتف المستلم أو المعرف المالي المكون من 5 أرقام فما فوق (أرقام فقط)"
            triggerWarningHaptics()
            return
        }

        val amountVal = _invoiceAmount.value.toDoubleOrNull() ?: 0.0
        if (amountVal <= 0.0) {
            _recipientLookupError.value = "يرجى إدخال مبلغ صحيح أكبر من صفر"
            triggerWarningHaptics()
            return
        }

        val currentBal = _walletBalance.value
        if (amountVal > currentBal) {
            _recipientLookupError.value =
                "الرصيد المتاح (${String.format(Locale.US, "%,.2f", currentBal)} ${_selectedCurrency.value.code}) غير كافٍ لإتمام التحويل"
            triggerWarningHaptics()
            return
        }

        viewModelScope.launch {
            _isCloudLookupInProgress.value = true
            // 1. Live Cloud Lookup in Firebase Firestore when Online & Authenticated
            val cloudProfile: OfficialAccountProfile? = if (isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.lookupRecipientInCloud(cleanDigits).getOrNull()
            } else {
                null
            }

            // 2. Fallback to Local Official Directory when Offline or if account is local-first
            val finalLookupResult: Pair<OfficialAccountProfile, String>? = if (cloudProfile != null) {
                val matchedType = if (cloudProfile.accountId == cleanDigits) "ACCOUNT_ID" else "PHONE"
                cloudProfile to matchedType
            } else {
                repository.lookupRecipientByDigits(cleanDigits)
            }
            _isCloudLookupInProgress.value = false

            if (finalLookupResult == null) {
                _recipientLookupError.value = if (isNetworkOnline.value) {
                    "لم يتم العثور على مستلم مسجل بهذا الرقم في قاعدة البيانات السحابية أو المحلية"
                } else {
                    "لم يتم العثور على مستلم مسجل بهذا الرقم (وضع الأوفلاين)"
                }
                triggerWarningHaptics()
                return@launch
            }

            val (recipientProfile, matchedBy) = finalLookupResult
            if (cloudProfile != null) {
                repository.upsertOfficialAccount(recipientProfile)
            }

            val isOnlineCloudReady = isNetworkOnline.value && _firebaseUser.value != null
            _recipientLookupError.value = null
            _invoiceStoreName.value = recipientProfile.fullQuadName
            _pendingTransferVerification.value = PendingTransferVerification(
                recipientAccount = recipientProfile,
                enteredDestinationDigits = cleanDigits,
                matchedBy = matchedBy,
                amount = amountVal,
                formattedAmount = String.format(Locale.US, "%,.2f", amountVal),
                currency = _selectedCurrency.value,
                note = if (isOnlineCloudReady) {
                    "تحويل سحابي فوري (Atomic Cloud Transaction) إلى ${recipientProfile.fullQuadName}"
                } else {
                    "تحويل أوفلاين عبر الموجات فوق الصوتية (Ultrasonic) + مزامنة تلقائية عند عودة الإنترنت"
                },
                isOnlineAvailable = isOnlineCloudReady
            )
        }
    }

    /**
     * Cancel Path (مسار الإلغاء):
     * Immediately cancels the operation, closes the verification modal, and returns to the
     * same transfer screen with all entered data intact.
     */
    fun cancelRecipientVerificationModal() {
        _pendingTransferVerification.value = null
    }

    /**
     * Confirm Path (مسار الإتمام والتأكيد الأمني):
     * Closes the verification modal and prompts the user for their Secret PIN or
     * Transaction Biometric for final confirmation.
     */
    fun confirmRecipientAndRequestSecurityAuth() {
        val verification = _pendingTransferVerification.value ?: return
        _pendingTransferVerification.value = null

        val actionTitle =
            "تحويل ${verification.formattedAmount} ${verification.currency.code} إلى ${verification.recipientAccount.fullQuadName}"
        requestProtectedSonicAction(actionTitle) {
            executeOfficialHybridTransfer(verification)
        }
    }

    /**
     * Instant Execution (Online Cloud/Local Sync + Automated Ultrasonic Broadcast):
     * - Automatically generates a fresh unique random OTT_ID and dynamic encryption Salt.
     * - Deducts the amount immediately from the sender's balance.
     * - Credits the amount immediately to the recipient's account balance.
     * - Broadcasts the encrypted ultrasonic pulse and opens the Digital Transfer Voucher.
     */
    private fun executeOfficialHybridTransfer(verification: PendingTransferVerification) {
        if (!verifyHardwareHmacOrFreeze()) return

        val currency = verification.currency
        val amountVal = verification.amount
        val recipient = verification.recipientAccount

        // 1. Automatically generate a fresh unique OTT_ID and random Salt in background
        var autoOttId: String
        do {
            autoOttId = MfskProtocol.generateOttId()
        } while (localStorage.getBurnedToken(autoOttId) != null)

        _invoiceOttId.value = autoOttId
        _previewSalt.value = Random.nextInt(1, 256)
        _invoiceStoreName.value = recipient.fullQuadName

        // 2. Deduct amount immediately from Sender's balance and re-sign with Keystore HMAC-SHA256
        val senderOldBalance = localStorage.getWalletBalance(currency)
        val senderNewBalance = (senderOldBalance - amountVal).coerceAtLeast(0.0)
        localStorage.setWalletBalance(senderNewBalance, currency)
        _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()

        // Record burned OTT token so the same pulse cannot be replayed
        val nowMs = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("HH:mm:ss • yyyy/MM/dd", Locale.getDefault())
            .format(Date(nowMs))
        localStorage.recordBurnedToken(
            BurnedTokenInfo(
                ottId = autoOttId,
                operationTypeAr = "تحويل رسمي إلى ${recipient.firstName} ${recipient.familyName}",
                amount = "${verification.formattedAmount} ${currency.code}",
                recordedTime = formattedTime,
                timestamp = nowMs
            )
        )
        _burnedTokensList.value = localStorage.getAllBurnedTokens()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

        // 3. Authoritative Atomic Online Cloud Transaction (خصم وإضافة فورية في السحابة المركزية)
        val isOnlineCloudActive = isNetworkOnline.value && _firebaseUser.value != null
        viewModelScope.launch {
            val senderAcc = _activeAccount.value
            if (senderAcc != null) {
                val updatedSender = repository.creditOrDebitAccountBalance(
                    accountId = senderAcc.accountId,
                    currency = currency,
                    deltaAmount = -amountVal
                )
                if (updatedSender != null) {
                    _activeAccount.value = updatedSender
                }
            }
            repository.creditOrDebitAccountBalance(
                accountId = recipient.accountId,
                currency = currency,
                deltaAmount = +amountVal
            )

            if (isOnlineCloudActive && senderAcc != null) {
                val transferDocId = "TX_${nowMs}_${autoOttId}"
                val atomicResult = cloudRepository.executeAtomicOnlineTransferDetailed(
                    transferId = transferDocId,
                    ottId = autoOttId,
                    senderAccount = senderAcc,
                    recipientAccount = recipient,
                    amount = amountVal,
                    currency = currency,
                    syncChannel = "ONLINE_CLOUD_ATOMIC"
                )
                atomicResult.onSuccess { outcome ->
                    processedCloudTransferIds.add(outcome.transferDoc.transferId)
                    // Enforce Cloud-Authoritative Balances in Hardware Keystore Cache
                    localStorage.syncAuthoritativeBalancesFromCloud(
                        balanceYer = outcome.authoritativeSenderYer,
                        balanceSar = outcome.authoritativeSenderSar,
                        balanceUsd = outcome.authoritativeSenderUsd
                    )
                    _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
                    _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
                    _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

                    val syncedSender = senderAcc.copy(
                        balanceYer = outcome.authoritativeSenderYer,
                        balanceSar = outcome.authoritativeSenderSar,
                        balanceUsd = outcome.authoritativeSenderUsd
                    )
                    repository.upsertOfficialAccount(syncedSender)
                    _activeAccount.value = syncedSender

                    _cloudSyncStatusAr.value =
                        "تمت العملية السحابية المشفرة (#${outcome.transferDoc.ottId}) وتحديث رصيد الطرفين لحظياً ✓"
                }.onFailure { err ->
                    _pendingOfflineSyncCount.value += 1
                    _cloudSyncStatusAr.value =
                        "تم تسجيل العملية محلياً بانتظار المزامنة السحابية (${err.localizedMessage ?: "Cloud Retry"})"
                }
            } else {
                _pendingOfflineSyncCount.value += 1
                _cloudSyncStatusAr.value =
                    "عملية مشفرة مسجلة محلياً • ستتم مزامنتها سحابياً فور اتصال الإنترنت (${_pendingOfflineSyncCount.value})"
            }
        }

        // 4. Build and Broadcast Encrypted Ultrasonic Pulse automatically
        val microPacketText = MfskProtocol.buildMicroPacket(
            opType = MicroOpType.PAYMENT,
            amount = String.format(Locale.US, "%.2f", amountVal),
            ottId = autoOttId,
            storeName = recipient.fullQuadName,
            currency = currency.code
        )
        val packet = MfskProtocol.encodePayload(
            text = microPacketText,
            mode = _acousticMode.value,
            encrypt = true,
            secretPin = XorStreamCipher.DEFAULT_FAST_PIN,
            explicitSalt = _previewSalt.value
        )
        _encodedPreview.value = packet

        syncEngineSettings()
        transceiverEngine.playFinancialSuccessChime()
        triggerSuccessPaymentHaptics()

        val operationBadgeAr = if (isOnlineCloudActive) {
            "تحويل سحابي ذري فوري (Atomic Cloud + Ultrasonic)"
        } else {
            "تحويل صوتي أوفلاين (Offline Ultrasonic • بانتظار المزامنة)"
        }

        val senderProfile = _activeAccount.value
        val generatedRefId = "VP-REF-$autoOttId-${((nowMs / 1000L) % 1000000L).toString().padStart(6, '0')}"

        val voucherRecord = TransferRecord(
            direction = "SENT",
            payloadType = "INVOICE",
            title = "تحويل إلى: ${recipient.fullQuadName} (ID: ${recipient.accountId})",
            rawContent = packet.originalText,
            hexPayload = packet.fullHexWithChecksum,
            checksumHex = packet.checksumHex,
            isChecksumValid = true,
            isEncrypted = true,
            saltHex = packet.saltHex ?: "--",
            ottId = autoOttId,
            amount = "${verification.formattedAmount} ${currency.code}",
            operationTypeAr = operationBadgeAr,
            acousticMode = _acousticMode.value.name,
            byteCount = packet.transmittedBytes.size,
            toneCount = packet.scheduledTones.size,
            durationMs = packet.totalDurationMs,
            timestamp = nowMs,
            senderName = senderProfile?.fullQuadName ?: "أحمد محمد عبدالله الشامي",
            senderPhone = senderProfile?.formattedInternationalPhone ?: "+967 771234567",
            recipientName = recipient.fullQuadName,
            recipientPhone = "${recipient.formattedInternationalPhone} (ID: ${recipient.accountId})",
            referenceId = generatedRefId
        )

        viewModelScope.launch {
            repository.insert(voucherRecord)
        }

        // Record Financial Notification in In-App Notification Center
        pushInAppNotification(
            category = NotificationCategory.FINANCIAL,
            titleAr = "تم إرسال حوالة مالية ناجحة ↗",
            messageAr = "تم تحويل مبلغ ${verification.formattedAmount} ${currency.code} إلى المستلم (${recipient.fullQuadName}) بنجاح.",
            amountBadge = "-${verification.formattedAmount} ${currency.code}",
            counterpartyName = recipient.fullQuadName,
            counterpartyPhone = recipient.formattedInternationalPhone,
            referenceId = generatedRefId
        )

        // Show official Digital Receipt Modal immediately and broadcast ultrasonic wave
        _selectedReceiptRecord.value = voucherRecord
        _lastTopUpBanner.value =
            "تم تحويل ${verification.formattedAmount} ${currency.code} إلى ${recipient.fullQuadName} بنجاح ✓"

        transceiverEngine.transmitPacket(packet)
    }

    /**
     * Verifies the Android Keystore HMAC-SHA256 mathematical signature of the local wallet and
     * burned tokens. If any local tampering is detected, immediately freezes the app!
     */
    fun verifyHardwareHmacOrFreeze(): Boolean {
        val verification = localStorage.verifyLedgerHmacIntegrity()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
        if (verification is TamperVerificationResult.TamperDetected) {
            stopListening()
            stopTransmitting()
            _pendingPinProtectedAction.value = null
            _pendingBuyerApproval.value = null
            _pendingTransferVerification.value = null
            transceiverEngine.playSecurityErrorBuzzer()
            triggerWarningHaptics()
            return false
        }
        return true
    }

    fun simulateRootTamperAttack() {
        localStorage.simulateUnauthorizedLocalTamperAttack(_selectedCurrency.value)
        verifyHardwareHmacOrFreeze()
    }

    fun selectWalletCurrency(currency: WalletCurrency) {
        if (!verifyHardwareHmacOrFreeze()) return
        localStorage.setSelectedCurrency(currency)
        _selectedCurrency.value = currency
        _walletBalance.value = localStorage.getWalletBalance(currency)
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
        _invoiceCurrency.value = currency.code
        _lastTopUpBanner.value = null
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
        refreshEncodedPreview()
    }

    fun topUpWalletBalance(amountToAdd: Double) {
        if (amountToAdd <= 0.0) return
        if (!verifyHardwareHmacOrFreeze()) return
        val currency = _selectedCurrency.value
        val current = localStorage.getWalletBalance(currency)
        val updated = current + amountToAdd
        localStorage.setWalletBalance(updated, currency)
        _walletBalance.value = updated
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

        viewModelScope.launch {
            _activeAccount.value?.let { acc ->
                val updatedAcc = repository.creditOrDebitAccountBalance(acc.accountId, currency, amountToAdd)
                if (updatedAcc != null) {
                    _activeAccount.value = updatedAcc
                    if (isNetworkOnline.value && _firebaseUser.value != null) {
                        cloudRepository.executeAtomicCloudDeposit(updatedAcc, currency, amountToAdd)
                            .onSuccess { authoritativeProfile ->
                                repository.upsertOfficialAccount(authoritativeProfile)
                                _activeAccount.value = authoritativeProfile
                                localStorage.syncAuthoritativeBalancesFromCloud(
                                    balanceYer = authoritativeProfile.balanceYer,
                                    balanceSar = authoritativeProfile.balanceSar,
                                    balanceUsd = authoritativeProfile.balanceUsd
                                )
                                _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
                                _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
                                _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
                                _cloudSyncStatusAr.value =
                                    "تم توثيق الإيداع في السحابة المشفرة (${authoritativeProfile.accountId}) ✓"
                            }
                    } else {
                        _pendingOfflineSyncCount.value += 1
                    }
                }
            }
        }

        val nowMs = System.currentTimeMillis()
        val depOtt = MfskProtocol.generateOttId()
        val depRefId = "VP-DEP-$depOtt-${((nowMs / 1000L) % 1000000L).toString().padStart(6, '0')}"
        val formattedDeposit = String.format(Locale.US, "%,.2f", amountToAdd)
        val activeAcc = _activeAccount.value

        val depositRecord = TransferRecord(
            direction = "RECEIVED",
            payloadType = "INVOICE",
            title = "إيداع سحابي مباشر في المحفظة (#$depOtt)",
            rawContent = "DEP|$formattedDeposit|$depOtt",
            hexPayload = "444550$depOtt",
            checksumHex = "A8",
            isChecksumValid = true,
            isEncrypted = true,
            saltHex = "01",
            ottId = depOtt,
            amount = "$formattedDeposit ${currency.code}",
            operationTypeAr = "إيداع مالي سحابي مباشر (Cloud Deposit)",
            acousticMode = _acousticMode.value.name,
            byteCount = 16,
            toneCount = 32,
            durationMs = 1120,
            timestamp = nowMs,
            senderName = "خزينة الإيداع السحابي الرسمي (VibraPay Treasury)",
            senderPhone = "Cloud Gateway • #VP-TREASURY",
            recipientName = activeAcc?.fullQuadName ?: "أحمد محمد عبدالله الشامي",
            recipientPhone = activeAcc?.let { "${it.formattedInternationalPhone} (ID: ${it.accountId})" } ?: "+967 771234567",
            referenceId = depRefId
        )

        viewModelScope.launch {
            repository.insert(depositRecord)
        }

        pushInAppNotification(
            category = NotificationCategory.FINANCIAL,
            titleAr = "تم إيداع رصيد مالي في محفظتك 💰",
            messageAr = "تمت إضافة مبلغ $formattedDeposit ${currency.code} إلى رصيد محفظتك السحابية وتوثيق العملية بنجاح.",
            amountBadge = "+$formattedDeposit ${currency.code}",
            counterpartyName = "خزينة VibraPay السحابية",
            counterpartyPhone = "Cloud Deposit",
            referenceId = depRefId
        )

        _selectedReceiptRecord.value = depositRecord
        _lastTopUpBanner.value =
            "تم إيداع +${String.format(Locale.US, "%,.0f", amountToAdd)} ${currency.code} في الرصيد السحابي بنجاح ✓"
        transceiverEngine.playFinancialSuccessChime()
        triggerSuccessPaymentHaptics()
    }

    fun setWalletPinLockEnabled(enabled: Boolean) {
        if (!verifyHardwareHmacOrFreeze()) return
        localStorage.setWalletPinEnabled(enabled)
        _isWalletPinEnabled.value = enabled
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
    }

    fun openWalletPinSetupDialog() {
        if (!verifyHardwareHmacOrFreeze()) return
        _showPinSetupDialog.value = true
    }

    fun closeWalletPinSetupDialog() {
        _showPinSetupDialog.value = false
    }

    fun saveWalletPin(newPin: String, enableLock: Boolean) {
        if (!verifyHardwareHmacOrFreeze()) return
        val clean = newPin.trim().filter { it.isDigit() }.ifBlank { "1234" }
        localStorage.setWalletPin(clean)
        localStorage.setWalletPinEnabled(enableLock)
        _walletPin.value = clean
        _isWalletPinEnabled.value = enableLock
        _showPinSetupDialog.value = false
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
        pushInAppNotification(
            category = NotificationCategory.SECURITY,
            titleAr = "تم تغيير رمز التأكيد (PIN Code) للمحفظة 🛡️",
            messageAr = "تم تحديث رمز التأكيد الشخصي (PIN) وتوقيعه داخل شريحة الأمان Hardware Keystore بنجاح.",
            amountBadge = "تحديث PIN",
            counterpartyName = _activeAccount.value?.fullQuadName ?: "حساب المحفظة",
            counterpartyPhone = _activeAccount.value?.formattedInternationalPhone ?: "",
            referenceId = "SEC-PIN-${System.currentTimeMillis() % 100000}"
        )
        triggerSuccessPaymentHaptics()
    }

    fun requestProtectedSonicAction(actionTitleAr: String, action: () -> Unit) {
        if (!verifyHardwareHmacOrFreeze()) return
        if (_isWalletPinEnabled.value) {
            _pinAuthError.value = null
            _pendingPinProtectedAction.value = PendingPinProtectedAction(
                titleAr = actionTitleAr,
                execute = action
            )
        } else {
            action()
        }
    }

    fun verifyWalletPinAndProceed(enteredPin: String) {
        if (!verifyHardwareHmacOrFreeze()) return
        val pending = _pendingPinProtectedAction.value ?: return
        if (enteredPin.trim() == _walletPin.value) {
            _pendingPinProtectedAction.value = null
            _pinAuthError.value = null
            pending.execute()
        } else {
            _pinAuthError.value = "رمز حماية المحفظة (PIN) غير صحيح! حاول مرة أخرى أو استخدم البصمة"
            transceiverEngine.playSecurityErrorBuzzer()
            triggerWarningHaptics()
        }
    }

    fun onBiometricAuthenticated() {
        if (!verifyHardwareHmacOrFreeze()) return
        val pending = _pendingPinProtectedAction.value ?: return
        _pendingPinProtectedAction.value = null
        _pinAuthError.value = null
        triggerSuccessPaymentHaptics()
        pending.execute()
    }

    fun onBiometricFallbackOrInfo(messageAr: String) {
        _pinAuthError.value = messageAr
    }

    fun cancelWalletPinAuth() {
        _pendingPinProtectedAction.value = null
        _pinAuthError.value = null
    }

    fun selectNavTab(tab: MainNavTab) {
        if (tab != MainNavTab.RADAR_TRANSCEIVER && isListening.value) {
            stopListening()
        }
        _selectedNavTab.value = tab
    }

    fun selectWorkspaceMode(mode: TransceiverWorkspaceMode) {
        _workspaceMode.value = mode
        if (mode == TransceiverWorkspaceMode.SEND && isListening.value) {
            stopListening()
        }
    }

    fun onAppBackgrounded() {
        _isAppForegroundActive.value = false
        autoTimeoutJob?.cancel()
        autoTimeoutJob = null
        _listeningRemainingSeconds.value = 0
        transceiverEngine.stopListening(ReceiverStatus.BACKGROUND_KILLED)
        stopTransmitting()
    }

    fun onAppForegroundResumed() {
        _isAppForegroundActive.value = true
        verifyHardwareHmacOrFreeze()
    }

    fun openReceiptModal(record: TransferRecord) {
        _selectedReceiptRecord.value = record
    }

    fun closeReceiptModal() {
        _selectedReceiptRecord.value = null
    }

    fun dismissBuyerApprovalModal() {
        _pendingBuyerApproval.value = null
    }

    fun setAcousticMode(mode: AcousticMode) {
        _acousticMode.value = mode
        transceiverEngine.currentMode = mode
        refreshEncodedPreview()
    }

    fun setNfcProximityMode(enabled: Boolean) {
        _nfcProximityMode.value = enabled
        transceiverEngine.nfcProximityMode = enabled
        val newThreshold = if (enabled) {
            MfskProtocol.NFC_PROXIMITY_THRESHOLD
        } else {
            MfskProtocol.DEFAULT_THRESHOLD
        }
        _threshold.value = newThreshold
        transceiverEngine.threshold = newThreshold
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        _isEncryptionEnabled.value = true
        _previewSalt.value = Random.nextInt(1, 256)
        syncEngineSettings()
        refreshEncodedPreview()
    }

    fun updateSecretPin(pin: String) {
        _secretPin.value = XorStreamCipher.DEFAULT_FAST_PIN
        syncEngineSettings()
        refreshEncodedPreview()
    }

    fun regenerateSalt() {
        _previewSalt.value = Random.nextInt(1, 256)
        refreshEncodedPreview()
    }

    private fun syncEngineSettings() {
        transceiverEngine.nfcProximityMode = _nfcProximityMode.value
        transceiverEngine.threshold = _threshold.value
        transceiverEngine.encryptionEnabled = true
        transceiverEngine.receiverSecretPin = XorStreamCipher.DEFAULT_FAST_PIN
    }

    fun setThreshold(newThreshold: Int) {
        val minAllowed = if (_nfcProximityMode.value) MfskProtocol.NFC_PROXIMITY_THRESHOLD else 45
        val clamped = newThreshold.coerceIn(minAllowed, 240)
        _threshold.value = clamped
        transceiverEngine.threshold = clamped
    }

    fun setLoopbackSelfTest(enabled: Boolean) {
        _loopbackSelfTestEnabled.value = enabled
        transceiverEngine.loopbackBridgeEnabled = enabled
    }

    fun setHighPassFilterEnabled(enabled: Boolean) {
        _highPassFilterEnabled.value = enabled
        transceiverEngine.highPassFilterEnabled = enabled
    }

    fun setSmartAutoRetryEnabled(enabled: Boolean) {
        _smartAutoRetryEnabled.value = enabled
        transceiverEngine.smartAutoRetryEnabled = enabled
    }

    fun selectTemplate(tab: TemplateTab) {
        _selectedTemplate.value = tab
        refreshEncodedPreview()
    }

    fun selectMicroOpType(opType: MicroOpType) {
        _microOpType.value = opType
        refreshEncodedPreview()
    }

    fun updateFreeText(text: String) {
        _freeTextInput.value = text
        refreshEncodedPreview()
    }

    fun updateInvoiceFields(
        storeName: String = _invoiceStoreName.value,
        amount: String = _invoiceAmount.value,
        currency: String = _invoiceCurrency.value,
        ottId: String = _invoiceOttId.value
    ) {
        _invoiceStoreName.value = storeName
        _invoiceAmount.value = amount
        _invoiceCurrency.value = currency
        _invoiceOttId.value = ottId.uppercase().take(4)
        refreshEncodedPreview()
    }

    fun generateNewOttId() {
        _invoiceOttId.value = MfskProtocol.generateOttId()
        refreshEncodedPreview()
    }

    fun updateContactFields(
        name: String = _contactName.value,
        phone: String = _contactPhone.value,
        org: String = _contactOrg.value
    ) {
        _contactName.value = name
        _contactPhone.value = phone
        _contactOrg.value = org
        refreshEncodedPreview()
    }

    fun getActivePayloadString(): String {
        return when (_selectedTemplate.value) {
            TemplateTab.FREE_TEXT -> _freeTextInput.value.ifBlank { "مرحبا" }
            TemplateTab.QUICK_INVOICE -> MfskProtocol.buildMicroPacket(
                opType = _microOpType.value,
                amount = _invoiceAmount.value.ifBlank { "0.00" },
                ottId = _invoiceOttId.value.ifBlank { "A1B2" },
                storeName = _invoiceStoreName.value,
                currency = _selectedCurrency.value.code
            )
            TemplateTab.CONTACT_CARD -> MfskProtocol.buildContactJson(
                fullName = _contactName.value.ifBlank { "مستخدم VibraPay" },
                phone = _contactPhone.value.ifBlank { "0500000000" },
                organization = _contactOrg.value.ifBlank { "VibraPay" }
            )
        }
    }

    private fun refreshEncodedPreview() {
        val payload = getActivePayloadString()
        _encodedPreview.value = MfskProtocol.encodePayload(
            text = payload,
            mode = _acousticMode.value,
            encrypt = true,
            secretPin = XorStreamCipher.DEFAULT_FAST_PIN,
            explicitSalt = _previewSalt.value
        )
    }

    fun onPermissionGrantedAndStartListening() {
        _isAppForegroundActive.value = true
        _selectedNavTab.value = MainNavTab.RADAR_TRANSCEIVER
        _workspaceMode.value = TransceiverWorkspaceMode.RECEIVE
        startListening(ReceiverStatus.LISTENING)
    }

    fun startListening(initialStatus: ReceiverStatus = ReceiverStatus.LISTENING) {
        if (!verifyHardwareHmacOrFreeze()) return
        _isAppForegroundActive.value = true
        if (_selectedNavTab.value != MainNavTab.RADAR_TRANSCEIVER) {
            _selectedNavTab.value = MainNavTab.RADAR_TRANSCEIVER
        }
        _workspaceMode.value = TransceiverWorkspaceMode.RECEIVE
        syncEngineSettings()
        vibratePattern(longArrayOf(0L, 40L))
        transceiverEngine.startListening(initialStatus)
        startTwentySecondAutoTimeout()
    }

    private fun startTwentySecondAutoTimeout() {
        autoTimeoutJob?.cancel()
        _listeningRemainingSeconds.value = MfskProtocol.AUTO_LISTEN_TIMEOUT_SECONDS
        autoTimeoutJob = viewModelScope.launch {
            var remaining = MfskProtocol.AUTO_LISTEN_TIMEOUT_SECONDS
            while (remaining > 0 && isListening.value) {
                delay(1000L)
                remaining--
                _listeningRemainingSeconds.value = remaining
            }
            if (remaining <= 0 && isListening.value) {
                transceiverEngine.stopListening(ReceiverStatus.AUTO_TIMEOUT_CLOSED)
                _listeningRemainingSeconds.value = 0
            }
        }
    }

    fun stopListening() {
        autoTimeoutJob?.cancel()
        autoTimeoutJob = null
        _listeningRemainingSeconds.value = 0
        transceiverEngine.stopListening(ReceiverStatus.IDLE)
    }

    fun clearLastDecodedPacket() {
        _lastDecodedPacket.value = null
        transceiverEngine.resetReceiverPacketState()
    }

    fun simulateCrowdIntruderAttack() {
        if (!verifyHardwareHmacOrFreeze()) return
    }

    fun transmitCurrentPacket(
        enableSelfRadarIfIdle: Boolean = false,
        simulateWrongKeyOnReceiver: Boolean = false,
        keepSameOttForReplayTest: Boolean = false
    ) {
        val actionTitle =
            "تأكيد بث ${_microOpType.value.labelAr} بقيمة ${_invoiceAmount.value} ${_selectedCurrency.value.code}"
        requestProtectedSonicAction(actionTitle) {
            executeTransmitCurrentPacket()
        }
    }

    private fun executeTransmitCurrentPacket() {
        if (!verifyHardwareHmacOrFreeze()) return
        transceiverEngine.simulateFarCrowdSignalInLoopback = false

        // Always auto-generate a fresh unique OTT_ID and random Salt in background!
        var freshOtt: String
        do {
            freshOtt = MfskProtocol.generateOttId()
        } while (localStorage.getBurnedToken(freshOtt) != null)
        _invoiceOttId.value = freshOtt
        _previewSalt.value = Random.nextInt(1, 256)

        refreshEncodedPreview()
        val packet = _encodedPreview.value
        val mode = _acousticMode.value
        val isSellerReq = _selectedTemplate.value == TemplateTab.QUICK_INVOICE &&
            _microOpType.value == MicroOpType.SELLER_REQUEST

        syncEngineSettings()
        vibratePattern(longArrayOf(0L, 55L, 70L, 55L, 70L, 55L))

        val meta = extractRecordMeta(packet.originalText)

        viewModelScope.launch {
            repository.insert(
                TransferRecord(
                    direction = "SENT",
                    payloadType = meta.payloadType,
                    title = meta.title,
                    rawContent = packet.originalText,
                    hexPayload = packet.fullHexWithChecksum,
                    checksumHex = packet.checksumHex,
                    isChecksumValid = true,
                    isEncrypted = packet.isEncrypted,
                    saltHex = packet.saltHex ?: "--",
                    ottId = meta.ottId,
                    amount = meta.amount,
                    operationTypeAr = meta.operationTypeAr,
                    acousticMode = mode.name,
                    byteCount = packet.transmittedBytes.size,
                    toneCount = packet.scheduledTones.size,
                    durationMs = packet.totalDurationMs
                )
            )
        }

        transceiverEngine.transmitPacket(packet) {
            if (isSellerReq) {
                _workspaceMode.value = TransceiverWorkspaceMode.RECEIVE
                startListening(ReceiverStatus.SELLER_WAITING_PAY)
            }
        }
    }

    fun approveBuyerPaymentRequest(requestInvoice: StructuredPayload.Invoice) {
        val actionTitle =
            "موافقة ودفع مبلغ ${requestInvoice.amount} ${requestInvoice.currency} (#${requestInvoice.ottId})"
        requestProtectedSonicAction(actionTitle) {
            executeBuyerPaymentApproval(requestInvoice)
        }
    }

    private fun executeBuyerPaymentApproval(requestInvoice: StructuredPayload.Invoice) {
        if (!verifyHardwareHmacOrFreeze()) return
        _pendingBuyerApproval.value = null
        val amountVal = requestInvoice.amount.toDoubleOrNull() ?: 0.0
        val ottId = requestInvoice.ottId.trim().uppercase()
        val invoiceWalletCurrency = WalletCurrency.fromCode(
            if (requestInvoice.currency in listOf("YER", "SAR", "USD")) {
                requestInvoice.currency
            } else {
                _selectedCurrency.value.code
            }
        )

        val existingBurned = localStorage.getBurnedToken(ottId)
        if (existingBurned != null) {
            val replayAlert =
                "هذه العملية صالحة لمرة واحدة فقط وتم استخدامها مسبقاً! (تم ${existingBurned.operationTypeAr} بقيمة ${existingBurned.amount} في ${existingBurned.recordedTime})"
            _lastDecodedPacket.value = _lastDecodedPacket.value?.copy(
                isReplayBurnedError = true,
                securityAlertMessage = replayAlert
            )
            transceiverEngine.playSecurityErrorBuzzer()
            triggerWarningHaptics()
            return
        }

        val currentCurrBal = localStorage.getWalletBalance(invoiceWalletCurrency)
        val buyerNewBalance = (currentCurrBal - amountVal).coerceAtLeast(0.0)
        localStorage.setWalletBalance(buyerNewBalance, invoiceWalletCurrency)
        _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

        val payMicroPacket = MfskProtocol.buildMicroPacket(
            opType = MicroOpType.BUYER_CONFIRM_PAY,
            amount = requestInvoice.amount,
            ottId = ottId,
            storeName = requestInvoice.storeName,
            currency = invoiceWalletCurrency.code
        )
        val payPacket = MfskProtocol.encodePayload(
            text = payMicroPacket,
            mode = _acousticMode.value,
            encrypt = true,
            secretPin = XorStreamCipher.DEFAULT_FAST_PIN
        )

        if (!_loopbackSelfTestEnabled.value) {
            val nowMs = System.currentTimeMillis()
            val formattedTime = SimpleDateFormat("HH:mm:ss • yyyy/MM/dd", Locale.getDefault())
                .format(Date(nowMs))
            localStorage.recordBurnedToken(
                BurnedTokenInfo(
                    ottId = ottId,
                    operationTypeAr = "دفع للمتجر",
                    amount = "${requestInvoice.amount} ${invoiceWalletCurrency.code}",
                    recordedTime = formattedTime,
                    timestamp = nowMs
                )
            )
            _burnedTokensList.value = localStorage.getAllBurnedTokens()
            _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
            transceiverEngine.playFinancialSuccessChime()
            triggerSuccessPaymentHaptics()
        } else {
            startListening(ReceiverStatus.SELLER_WAITING_PAY)
        }

        val meta = extractRecordMeta(payPacket.originalText)
        val nowPayMs = System.currentTimeMillis()
        val payRefId = "VP-PAY-$ottId-${((nowPayMs / 1000L) % 1000000L).toString().padStart(6, '0')}"
        val buyerAcc = _activeAccount.value
        val buyerRecord = TransferRecord(
            direction = "SENT",
            payloadType = meta.payloadType,
            title = "تأكيد دفع مشتري (PAY): ${requestInvoice.amount} ${invoiceWalletCurrency.code} (#$ottId)",
            rawContent = payPacket.originalText,
            hexPayload = payPacket.fullHexWithChecksum,
            checksumHex = payPacket.checksumHex,
            isChecksumValid = true,
            isEncrypted = payPacket.isEncrypted,
            saltHex = payPacket.saltHex ?: "--",
            ottId = ottId,
            amount = "${requestInvoice.amount} ${invoiceWalletCurrency.code}",
            operationTypeAr = "موافقة ودفع مشتري",
            acousticMode = _acousticMode.value.name,
            byteCount = payPacket.transmittedBytes.size,
            toneCount = payPacket.scheduledTones.size,
            durationMs = payPacket.totalDurationMs,
            timestamp = nowPayMs,
            senderName = buyerAcc?.fullQuadName ?: "أحمد محمد عبدالله الشامي",
            senderPhone = buyerAcc?.formattedInternationalPhone ?: "+967 771234567",
            recipientName = requestInvoice.storeName.ifBlank { "متجر معتمد في VibraPay" },
            recipientPhone = "Merchant OTT #$ottId",
            referenceId = payRefId
        )

        viewModelScope.launch {
            repository.insert(buyerRecord)
        }

        pushInAppNotification(
            category = NotificationCategory.FINANCIAL,
            titleAr = "تم سداد فاتورة مشتري بنجاح ✅",
            messageAr = "تم خصم وسداد مبلغ ${requestInvoice.amount} ${invoiceWalletCurrency.code} لصالح (${requestInvoice.storeName}).",
            amountBadge = "-${requestInvoice.amount} ${invoiceWalletCurrency.code}",
            counterpartyName = requestInvoice.storeName,
            counterpartyPhone = "OTT #$ottId",
            referenceId = payRefId
        )

        _selectedReceiptRecord.value = buyerRecord
        transceiverEngine.transmitPacket(payPacket)
    }

    fun retransmitRecord(record: TransferRecord) {
        requestProtectedSonicAction("إعادة بث العملية (#${record.ottId})") {
            val mode = try {
                AcousticMode.valueOf(record.acousticMode)
            } catch (_: Exception) {
                _acousticMode.value
            }
            setAcousticMode(mode)
            val packet = MfskProtocol.encodePayload(
                text = record.rawContent,
                mode = mode,
                encrypt = true,
                secretPin = XorStreamCipher.DEFAULT_FAST_PIN
            )
            _selectedNavTab.value = MainNavTab.RADAR_TRANSCEIVER
            if (!isListening.value) {
                startListening()
            }
            transceiverEngine.transmitPacket(packet)
        }
    }

    fun stopTransmitting() {
        transceiverEngine.stopTransmitting()
        syncEngineSettings()
    }

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun resetWalletAndBurnedTokens() {
        localStorage.resetAllWalletBalances()
        localStorage.clearBurnedTokens()
        _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
        _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
        _burnedTokensList.value = emptyList()
        _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
        _lastTopUpBanner.value = "تمت استعادة الأرصدة الموثقة من السحابة وتوليد توقيع Keystore HMAC-SHA256 جديد ✓"

        // Also fetch authoritative cloud balance if online & signed in
        viewModelScope.launch {
            val active = _activeAccount.value
            if (active != null && isNetworkOnline.value && _firebaseUser.value != null) {
                cloudRepository.fetchAuthoritativeCloudAccount(active.accountId).getOrNull()?.let { cloudAcc ->
                    repository.upsertOfficialAccount(cloudAcc)
                    _activeAccount.value = cloudAcc
                    localStorage.syncAuthoritativeBalancesFromCloud(
                        balanceYer = cloudAcc.balanceYer,
                        balanceSar = cloudAcc.balanceSar,
                        balanceUsd = cloudAcc.balanceUsd
                    )
                    _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
                    _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
                    _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()
                }
            }
        }
    }

    private fun handlePacketDecoded(decoded: DecodedPacketResult, mode: AcousticMode) {
        autoTimeoutJob?.cancel()
        _listeningRemainingSeconds.value = 0

        if (!verifyHardwareHmacOrFreeze()) return

        if (decoded.isKeyMismatch) {
            _lastDecodedPacket.value = decoded
            transceiverEngine.playSecurityErrorBuzzer()
            triggerWarningHaptics()
            saveDecodedRecordToHistory(decoded, mode)
            return
        }

        val structured = decoded.structuredPayload
        if (decoded.isChecksumValid && structured is StructuredPayload.Invoice) {
            val ottId = structured.ottId.trim().uppercase()
            val existingBurned = localStorage.getBurnedToken(ottId)

            if (existingBurned != null) {
                val replayAlert =
                    "هذه العملية صالحة لمرة واحدة فقط وتم استخدامها مسبقاً! (تم ${existingBurned.operationTypeAr} هذه الدفعة مسبقاً بقيمة ${existingBurned.amount} في وقت ${existingBurned.recordedTime})"
                val rejectedResult = decoded.copy(
                    isReplayBurnedError = true,
                    securityAlertMessage = replayAlert
                )
                _lastDecodedPacket.value = rejectedResult
                transceiverEngine.playSecurityErrorBuzzer()
                triggerWarningHaptics()
                saveDecodedRecordToHistory(rejectedResult, mode)
                return
            }

            if (structured.opType == MicroOpType.SELLER_REQUEST) {
                _lastDecodedPacket.value = decoded
                _pendingBuyerApproval.value = structured
                triggerSuccessPaymentHaptics()
                saveDecodedRecordToHistory(decoded, mode)
                return
            }

            val nowMs = System.currentTimeMillis()
            val formattedTime = SimpleDateFormat("HH:mm:ss • yyyy/MM/dd", Locale.getDefault())
                .format(Date(nowMs))
            val amountVal = structured.amount.toDoubleOrNull() ?: 0.0
            val targetCurrency = WalletCurrency.fromCode(
                if (structured.currency in listOf("YER", "SAR", "USD")) {
                    structured.currency
                } else {
                    _selectedCurrency.value.code
                }
            )
            val amountFormatted = "${structured.amount} ${targetCurrency.code}"

            val burnedInfo = BurnedTokenInfo(
                ottId = ottId,
                operationTypeAr = structured.opType.actionVerbAr,
                amount = amountFormatted,
                recordedTime = formattedTime,
                timestamp = nowMs
            )
            localStorage.recordBurnedToken(burnedInfo)
            _burnedTokensList.value = localStorage.getAllBurnedTokens()

            val currentBal = localStorage.getWalletBalance(targetCurrency)
            val updatedBal = when (structured.opType) {
                MicroOpType.PAYMENT, MicroOpType.BUYER_CONFIRM_PAY, MicroOpType.COLLECTION, MicroOpType.REFUND ->
                    currentBal + amountVal
                MicroOpType.SELLER_REQUEST -> currentBal
            }
            localStorage.setWalletBalance(updatedBal, targetCurrency)
            _walletBalance.value = localStorage.getWalletBalance(_selectedCurrency.value)
            _allCurrencyBalances.value = localStorage.getAllCurrencyBalances()
            _hardwareSecurityStatus.value = localStorage.getHardwareSecurityStatus()

            // Smart Hybrid Sync: Update active account balance in Room + sync with Cloud if online
            viewModelScope.launch {
                val delta = updatedBal - currentBal
                if (delta != 0.0) {
                    _activeAccount.value?.let { activeAcc ->
                        val updatedProfile = repository.creditOrDebitAccountBalance(
                            accountId = activeAcc.accountId,
                            currency = targetCurrency,
                            deltaAmount = delta
                        )
                        if (updatedProfile != null) {
                            _activeAccount.value = updatedProfile
                            if (isNetworkOnline.value && _firebaseUser.value != null) {
                                cloudRepository.registerOrSyncCloudAccount(updatedProfile)
                                _cloudSyncStatusAr.value =
                                    "تمت مزامنة الدفعة الصوتية المستلمة (#$ottId) مع السحابة تلقائياً ✓"
                            } else {
                                _pendingOfflineSyncCount.value += 1
                                _cloudSyncStatusAr.value =
                                    "تم استلام دفعة صوتية أوفلاين (#$ottId) • ستتم المزامنة فور توفر الإنترنت"
                            }
                        }
                    }
                }
            }

            _lastDecodedPacket.value = decoded
            transceiverEngine.playFinancialSuccessChime()
            triggerSuccessPaymentHaptics()
            val savedRecord = saveDecodedRecordToHistory(decoded, mode)
            pushInAppNotification(
                category = NotificationCategory.FINANCIAL,
                titleAr = "استلام دفعة صوتية مشفرة ناجحة 📡",
                messageAr = "تم استلام وتوثيق مبلغ $amountFormatted عبر التلامس الصوتي القريب (${structured.storeName}).",
                amountBadge = "+$amountFormatted",
                counterpartyName = structured.storeName.ifBlank { "مرسل VibraPay" },
                counterpartyPhone = "OTT #$ottId",
                referenceId = savedRecord.resolvedReferenceId()
            )
            _selectedReceiptRecord.value = savedRecord
            return
        }

        _lastDecodedPacket.value = decoded
        transceiverEngine.playFinancialSuccessChime()
        triggerSuccessPaymentHaptics()
        saveDecodedRecordToHistory(decoded, mode)
    }

    private fun saveDecodedRecordToHistory(
        decoded: DecodedPacketResult,
        mode: AcousticMode
    ): TransferRecord {
        val meta = when {
            decoded.isKeyMismatch -> RecordMeta(
                payloadType = "SECURITY_ALERT",
                title = decoded.securityAlertMessage ?: XorStreamCipher.SECURITY_ALERT_KEY_MISMATCH,
                ottId = "--",
                amount = "0.00",
                operationTypeAr = "نبضة مشفرة مرفوضة"
            )
            decoded.isReplayBurnedError -> {
                val inv = decoded.structuredPayload as? StructuredPayload.Invoice
                RecordMeta(
                    payloadType = "SECURITY_ALERT",
                    title = decoded.securityAlertMessage ?: "عملية مكررة مرفوضة",
                    ottId = inv?.ottId ?: "--",
                    amount = inv?.amount ?: "0.00",
                    operationTypeAr = "مرفوض (مستخدم مسبقاً)"
                )
            }
            else -> extractRecordMeta(decoded.decodedText)
        }

        val estimatedPacket = MfskProtocol.encodePayload(
            text = decoded.decodedText.ifBlank { "?" },
            mode = mode,
            encrypt = decoded.isEncrypted
        )

        val record = TransferRecord(
            direction = "RECEIVED",
            payloadType = meta.payloadType,
            title = meta.title,
            rawContent = decoded.decodedText,
            hexPayload = decoded.rawHexReceived,
            checksumHex = decoded.receivedChecksumHex,
            isChecksumValid = decoded.isChecksumValid && !decoded.isReplayBurnedError,
            isEncrypted = decoded.isEncrypted,
            saltHex = decoded.extractedSaltHex ?: "--",
            ottId = meta.ottId,
            amount = meta.amount,
            operationTypeAr = meta.operationTypeAr,
            acousticMode = mode.name,
            byteCount = decoded.dataHex.length / 2,
            toneCount = estimatedPacket.scheduledTones.size,
            durationMs = estimatedPacket.totalDurationMs
        )

        viewModelScope.launch {
            repository.insert(record)
        }
        return record
    }

    private data class RecordMeta(
        val payloadType: String,
        val title: String,
        val ottId: String,
        val amount: String,
        val operationTypeAr: String
    )

    private fun extractRecordMeta(rawText: String): RecordMeta {
        return when (val structured = MfskProtocol.parseStructuredPayload(rawText)) {
            is StructuredPayload.Invoice -> RecordMeta(
                payloadType = "INVOICE",
                title = "${structured.opType.labelAr}: ${structured.amount} ${structured.currency} (#${structured.ottId})",
                ottId = structured.ottId,
                amount = "${structured.amount} ${structured.currency}",
                operationTypeAr = structured.opType.actionVerbAr
            )
            is StructuredPayload.ContactCard -> RecordMeta(
                payloadType = "CONTACT",
                title = "بطاقة تواصل: ${structured.fullName}",
                ottId = MfskProtocol.generateOttId(),
                amount = "0.00",
                operationTypeAr = "بطاقة تواصل"
            )
            is StructuredPayload.FreeText -> RecordMeta(
                payloadType = "TEXT",
                title = structured.text.take(40),
                ottId = MfskProtocol.generateOttId(),
                amount = "0.00",
                operationTypeAr = "رسالة نصية"
            )
        }
    }

    private fun triggerSuccessPaymentHaptics() {
        vibratePattern(longArrayOf(0L, 100L, 50L, 100L))
    }

    private fun triggerWarningHaptics() {
        vibratePattern(longArrayOf(0L, 180L, 60L, 180L, 60L, 180L))
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        } catch (_: Throwable) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealTimeCloudListeners()
        connectivityObserver.unregister()
        stopListening()
        stopTransmitting()
    }
}
