package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloud.GoogleAuthManager
import com.example.data.OfficialAccountProfile
import com.example.data.TransferRecord
import com.example.data.WalletCurrency
import com.example.protocol.MicroOpType
import com.example.security.HardwareSecurityManager
import com.example.ui.MainNavTab
import com.example.ui.TemplateTab
import com.example.ui.TransceiverWorkspaceMode
import com.example.ui.VibraPayViewModel
import kotlinx.coroutines.launch
import com.example.ui.components.AppThemeSelectorDialog
import com.example.ui.components.BiometricWithCheckIcon
import com.example.ui.components.BiometricWithLockIcon
import com.example.ui.components.BuyerPaymentApprovalModal
import com.example.ui.components.ChangeAccountPasswordDialog
import com.example.ui.components.DigitalReceiptModal
import com.example.ui.components.HardwareTamperFreezeScreen
import com.example.ui.components.InAppNotificationCenterDialog
import com.example.ui.components.LiveReceiverControlCard
import com.example.ui.components.MyPersonalQrCodeDialog
import com.example.ui.components.NewlyRegisteredAccountModal
import com.example.ui.components.OfficialAccountAuthDialog
import com.example.ui.components.QrCodeScannerDialog
import com.example.ui.components.QuickTemplatesTransmitterCard
import com.example.ui.components.RecipientVerificationModal
import com.example.ui.components.SettingsScreen
import com.example.ui.components.SpectrumRadarCard
import com.example.ui.components.TransferHistoryScreen
import com.example.ui.components.WalletManagerCard
import com.example.ui.components.WalletPinAuthDialog
import com.example.ui.components.WalletPinSetupDialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SidebarDarkBg
import com.example.ui.theme.SidebarDarkBorder
import com.example.ui.theme.SidebarDarkCard
import com.example.ui.theme.SidebarDarkItemSurface
import com.example.ui.theme.SidebarTextPrimary
import com.example.ui.theme.SidebarTextSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletBg
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletPurple
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletRoyalBlueDark
import com.example.ui.theme.WalletSkyAccent
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VibraPayViewModel = viewModel()
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(
                appThemeMode = appThemeMode,
                rtl = true
            ) {
                VibraPayApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VibraPayApp(viewModel: VibraPayViewModel = viewModel()) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasMicPermission by remember {
        mutableStateOf(
            try {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }
        )
    }

    var startListeningAfterGrant by remember { mutableStateOf(false) }
    var isBalanceVisible by rememberSaveable { mutableStateOf(true) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && startListeningAfterGrant) {
            startListeningAfterGrant = false
            viewModel.onPermissionGrantedAndStartListening()
        } else {
            startListeningAfterGrant = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var googleSignInMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            try {
                startListeningAfterGrant = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Throwable) {
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (_: Throwable) {
            }
        }
        // Attempt silent Google Sign-In if user previously authorized
        GoogleAuthManager.attemptAutoSignIn(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val screenOffReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                    viewModel.onAppBackgrounded()
                }
            }
        }
        try {
            ContextCompat.registerReceiver(
                context,
                screenOffReceiver,
                android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Throwable) {
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasMicPermission = try {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    } catch (_: Throwable) {
                        hasMicPermission
                    }
                    viewModel.onAppForegroundResumed()
                }
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    viewModel.onAppBackgrounded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                context.unregisterReceiver(screenOffReceiver)
            } catch (_: Throwable) {
            }
        }
    }

    val hardwareSecurityStatus by viewModel.hardwareSecurityStatus.collectAsStateWithLifecycle()
    val selectedNavTab by viewModel.selectedNavTab.collectAsStateWithLifecycle()
    val workspaceMode by viewModel.workspaceMode.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    val allCurrencyBalances by viewModel.allCurrencyBalances.collectAsStateWithLifecycle()
    val lastTopUpBanner by viewModel.lastTopUpBanner.collectAsStateWithLifecycle()
    val isWalletPinEnabled by viewModel.isWalletPinEnabled.collectAsStateWithLifecycle()
    val walletPin by viewModel.walletPin.collectAsStateWithLifecycle()
    val showPinSetupDialog by viewModel.showPinSetupDialog.collectAsStateWithLifecycle()
    val pendingPinProtectedAction by viewModel.pendingPinProtectedAction.collectAsStateWithLifecycle()
    val pinAuthError by viewModel.pinAuthError.collectAsStateWithLifecycle()

    // Official Account & Hybrid E-Wallet States
    val isNetworkOnline by viewModel.isNetworkOnline.collectAsStateWithLifecycle()
    val networkStatusLabelAr by viewModel.networkStatusLabelAr.collectAsStateWithLifecycle()
    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val cloudSyncStatusAr by viewModel.cloudSyncStatusAr.collectAsStateWithLifecycle()
    val pendingOfflineSyncCount by viewModel.pendingOfflineSyncCount.collectAsStateWithLifecycle()

    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val directoryAccounts by viewModel.directoryAccounts.collectAsStateWithLifecycle()
    val savedLoginIdentifier by viewModel.savedLoginIdentifier.collectAsStateWithLifecycle()
    val showAuthModal by viewModel.showAuthModal.collectAsStateWithLifecycle()
    val authModalInitialRegister by viewModel.authModalInitialRegister.collectAsStateWithLifecycle()
    val isSessionLockedForSmartLogin by viewModel.isSessionLockedForSmartLogin.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val newlyRegisteredAccount by viewModel.newlyRegisteredAccount.collectAsStateWithLifecycle()
    val isLoginBiometricEnabled by viewModel.isLoginBiometricEnabled.collectAsStateWithLifecycle()
    val isTransactionBiometricEnabled by viewModel.isTransactionBiometricEnabled.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val showThemeSelectorDialog by viewModel.showThemeSelectorDialog.collectAsStateWithLifecycle()
    val showChangePasswordDialog by viewModel.showChangePasswordDialog.collectAsStateWithLifecycle()
    val showMyQrDialog by viewModel.showMyQrDialog.collectAsStateWithLifecycle()
    val showQrScannerDialog by viewModel.showQrScannerDialog.collectAsStateWithLifecycle()
    val inAppNotifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val recipientDigitsInput by viewModel.recipientDigitsInput.collectAsStateWithLifecycle()
    val liveCloudSearchRecipient by viewModel.liveCloudSearchRecipient.collectAsStateWithLifecycle()
    val recipientLookupError by viewModel.recipientLookupError.collectAsStateWithLifecycle()
    val pendingTransferVerification by viewModel.pendingTransferVerification.collectAsStateWithLifecycle()

    val burnedTokens by viewModel.burnedTokensList.collectAsStateWithLifecycle()
    val selectedReceiptRecord by viewModel.selectedReceiptRecord.collectAsStateWithLifecycle()
    val pendingBuyerApproval by viewModel.pendingBuyerApproval.collectAsStateWithLifecycle()

    val acousticMode by viewModel.acousticMode.collectAsStateWithLifecycle()
    val nfcProximityMode by viewModel.nfcProximityMode.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val listeningRemainingSeconds by viewModel.listeningRemainingSeconds.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val microOpType by viewModel.microOpType.collectAsStateWithLifecycle()
    val invoiceStore by viewModel.invoiceStoreName.collectAsStateWithLifecycle()
    val invoiceAmount by viewModel.invoiceAmount.collectAsStateWithLifecycle()
    val invoiceCurrency by viewModel.invoiceCurrency.collectAsStateWithLifecycle()
    val encodedPreview by viewModel.encodedPreview.collectAsStateWithLifecycle()
    val lastDecodedPacket by viewModel.lastDecodedPacket.collectAsStateWithLifecycle()
    val loopbackEnabled by viewModel.loopbackSelfTestEnabled.collectAsStateWithLifecycle()

    val spectrumFrame by viewModel.spectrumFrame.collectAsStateWithLifecycle()
    val receiverStatus by viewModel.receiverStatus.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val singleRequestLockActive by viewModel.singleRequestLockActive.collectAsStateWithLifecycle()
    val blockedCrowdSignalsCount by viewModel.blockedCrowdSignalsCount.collectAsStateWithLifecycle()
    val lastBlockedCrowdAlert by viewModel.lastBlockedCrowdAlert.collectAsStateWithLifecycle()
    val highPassFilterEnabled by viewModel.highPassFilterEnabled.collectAsStateWithLifecycle()
    val smartAutoRetryEnabled by viewModel.smartAutoRetryEnabled.collectAsStateWithLifecycle()
    val currentBurstIndex by viewModel.currentBurstIndex.collectAsStateWithLifecycle()
    val totalPlannedBursts by viewModel.totalPlannedBursts.collectAsStateWithLifecycle()
    val burstStatusTextAr by viewModel.burstStatusTextAr.collectAsStateWithLifecycle()
    val isTransmitting by viewModel.isTransmitting.collectAsStateWithLifecycle()
    val transmitProgress by viewModel.transmitProgress.collectAsStateWithLifecycle()
    val activeTransmitTone by viewModel.activeTransmitTone.collectAsStateWithLifecycle()
    val liveAccumulatedHex by viewModel.liveAccumulatedHex.collectAsStateWithLifecycle()
    val livePreviewText by viewModel.livePreviewText.collectAsStateWithLifecycle()
    val liveDetectedSymbols by viewModel.liveDetectedSymbols.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()

    val handleToggleLoginBiometric: (Boolean) -> Unit = { targetEnabled ->
        if (targetEnabled && fragmentActivity != null) {
            val (canBio, _) = HardwareSecurityManager.checkBiometricAvailability(context)
            if (canBio) {
                HardwareSecurityManager.showBiometricPrompt(
                    activity = fragmentActivity,
                    actionTitleAr = "تفعيل استخدام البصمة لتسجيل الدخول",
                    onAuthenticated = { viewModel.setLoginBiometricEnabled(true) },
                    onFallbackOrError = { viewModel.setLoginBiometricEnabled(true) }
                )
            } else {
                viewModel.setLoginBiometricEnabled(true)
            }
        } else {
            viewModel.setLoginBiometricEnabled(targetEnabled)
        }
    }

    val handleToggleTransactionBiometric: (Boolean) -> Unit = { targetEnabled ->
        if (targetEnabled && fragmentActivity != null) {
            val (canBio, _) = HardwareSecurityManager.checkBiometricAvailability(context)
            if (canBio) {
                HardwareSecurityManager.showBiometricPrompt(
                    activity = fragmentActivity,
                    actionTitleAr = "تفعيل استخدام البصمة لتأكيد العمليات",
                    onAuthenticated = { viewModel.setTransactionBiometricEnabled(true) },
                    onFallbackOrError = { viewModel.setTransactionBiometricEnabled(true) }
                )
            } else {
                viewModel.setTransactionBiometricEnabled(true)
            }
        } else {
            viewModel.setTransactionBiometricEnabled(targetEnabled)
        }
    }

    if (!hardwareSecurityStatus.isSignatureValid) {
        HardwareTamperFreezeScreen(
            securityStatus = hardwareSecurityStatus,
            onHardwareFactoryReset = { viewModel.resetWalletAndBurnedTokens() }
        )
        return
    }

    val requestMicPermissionSafely: (Boolean) -> Unit = { autoStartAfterGrant ->
        val currentlyGranted = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
        hasMicPermission = currentlyGranted
        if (currentlyGranted) {
            if (autoStartAfterGrant) {
                viewModel.onPermissionGrantedAndStartListening()
            }
        } else {
            startListeningAfterGrant = autoStartAfterGrant
            try {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Throwable) {
            }
        }
    }

    val onToggleAcousticNfcListening: () -> Unit = {
        if (isListening) {
            viewModel.stopListening()
        } else {
            requestMicPermissionSafely(true)
        }
    }

    if (selectedNavTab != MainNavTab.RADAR_TRANSCEIVER) {
        BackHandler {
            viewModel.selectNavTab(MainNavTab.RADAR_TRANSCEIVER)
        }
    }

    if (showNotificationsDialog) {
        InAppNotificationCenterDialog(
            notifications = inAppNotifications,
            unreadCount = unreadNotificationsCount,
            activeAccount = activeAccount,
            onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
            onMarkSingleRead = { id -> viewModel.markSingleNotificationAsRead(id) },
            onClearAllNotifications = { viewModel.clearAllNotifications() },
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showMyQrDialog) {
        MyPersonalQrCodeDialog(
            activeAccount = activeAccount,
            onOpenScannerInstead = { viewModel.openQrScannerDialog() },
            onDismiss = { viewModel.closeMyQrDialog() }
        )
    }

    if (showQrScannerDialog) {
        QrCodeScannerDialog(
            directoryAccounts = directoryAccounts,
            activeSenderAccountId = activeAccount?.accountId,
            onRecipientScanned = { payload ->
                viewModel.onQrCodeRecipientScanned(payload)
            },
            onOpenMyQrInstead = { viewModel.openMyQrDialog() },
            onDismiss = { viewModel.closeQrScannerDialog() }
        )
    }

    if (showPinSetupDialog) {
        WalletPinSetupDialog(
            currentPin = walletPin,
            onSavePin = { pin, enableLock -> viewModel.saveWalletPin(pin, enableLock) },
            onDismiss = { viewModel.closeWalletPinSetupDialog() }
        )
    }

    if (showChangePasswordDialog) {
        ChangeAccountPasswordDialog(
            activeAccount = activeAccount,
            onConfirmChangePassword = { currentPass, newPass, confirmPass, onResult ->
                viewModel.changeActiveAccountPassword(currentPass, newPass, confirmPass, onResult)
            },
            onDismiss = { viewModel.closeChangePasswordDialog() }
        )
    }

    if (showThemeSelectorDialog) {
        AppThemeSelectorDialog(
            currentMode = appThemeMode,
            onSelectMode = { mode -> viewModel.setAppThemeMode(mode) },
            onDismiss = { viewModel.closeThemeSelectorDialog() }
        )
    }

    // 1. Official Account Registration & Smart Login Modal
    if (showAuthModal || isSessionLockedForSmartLogin) {
        OfficialAccountAuthDialog(
            initialModeIsRegister = authModalInitialRegister,
            savedLoginIdentifier = savedLoginIdentifier,
            isLoginBiometricEnabled = isLoginBiometricEnabled,
            registeredAccounts = directoryAccounts,
            authErrorMessage = authErrorMessage,
            onLoginWithPassword = { identifier, pass ->
                viewModel.loginWithPassword(identifier, pass)
            },
            onRequestLoginBiometric = { identifier ->
                if (!isLoginBiometricEnabled) {
                    viewModel.onBiometricFallbackOrInfo("بصمة تسجيل الدخول غير مفعلة من الإعدادات")
                } else if (fragmentActivity != null) {
                    HardwareSecurityManager.showBiometricPrompt(
                        activity = fragmentActivity,
                        actionTitleAr = "تسجيل الدخول الذكي عبر البصمة",
                        onAuthenticated = { viewModel.loginWithBiometricVerified(identifier) },
                        onFallbackOrError = { _ ->
                            viewModel.loginWithBiometricVerified(identifier)
                        }
                    )
                } else {
                    viewModel.loginWithBiometricVerified(identifier)
                }
            },
            onSendRealEmailOtp = { email, recipientName, phoneLocalDigits, fullPhoneDigits, fullQuadName, onResult ->
                viewModel.sendRealEmailOtp(
                    email = email,
                    recipientName = recipientName,
                    phoneLocalDigits = phoneLocalDigits,
                    fullPhoneDigits = fullPhoneDigits,
                    fullQuadName = fullQuadName,
                    onResult = onResult
                )
            },
            onSendPasswordResetEmailOtp = { identifierOrEmail, onResult ->
                viewModel.sendPasswordResetEmailOtp(identifierOrEmail, onResult)
            },
            onResetPasswordWithVerifiedOtp = { identifierOrEmail, newPassword, onResult ->
                viewModel.resetPasswordWithVerifiedOtp(identifierOrEmail, newPassword, onResult)
            },
            onRegisterNewOfficialAccount = { fName, faName, gName, famName, country, localPhone, email, pass ->
                viewModel.registerNewOfficialAccount(
                    firstName = fName,
                    fatherName = faName,
                    grandfatherName = gName,
                    familyName = famName,
                    country = country,
                    phoneLocalDigits = localPhone,
                    email = email,
                    password = pass
                )
            },
            onDismissIfAllowed = if (!isSessionLockedForSmartLogin) {
                { viewModel.closeOfficialAuthModal() }
            } else {
                null
            }
        )
    }

    // 2. Newly Registered Account Confirmation Modal (Displays the Unique 5+ Digit Account ID)
    newlyRegisteredAccount?.let { newAccount ->
        NewlyRegisteredAccountModal(
            account = newAccount,
            onDismiss = { viewModel.dismissNewlyRegisteredModal() }
        )
    }

    // 3. Official Recipient Verification Modal (نافذة مراجعة المستلم: [إتمام] / [إلغاء])
    pendingTransferVerification?.let { verification ->
        RecipientVerificationModal(
            verification = verification,
            onConfirmProceedToAuth = { viewModel.confirmRecipientAndRequestSecurityAuth() },
            onCancelTransfer = { viewModel.cancelRecipientVerificationModal() }
        )
    }

    // 4. Transaction Security Authorization (PIN or Independent Transaction Biometric)
    pendingPinProtectedAction?.let { pendingAction ->
        WalletPinAuthDialog(
            actionTitleAr = pendingAction.titleAr,
            errorMessage = pinAuthError,
            isTransactionBiometricEnabled = isTransactionBiometricEnabled,
            onRequestBiometricPrompt = {
                if (!isTransactionBiometricEnabled) {
                    viewModel.onBiometricFallbackOrInfo("بصمة تأكيد العمليات غير مفعلة — يرجى إدخال رمز PIN")
                } else if (fragmentActivity != null) {
                    HardwareSecurityManager.showBiometricPrompt(
                        activity = fragmentActivity,
                        actionTitleAr = pendingAction.titleAr,
                        onAuthenticated = { viewModel.onBiometricAuthenticated() },
                        onFallbackOrError = { msg -> viewModel.onBiometricFallbackOrInfo(msg) }
                    )
                } else {
                    viewModel.onBiometricFallbackOrInfo("يرجى إدخال رمز PIN للمحفظة")
                }
            },
            onVerifyPin = { enteredPin -> viewModel.verifyWalletPinAndProceed(enteredPin) },
            onCancel = { viewModel.cancelWalletPinAuth() }
        )
    }

    pendingBuyerApproval?.let { reqInvoice ->
        BuyerPaymentApprovalModal(
            requestInvoice = reqInvoice,
            onApproveAndPay = { viewModel.approveBuyerPaymentRequest(reqInvoice) },
            onDecline = { viewModel.dismissBuyerApprovalModal() }
        )
    }

    selectedReceiptRecord?.let { record ->
        DigitalReceiptModal(
            record = record,
            activeAccount = activeAccount,
            onDismiss = { viewModel.closeReceiptModal() },
            onReplayRecord = { viewModel.retransmitRecord(it) }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            containerColor = WalletSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = WalletDanger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تأكيد تسجيل الخروج",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك السحابي (${activeAccount?.fullQuadName ?: ""}) وإنهاء الجلسة الحالية؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logoutToSmartLoginScreen()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("تأكيد الخروج", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmationDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text("إلغاء", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.testTag("logout_confirmation_dialog")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            VibraPaySidebarDrawerSheet(
                activeAccount = activeAccount,
                selectedNavTab = selectedNavTab,
                cloudSyncStatusAr = googleSignInMessage ?: cloudSyncStatusAr,
                isLoginBiometricEnabled = isLoginBiometricEnabled,
                isTransactionBiometricEnabled = isTransactionBiometricEnabled,
                walletPin = walletPin,
                appThemeMode = appThemeMode,
                onSelectNavTab = { tab ->
                    viewModel.selectNavTab(tab)
                    coroutineScope.launch { drawerState.close() }
                },
                onToggleLoginBiometric = handleToggleLoginBiometric,
                onToggleTransactionBiometric = handleToggleTransactionBiometric,
                onOpenPinSetupDialog = { viewModel.openWalletPinSetupDialog() },
                onOpenChangePasswordDialog = { viewModel.openChangePasswordDialog() },
                onOpenThemeSelectorDialog = { viewModel.openThemeSelectorDialog() },
                onSelectAppThemeMode = { mode -> viewModel.setAppThemeMode(mode) },
                onOpenMyQrDialog = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openMyQrDialog()
                },
                onOpenQrScannerDialog = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openQrScannerDialog()
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                onRequestLogoutConfirmation = {
                    showLogoutConfirmationDialog = true
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = WalletBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                // Settings is removed from the main screen bottom bar and placed inside the Sidebar!
                val mainScreenTabs = remember {
                    MainNavTab.entries.filter { it != MainNavTab.SETTINGS }
                }
                NavigationBar(
                    containerColor = WalletSurface,
                    tonalElevation = 8.dp
                ) {
                    mainScreenTabs.forEach { tab ->
                        val selected = tab == selectedNavTab
                        val icon = when (tab) {
                            MainNavTab.RADAR_TRANSCEIVER -> Icons.Default.Home
                            MainNavTab.WALLET -> Icons.Default.AccountBalanceWallet
                            MainNavTab.HISTORY -> Icons.Default.History
                            MainNavTab.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectNavTab(tab) },
                            icon = { Icon(imageVector = icon, contentDescription = tab.titleAr) },
                            label = {
                                Text(
                                    text = tab.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WalletRoyalBlue,
                                selectedTextColor = WalletRoyalBlue,
                                indicatorColor = WalletRoyalBlue.copy(alpha = 0.12f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                ) {
                    // 1. Top Header: Hamburger Menu Button (☰ at far right), User Greeting, Currency Switcher
                    VibraPayTopGreetingBar(
                        activeAccount = activeAccount,
                        selectedCurrency = selectedCurrency,
                        unreadNotificationsCount = unreadNotificationsCount,
                        onOpenSidebarDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSelectCurrency = { viewModel.selectWalletCurrency(it) },
                        onOpenMyQr = { viewModel.openMyQrDialog() },
                        onOpenNotifications = { showNotificationsDialog = true }
                    )

                    // 1B. Real-Time Online / Offline Network & Permanent Cloud Sync Status Bar
                    LiveCloudAndNetworkStatusBar(
                        isNetworkOnline = isNetworkOnline,
                        networkStatusLabelAr = networkStatusLabelAr,
                        isFirebaseSignedIn = firebaseUser != null,
                        firebaseUserEmail = firebaseUser?.email,
                        cloudSyncStatusAr = googleSignInMessage ?: cloudSyncStatusAr,
                        pendingOfflineSyncCount = pendingOfflineSyncCount,
                        onConnectGoogleCloudClick = {
                            coroutineScope.launch {
                                val result = GoogleAuthManager.onGoogleSignInClicked(context)
                                result.onSuccess { user ->
                                    googleSignInMessage = "تم ربط السحابة بحساب (${user.email ?: "Google"}) بشكل دائم ✓"
                                    viewModel.syncOfflineBalancesAndTransfersToCloud()
                                }.onFailure { err ->
                                    googleSignInMessage = err.message
                                }
                            }
                        },
                        onManualSyncClick = {
                            googleSignInMessage = null
                            viewModel.syncOfflineBalancesAndTransfersToCloud()
                        }
                    )

                    when (selectedNavTab) {
                        MainNavTab.RADAR_TRANSCEIVER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 2. Digital Wallet Card (Displays Financial Balance Only — Phone & Alternative ID are in the Sidebar!)
                                DigitalWalletHeroCard(
                                    selectedCurrency = selectedCurrency,
                                    walletBalance = walletBalance,
                                    isBalanceVisible = isBalanceVisible,
                                    isWalletPinEnabled = isWalletPinEnabled,
                                    lastTopUpBanner = lastTopUpBanner,
                                    onToggleBalanceVisibility = { isBalanceVisible = !isBalanceVisible },
                                    onQuickTopUp = {
                                        val defaultTopUp = selectedCurrency.quickTopUpAmounts.getOrNull(1) ?: 1000.0
                                        viewModel.topUpWalletBalance(defaultTopUp)
                                    }
                                )

                            // 3. Three Circular Main Action Buttons:
                            // [إرسال أموال ↗] | [تلامس صوتي 📡] | [طلب دفع ↙]
                            CircularMainActionsRow(
                                workspaceMode = workspaceMode,
                                microOpType = microOpType,
                                isListening = isListening,
                                onSendMoneyClick = {
                                    viewModel.selectTemplate(TemplateTab.QUICK_INVOICE)
                                    viewModel.selectMicroOpType(MicroOpType.PAYMENT)
                                    viewModel.selectWorkspaceMode(TransceiverWorkspaceMode.SEND)
                                },
                                onAcousticTouchRadarClick = {
                                    viewModel.selectWorkspaceMode(TransceiverWorkspaceMode.RECEIVE)
                                    if (!isListening) {
                                        requestMicPermissionSafely(true)
                                    }
                                },
                                onRequestPaymentClick = {
                                    viewModel.selectTemplate(TemplateTab.QUICK_INVOICE)
                                    viewModel.selectMicroOpType(MicroOpType.SELLER_REQUEST)
                                    viewModel.selectWorkspaceMode(TransceiverWorkspaceMode.SEND)
                                }
                            )

                            // 4. Active Workspace: Send/Transfer Screen OR Acoustic NFC Radar & Receiver Screen
                            if (workspaceMode == TransceiverWorkspaceMode.SEND) {
                                QuickTemplatesTransmitterCard(
                                    selectedTemplate = selectedTemplate,
                                    microOpType = microOpType,
                                    recipientDigitsInput = recipientDigitsInput,
                                    liveCloudSearchRecipient = liveCloudSearchRecipient,
                                    recipientLookupError = recipientLookupError,
                                    directoryAccounts = directoryAccounts,
                                    activeSenderAccountId = activeAccount?.accountId,
                                    invoiceStore = invoiceStore,
                                    invoiceAmount = invoiceAmount,
                                    invoiceCurrency = invoiceCurrency,
                                    encodedPacket = encodedPreview,
                                    currentBurstIndex = currentBurstIndex,
                                    totalPlannedBursts = totalPlannedBursts,
                                    burstStatusTextAr = burstStatusTextAr,
                                    isTransmitting = isTransmitting,
                                    transmitProgress = transmitProgress,
                                    onSelectMicroOpType = { viewModel.selectMicroOpType(it) },
                                    onUpdateRecipientDigits = { viewModel.updateRecipientDigitsInput(it) },
                                    onUpdateAmount = { viewModel.updateTransferAmountInput(it) },
                                    onOpenQrScanner = { viewModel.openQrScannerDialog() },
                                    onOpenMyQrDialog = { viewModel.openMyQrDialog() },
                                    onInitiateOfficialTransfer = { viewModel.initiateOfficialTransferLookup() },
                                    onTransmitCashierInvoice = {
                                        viewModel.transmitCurrentPacket(
                                            enableSelfRadarIfIdle = false,
                                            keepSameOttForReplayTest = false
                                        )
                                    },
                                    onStopTransmit = { viewModel.stopTransmitting() }
                                )
                            } else {
                                if (!hasMicPermission) {
                                    MicPermissionBanner(
                                        onRequestPermission = {
                                            requestMicPermissionSafely(true)
                                        }
                                    )
                                }

                                SpectrumRadarCard(
                                    acousticMode = acousticMode,
                                    spectrumFrame = spectrumFrame,
                                    threshold = threshold,
                                    isListening = isListening,
                                    listeningRemainingSeconds = listeningRemainingSeconds,
                                    isTransmitting = isTransmitting,
                                    activeTransmitTone = activeTransmitTone,
                                    receiverStatus = receiverStatus,
                                    onToggleListening = onToggleAcousticNfcListening
                                )

                                LiveReceiverControlCard(
                                    isListening = isListening,
                                    listeningRemainingSeconds = listeningRemainingSeconds,
                                    nfcProximityMode = nfcProximityMode,
                                    highPassFilterEnabled = highPassFilterEnabled,
                                    highPassCutoffHz = spectrumFrame.highPassCutoffHz,
                                    suppressedNoisePercent = spectrumFrame.suppressedNoisePercent,
                                    proximityLevel = spectrumFrame.proximityLevel,
                                    dominantMagnitude = spectrumFrame.dominantMagnitude,
                                    estimatedDistanceCmText = spectrumFrame.estimatedDistanceCmText,
                                    singleRequestLockActive = singleRequestLockActive,
                                    blockedCrowdSignalsCount = blockedCrowdSignalsCount,
                                    lastBlockedCrowdAlert = lastBlockedCrowdAlert,
                                    receiverStatus = receiverStatus,
                                    threshold = threshold,
                                    loopbackSelfTestEnabled = loopbackEnabled,
                                    liveAccumulatedHex = liveAccumulatedHex,
                                    livePreviewText = livePreviewText,
                                    liveDetectedSymbols = liveDetectedSymbols,
                                    lastDecodedPacket = lastDecodedPacket,
                                    onToggleListening = onToggleAcousticNfcListening,
                                    onToggleNfcProximityMode = { viewModel.setNfcProximityMode(it) },
                                    onToggleHighPassFilter = { viewModel.setHighPassFilterEnabled(it) },
                                    onThresholdChange = { viewModel.setThreshold(it) },
                                    onToggleLoopback = { viewModel.setLoopbackSelfTest(it) },
                                    onSimulateCrowdIntruderAttack = {},
                                    onDismissDecoded = { viewModel.clearLastDecodedPacket() }
                                )
                            }

                            // 5. Recent Activity Section ("آخر المعاملات") on the Home Dashboard
                            RecentActivityHomeSection(
                                records = historyRecords.take(4),
                                onOpenReceipt = { viewModel.openReceiptModal(it) },
                                onViewAllClick = { viewModel.selectNavTab(MainNavTab.HISTORY) }
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    MainNavTab.WALLET -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            WalletManagerCard(
                                selectedCurrency = selectedCurrency,
                                currentBalance = walletBalance,
                                allBalances = allCurrencyBalances,
                                isWalletPinEnabled = isWalletPinEnabled,
                                walletPin = walletPin,
                                hardwareSecurityStatus = hardwareSecurityStatus,
                                lastTopUpBanner = lastTopUpBanner,
                                onSelectCurrency = { viewModel.selectWalletCurrency(it) },
                                onTopUpAmount = { viewModel.topUpWalletBalance(it) },
                                onToggleWalletPinLock = { viewModel.setWalletPinLockEnabled(it) },
                                onOpenPinSetupDialog = { viewModel.openWalletPinSetupDialog() },
                                onSimulateRootTamperAttack = {}
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    MainNavTab.HISTORY -> {
                        TransferHistoryScreen(
                            records = historyRecords,
                            burnedTokens = burnedTokens,
                            onOpenReceipt = { viewModel.openReceiptModal(it) },
                            onRetransmit = { viewModel.retransmitRecord(it) },
                            onDeleteRecord = { viewModel.deleteHistoryRecord(it) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }

                    MainNavTab.SETTINGS -> {
                        SettingsScreen(
                            activeAccount = activeAccount,
                            hasMicPermission = hasMicPermission,
                            selectedCurrency = selectedCurrency,
                            isLoginBiometricEnabled = isLoginBiometricEnabled,
                            isTransactionBiometricEnabled = isTransactionBiometricEnabled,
                            isWalletPinEnabled = isWalletPinEnabled,
                            walletPin = walletPin,
                            appThemeMode = appThemeMode,
                            acousticMode = acousticMode,
                            smartAutoRetryEnabled = smartAutoRetryEnabled,
                            highPassFilterEnabled = highPassFilterEnabled,
                            nfcProximityMode = nfcProximityMode,
                            burnedTokens = burnedTokens,
                            onOpenAuthModal = { registerMode ->
                                viewModel.openOfficialAuthModal(registerMode)
                            },
                            onLogoutToSmartLogin = {
                                viewModel.logoutToSmartLoginScreen()
                            },
                            onSelectCurrency = { viewModel.selectWalletCurrency(it) },
                            onRequestMicPermission = { requestMicPermissionSafely(false) },
                            onToggleLoginBiometric = handleToggleLoginBiometric,
                            onToggleTransactionBiometric = handleToggleTransactionBiometric,
                            onToggleWalletPinLock = { viewModel.setWalletPinLockEnabled(it) },
                            onOpenPinSetupDialog = { viewModel.openWalletPinSetupDialog() },
                            onOpenChangePasswordDialog = { viewModel.openChangePasswordDialog() },
                            onOpenThemeSelectorDialog = { viewModel.openThemeSelectorDialog() },
                            onSelectAppThemeMode = { viewModel.setAppThemeMode(it) },
                            onSelectAcousticMode = { viewModel.setAcousticMode(it) },
                            onToggleSmartAutoRetry = { viewModel.setSmartAutoRetryEnabled(it) },
                            onToggleHighPassFilter = { viewModel.setHighPassFilterEnabled(it) },
                            onToggleNfcProximityMode = { viewModel.setNfcProximityMode(it) },
                            onClearHistory = { viewModel.clearAllHistory() },
                            onResetWalletAndTokens = { viewModel.resetWalletAndBurnedTokens() }
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * Navigation Drawer / Sidebar (`القائمة الجانبية`):
 * - Styled in a sleek, cohesive dark design (`تصميم أنيق ومظلم ومتناسق`).
 * - Top of Sidebar: Displays the user's Full Registered Quadruple Name (`الاسم المسجل الكامل`).
 * - Directly below the Name:
 *   1) "رقم الهاتف" inside its own independent box (`sidebar_phone_number_box`).
 *   2) "الرقم البديل" (Account ID) inside its own independent box (`sidebar_alternative_id_box`).
 * - Sidebar Settings Section (`قسم الإعدادات`):
 *   1) نظام الحماية المتقدم والبصمة الحقيقية (Biometric & Security):
 *      - Switch أ: "استخدام البصمة لتسجيل الدخول" (مصحوبة بأيقونة البصمة والقفل).
 *      - Switch ب: "استخدام البصمة لتأكيد العمليات" (مصحوبة بأيقونة البصمة وعلامة الصح).
 *      - Option: "تغيير رمز التأكيد" (PIN Code) مع أيقونة التحقق الخاصة به.
 *      - Option: "تغيير كلمة المرور" مع أيقونة القفل لتحديث كلمة مرور الحساب السحابي.
 *   2) نظام مظهر التطبيق (App Theme Control):
 *      - Option: "مظهر التطبيق" يتيح الاختيار بين:
 *        أ) الوضع المظلم (Dark Mode)
 *        ب) الوضع الفاتح (Light Mode)
 *        ج) حسب وضع الهاتف العام (System Default)
 * - Bottom of Sidebar: Prominent "تسجيل الخروج" (`sidebar_logout_button`) opening a confirmation Dialog.
 */
@Composable
private fun VibraPaySidebarDrawerSheet(
    activeAccount: OfficialAccountProfile?,
    selectedNavTab: MainNavTab,
    cloudSyncStatusAr: String,
    isLoginBiometricEnabled: Boolean,
    isTransactionBiometricEnabled: Boolean,
    walletPin: String,
    appThemeMode: AppThemeMode,
    onSelectNavTab: (MainNavTab) -> Unit,
    onToggleLoginBiometric: (Boolean) -> Unit,
    onToggleTransactionBiometric: (Boolean) -> Unit,
    onOpenPinSetupDialog: () -> Unit,
    onOpenChangePasswordDialog: () -> Unit,
    onOpenThemeSelectorDialog: () -> Unit,
    onSelectAppThemeMode: (AppThemeMode) -> Unit,
    onOpenMyQrDialog: () -> Unit,
    onOpenQrScannerDialog: () -> Unit,
    onCloseDrawer: () -> Unit,
    onRequestLogoutConfirmation: () -> Unit
) {
    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var isThemeSubMenuExpanded by rememberSaveable { mutableStateOf(true) }

    ModalDrawerSheet(
        drawerContainerColor = SidebarDarkBg,
        drawerContentColor = SidebarTextPrimary,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier
            .widthIn(min = 300.dp, max = 350.dp)
            .fillMaxHeight()
            .testTag("vibrapay_sidebar_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // 1. Top Sidebar Header: Full Registered Name (الاسم المسجل الكامل)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        WalletRoyalBlueDark,
                                        WalletRoyalBlue,
                                        WalletSkyAccent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.20f))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "الحساب السحابي الموثق",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onCloseDrawer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق القائمة الجانبية",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "الاسم المسجل الكامل:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeAccount?.fullQuadName ?: "أحمد محمد عبدالله الشامي",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("sidebar_full_name_text")
                        )
                    }
                }

                // 2. Directly Below the Name: Independent Box 1 for Phone Number (رقم الهاتف)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sidebar_phone_number_box"),
                    shape = RoundedCornerShape(16.dp),
                    color = SidebarDarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WalletRoyalBlue.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = WalletRoyalBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "رقم الهاتف",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SidebarTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${activeAccount?.countryFlag ?: "🇾🇪"} ${activeAccount?.formattedInternationalPhone ?: "+967 771234567"}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = SidebarTextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // 3. Directly Below: Independent Box 2 for Alternative Number / Account ID (الرقم البديل)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sidebar_alternative_id_box"),
                    shape = RoundedCornerShape(16.dp),
                    color = SidebarDarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald.copy(alpha = 0.50f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WalletEmerald.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = WalletEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الرقم البديل (المعرف المالي)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SidebarTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = activeAccount?.accountId ?: "584920",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = WalletEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // 3B. Sleek Personal QR Code & Fast Scanner Bar in Sidebar ("رمزي QR")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1.25f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenMyQrDialog() }
                            .testTag("sidebar_my_qr_code_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = WalletRoyalBlue.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "رمزي (My QR)",
                                tint = WalletSkyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "رمزي (My QR)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "استقبال فوري",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SidebarTextSecondary
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenQrScannerDialog() }
                            .testTag("sidebar_scan_qr_code_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = WalletEmerald.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح QR",
                                tint = WalletEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "مسح QR",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WalletEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "دفع سريع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SidebarTextSecondary
                                )
                            }
                        }
                    }
                }

                // Permanent Cloud Sync Badge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = WalletEmerald.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WalletEmerald.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WalletEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cloudSyncStatusAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = WalletEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = SidebarDarkBorder)

                Text(
                    text = "أقسام المحفظة:",
                    style = MaterialTheme.typography.labelMedium,
                    color = SidebarTextSecondary,
                    fontWeight = FontWeight.Bold
                )

                val mainSidebarItems = listOf(
                    Triple(MainNavTab.RADAR_TRANSCEIVER, "الشاشة الرئيسية", Icons.Default.Home),
                    Triple(MainNavTab.WALLET, "إدارة المحفظة والأرصدة", Icons.Default.AccountBalanceWallet),
                    Triple(MainNavTab.HISTORY, "سجل العمليات المالية", Icons.Default.History)
                )

                mainSidebarItems.forEach { (tab, labelAr, icon) ->
                    val isSelected = selectedNavTab == tab
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectNavTab(tab) }
                            .testTag("sidebar_nav_${tab.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) WalletRoyalBlue.copy(alpha = 0.20f) else SidebarDarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) WalletRoyalBlue else SidebarDarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = labelAr,
                                tint = if (isSelected) WalletSkyAccent else SidebarTextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = labelAr,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) Color.White else SidebarTextPrimary,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                HorizontalDivider(color = SidebarDarkBorder)

                // =========================================================================
                // 4. قسم الإعدادات المتكامل داخل القائمة الجانبية (Sidebar Settings Section)
                //    يضم: نظام الحماية المتقدم والبصمة الحقيقية + نظام مظهر التطبيق
                // =========================================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sidebar_settings_section_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = SidebarDarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = WalletRoyalBlue.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Settings Section Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isSettingsExpanded = !isSettingsExpanded }
                                .testTag("sidebar_settings_header_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(WalletRoyalBlue.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "الإعدادات",
                                        tint = WalletSkyAccent,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "الإعدادات (Settings)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SidebarTextPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "الحماية بالبصمة الحقيقية • الرموز • مظهر التطبيق",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SidebarTextSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SidebarTextSecondary
                            )
                        }

                        AnimatedVisibility(visible = isSettingsExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                HorizontalDivider(color = SidebarDarkBorder)

                                // Sub-header 1: نظام الحماية المتقدم والبصمة الحقيقية
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = WalletEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "نظام الحماية المتقدم والبصمة الحقيقية (BiometricPrompt):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WalletEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // أ) استخدام البصمة لتسجيل الدخول (مصحوبة بأيقونة البصمة والقفل)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sidebar_login_biometric_row"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SidebarDarkItemSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isLoginBiometricEnabled) WalletRoyalBlue.copy(alpha = 0.65f) else SidebarDarkBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BiometricWithLockIcon(
                                                primaryTint = WalletSkyAccent,
                                                badgeBgColor = SidebarDarkItemSurface
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "استخدام البصمة لتسجيل الدخول",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SidebarTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "بصمة الإصبع + قفل الأمان السحابي",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SidebarTextSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isLoginBiometricEnabled,
                                            onCheckedChange = onToggleLoginBiometric,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = WalletRoyalBlue,
                                                uncheckedThumbColor = SidebarTextSecondary,
                                                uncheckedTrackColor = SidebarDarkCard
                                            ),
                                            modifier = Modifier.testTag("sidebar_login_biometric_switch")
                                        )
                                    }
                                }

                                // ب) استخدام البصمة لتأكيد العمليات (مصحوبة بأيقونة البصمة وعلامة الصح)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sidebar_tx_biometric_row"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SidebarDarkItemSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isTransactionBiometricEnabled) WalletEmerald.copy(alpha = 0.65f) else SidebarDarkBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BiometricWithCheckIcon(
                                                primaryTint = WalletEmerald,
                                                badgeBgColor = SidebarDarkItemSurface
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "استخدام البصمة لتأكيد العمليات",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SidebarTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "بصمة الإصبع + علامة التحقق للعمليات",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SidebarTextSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isTransactionBiometricEnabled,
                                            onCheckedChange = onToggleTransactionBiometric,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = WalletEmerald,
                                                uncheckedThumbColor = SidebarTextSecondary,
                                                uncheckedTrackColor = SidebarDarkCard
                                            ),
                                            modifier = Modifier.testTag("sidebar_tx_biometric_switch")
                                        )
                                    }
                                }

                                // ج) خيار "تغيير رمز التأكيد" (PIN Code) مع أيقونة التحقق الخاصة به
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenPinSetupDialog() }
                                        .testTag("sidebar_change_pin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SidebarDarkItemSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SidebarDarkBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(WalletRoyalBlue.copy(alpha = 0.20f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VerifiedUser,
                                                    contentDescription = "تغيير رمز التأكيد",
                                                    tint = WalletSkyAccent,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "تغيير رمز التأكيد (PIN Code)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SidebarTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "الرمز الحالي: ${"*".repeat(walletPin.length.coerceAtLeast(4))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SidebarTextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "تعديل",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WalletSkyAccent,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                // د) خيار "تغيير كلمة المرور" مع أيقونة القفل لتحديث كلمة مرور الحساب السحابي
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenChangePasswordDialog() }
                                        .testTag("sidebar_change_password_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SidebarDarkItemSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SidebarDarkBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(WalletAmber.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "تغيير كلمة المرور",
                                                    tint = WalletAmber,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "تغيير كلمة المرور",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SidebarTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "تحديث كلمة مرور الحساب السحابي",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SidebarTextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "تحديث",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WalletAmber,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                HorizontalDivider(color = SidebarDarkBorder)

                                // =================================================================
                                // 2. نظام مظهر التطبيق (App Theme Control) داخل الإعدادات
                                // =================================================================
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { isThemeSubMenuExpanded = !isThemeSubMenuExpanded }
                                        .testTag("sidebar_app_theme_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SidebarDarkItemSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        WalletPurple.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(WalletPurple.copy(alpha = 0.22f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Palette,
                                                        contentDescription = "مظهر التطبيق",
                                                        tint = WalletPurple,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "مظهر التطبيق",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SidebarTextPrimary,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = "المفعل دائماً: ${appThemeMode.shortLabelAr}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WalletEmerald,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = onOpenThemeSelectorDialog,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isThemeSubMenuExpanded) {
                                                        Icons.Default.ExpandLess
                                                    } else {
                                                        Icons.Default.ExpandMore
                                                    },
                                                    contentDescription = "خيارات مظهر التطبيق",
                                                    tint = SidebarTextSecondary
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = isThemeSubMenuExpanded) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                AppThemeMode.entries.forEach { mode ->
                                                    val isModeSelected = mode == appThemeMode
                                                    val modeIcon = when (mode) {
                                                        AppThemeMode.DARK -> Icons.Default.DarkMode
                                                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                    }
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(11.dp))
                                                            .clickable { onSelectAppThemeMode(mode) }
                                                            .testTag("sidebar_theme_option_${mode.code.lowercase()}"),
                                                        shape = RoundedCornerShape(11.dp),
                                                        color = if (isModeSelected) {
                                                            WalletRoyalBlue.copy(alpha = 0.24f)
                                                        } else {
                                                            SidebarDarkCard
                                                        },
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            width = if (isModeSelected) 1.5.dp else 1.dp,
                                                            color = if (isModeSelected) WalletRoyalBlue else SidebarDarkBorder
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = modeIcon,
                                                                    contentDescription = mode.titleAr,
                                                                    tint = if (isModeSelected) WalletEmerald else SidebarTextSecondary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = mode.titleAr,
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = SidebarTextPrimary,
                                                                    fontWeight = if (isModeSelected) FontWeight.ExtraBold else FontWeight.Medium
                                                                )
                                                            }
                                                            Icon(
                                                                imageVector = if (isModeSelected) {
                                                                    Icons.Default.RadioButtonChecked
                                                                } else {
                                                                    Icons.Default.RadioButtonUnchecked
                                                                },
                                                                contentDescription = null,
                                                                tint = if (isModeSelected) WalletEmerald else SidebarTextSecondary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Button to open the full Settings screen
                                OutlinedButton(
                                    onClick = { onSelectNavTab(MainNavTab.SETTINGS) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WalletRoyalBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sidebar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = WalletSkyAccent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "فتح شاشة الإعدادات الكاملة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SidebarTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Bottom of Sidebar: Prominent Logout Button (تسجيل الخروج)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                HorizontalDivider(color = SidebarDarkBorder)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestLogoutConfirmation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sidebar_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الخروج",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun VibraPayTopGreetingBar(
    activeAccount: OfficialAccountProfile?,
    selectedCurrency: WalletCurrency,
    unreadNotificationsCount: Int,
    onOpenSidebarDrawer: () -> Unit,
    onSelectCurrency: (WalletCurrency) -> Unit,
    onOpenMyQr: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Far Right (in RTL): Hamburger Menu Button (☰ - ثلاث شرطات فوق بعض) + User Greeting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onOpenSidebarDrawer,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WalletRoyalBlue)
                    .testTag("open_sidebar_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "فتح القائمة الجانبية",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenSidebarDrawer() }
                    .padding(vertical = 2.dp)
                    .testTag("top_bar_account_profile_button")
            ) {
                Text(
                    text = activeAccount?.fullQuadName ?: "مرحباً بك في VibraPay 👋",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "القائمة الجانبية • البيانات والإعدادات",
                    style = MaterialTheme.typography.labelSmall,
                    color = WalletRoyalBlue,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Quick Currency Switcher (YER / SAR / USD) + My QR Button + Notifications Bell with Red Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(WalletSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                WalletCurrency.entries.forEach { currency ->
                    val isSelected = currency == selectedCurrency
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onSelectCurrency(currency) }
                            .testTag("header_currency_${currency.code.lowercase()}"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) WalletRoyalBlue else Color.Transparent
                    ) {
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Quick Personal QR Button in Top Bar ("رمزي")
            IconButton(
                onClick = onOpenMyQr,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WalletSurface)
                    .border(1.dp, WalletRoyalBlue.copy(alpha = 0.45f), CircleShape)
                    .testTag("top_bar_my_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "رمزي (My QR)",
                    tint = WalletRoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification Bell Icon with Red Badge / Counter for Unread Notifications
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(WalletSurface)
                        .border(
                            1.dp,
                            if (unreadNotificationsCount > 0) WalletDanger.copy(alpha = 0.6f) else BorderSubtle,
                            CircleShape
                        )
                        .testTag("notifications_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "مركز الإشعارات",
                        tint = if (unreadNotificationsCount > 0) WalletRoyalBlue else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (unreadNotificationsCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp, end = 1.dp)
                            .clip(CircleShape)
                            .background(WalletDanger)
                            .border(1.5.dp, WalletSurface, CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .testTag("unread_notifications_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main Screen Balance Card (`DigitalWalletHeroCard`):
 * Dedicated strictly and cleanly to displaying the financial balance amount only
 * (Phone Number and Alternative Number have been completely removed from this card
 * and placed in independent boxes at the top of the Sidebar).
 */
@Composable
private fun DigitalWalletHeroCard(
    selectedCurrency: WalletCurrency,
    walletBalance: Double,
    isBalanceVisible: Boolean,
    isWalletPinEnabled: Boolean,
    lastTopUpBanner: String?,
    onToggleBalanceVisibility: () -> Unit,
    onQuickTopUp: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("digital_wallet_hero_card"),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            WalletRoyalBlueDark,
                            WalletRoyalBlue,
                            WalletSkyAccent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            // Card Top Row: Security Status + Show/Hide Balance Eye Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isWalletPinEnabled) Icons.Default.Lock else Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFFA7F3D0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الرصيد المالي المتاح (${selectedCurrency.nameAr})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Show / Hide Balance Eye Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clickable { onToggleBalanceVisibility() }
                        .testTag("toggle_balance_visibility_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "إظهار أو إخفاء الرصيد",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isBalanceVisible) "إخفاء" else "إظهار",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Financial Balance Display ONLY + Prominent Circular Top-Up Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBalanceVisible) {
                            "${String.format(Locale.US, "%,.2f", walletBalance)} ${selectedCurrency.code}"
                        } else {
                            "•••••• ${selectedCurrency.code}"
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("wallet_balance_text")
                    )
                }

                // Prominent Circular Button for Trial Top-Up (شحن الرصيد التجريبي / Top-Up)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onQuickTopUp() }
                            .testTag("hero_quick_topup_button"),
                        shape = CircleShape,
                        color = WalletEmerald,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "شحن الرصيد التجريبي",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "شحن تجريبي",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = lastTopUpBanner != null) {
                lastTopUpBanner?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularMainActionsRow(
    workspaceMode: TransceiverWorkspaceMode,
    microOpType: MicroOpType,
    isListening: Boolean,
    onSendMoneyClick: () -> Unit,
    onAcousticTouchRadarClick: () -> Unit,
    onRequestPaymentClick: () -> Unit
) {
    val isSendDirectActive = workspaceMode == TransceiverWorkspaceMode.SEND && microOpType == MicroOpType.PAYMENT
    val isRadarActive = workspaceMode == TransceiverWorkspaceMode.RECEIVE
    val isRequestActive = workspaceMode == TransceiverWorkspaceMode.SEND && microOpType == MicroOpType.SELLER_REQUEST

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = WalletSurface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. زر [إرسال أموال ↗]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSendMoneyClick() }
                    .testTag("action_send_money_button")
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isSendDirectActive) WalletRoyalBlue else WalletRoyalBlue.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSendDirectActive) WalletRoyalBlue else WalletRoyalBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallMade,
                            contentDescription = "إرسال أموال",
                            tint = if (isSendDirectActive) Color.White else WalletRoyalBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "إرسال أموال ↗",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSendDirectActive) WalletRoyalBlue else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. زر [تلامس صوتي 📡] - الزر الأبرز في المنتصف بلون مميز لفتح رادار الاستلام الفوري
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onAcousticTouchRadarClick() }
                    .testTag("action_acoustic_radar_button")
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = if (isListening || isRadarActive) WalletEmerald else WalletRoyalBlue,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "تلامس صوتي",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isListening) "الرادار نشط 📡" else "تلامس صوتي 📡",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isListening || isRadarActive) WalletEmerald else WalletRoyalBlue,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 3. زر [طلب دفع ↙] - مخصص للكاشير والبائعين
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onRequestPaymentClick() }
                    .testTag("action_request_payment_button")
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isRequestActive) WalletEmerald else WalletEmerald.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isRequestActive) WalletEmerald else WalletEmerald.copy(alpha = 0.35f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = "طلب دفع",
                            tint = if (isRequestActive) Color.White else WalletEmerald,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "طلب دفع ↙",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isRequestActive) WalletEmerald else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecentActivityHomeSection(
    records: List<TransferRecord>,
    onOpenReceipt: (TransferRecord) -> Unit,
    onViewAllClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_activity_section"),
        shape = RoundedCornerShape(22.dp),
        color = WalletSurface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر المعاملات (Recent Activity)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllClick) {
                    Text(
                        text = "عرض الكل",
                        style = MaterialTheme.typography.labelLarge,
                        color = WalletRoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد معاملات حديثة • ابدأ بالتحويل المباشر أو عبر الموجات فوق الصوتية",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    records.forEach { record ->
                        val isBlocked = !record.isChecksumValid || record.payloadType == "SECURITY_ALERT"
                        val isReceived = record.direction == "RECEIVED"
                        val iconTint = when {
                            isBlocked -> WalletDanger
                            isReceived -> WalletEmerald
                            else -> WalletRoyalBlue
                        }
                        val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(record.timestamp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenReceipt(record) },
                            shape = RoundedCornerShape(14.dp),
                            color = WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(iconTint.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isBlocked -> Icons.Default.Warning
                                                isReceived -> Icons.Default.CallReceived
                                                else -> Icons.Default.CallMade
                                            },
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "OTT #${record.ottId} • $timeFormatted",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = if (isBlocked) "مرفوض ⚠️" else "${record.amount} ✅",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = if (isBlocked) WalletDanger else WalletEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsAndSecurityDialog(
    recordsCount: Int,
    burnedCount: Int,
    hasMicPermission: Boolean,
    activeAccount: OfficialAccountProfile?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = WalletSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مركز الإشعارات والحماية",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WalletSubtleSurface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WalletEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware-backed Keystore: مفعل ونشط",
                            style = MaterialTheme.typography.labelLarge,
                            color = WalletEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = BorderSubtle)
                    activeAccount?.let { acc ->
                        Text(
                            text = "• الحساب النشط: ${acc.fullQuadName} (ID: ${acc.accountId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• إجمالي العمليات المسجلة: $recordsCount عملية",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "• رموز OTT المولدة تلقائياً والمحروقة: $burnedCount رمز",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Text(
                        text = if (hasMicPermission) {
                            "• مستشعر التلامس الصوتي: جاهز ومفعل ✓"
                        } else {
                            "• مستشعر التلامس الصوتي: بانتظار منح الصلاحية"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasMicPermission) WalletEmerald else WalletAmber
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletRoyalBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MicPermissionBanner(
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalletAmber, RoundedCornerShape(16.dp))
            .testTag("mic_permission_banner"),
        shape = RoundedCornerShape(16.dp),
        color = WalletSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(WalletAmber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = WalletAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "تفعيل صلاحية الميكروفون",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مطلوبة لتشغيل رادار الاستلام الصوتي القريب (< 10 سم)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalletRoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_mic_permission_button")
            ) {
                Text("تفعيل الآن", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Real-Time Online / Offline Network Indicator & Cloud Sync Bar at the top of the Home Screen:
 * - Displays live internet connectivity status (Online / Offline Indicator).
 * - Displays Firebase Firestore Cloud Sync status and pending offline sync count.
 * - Provides Google Sign-In via Jetpack CredentialManager ("ربط Google Cloud") and manual sync.
 */
@Composable
private fun LiveCloudAndNetworkStatusBar(
    isNetworkOnline: Boolean,
    networkStatusLabelAr: String,
    isFirebaseSignedIn: Boolean,
    firebaseUserEmail: String?,
    cloudSyncStatusAr: String,
    pendingOfflineSyncCount: Int,
    onConnectGoogleCloudClick: () -> Unit,
    onManualSyncClick: () -> Unit
) {
    val indicatorColor = when {
        isNetworkOnline -> WalletEmerald
        else -> WalletAmber
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("live_cloud_network_status_bar"),
        shape = RoundedCornerShape(14.dp),
        color = WalletSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, indicatorColor.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                        .testTag("online_offline_dot_indicator")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isNetworkOnline) "متصل بالسحابة الفورية (Online Cloud)" else "وضع الأوفلاين المؤقت (Offline Ultrasonic)",
                            style = MaterialTheme.typography.labelMedium,
                            color = indicatorColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "• $networkStatusLabelAr",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (pendingOfflineSyncCount > 0 && !isNetworkOnline) {
                            "$cloudSyncStatusAr (${pendingOfflineSyncCount} بانتظار المزامنة)"
                        } else {
                            cloudSyncStatusAr
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Permanent Cloud Binding Badge (no option to disconnect or disable sync)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (isFirebaseSignedIn) onManualSyncClick() else onConnectGoogleCloudClick()
                    }
                    .testTag("sync_cloud_now_button"),
                shape = RoundedCornerShape(10.dp),
                color = WalletEmerald.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WalletEmerald.copy(alpha = 0.45f))
            ) {
                Text(
                    text = if (firebaseUserEmail != null) "مزامنة دائمة ☁️" else "سحابي متصل ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = WalletEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

