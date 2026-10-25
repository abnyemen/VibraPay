package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OfficialAccountProfile
import com.example.data.PendingTransferVerification
import com.example.data.WorldCountriesCatalog
import com.example.data.WorldCountry
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletRoyalBlueDark
import com.example.ui.theme.WalletSkyAccent
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface
import kotlin.random.Random

/**
 * Full-Screen or Modal Official Registration & Smart Login Experience:
 * 1) Official Registration Flow:
 *    - 4 separate mandatory fields for Quadruple Name (الاسم الأول، الأب، الجد، اللقب/العائلة)
 *    - World Country Picker Modal with Flag + Name + Dial Code + Live Search
 *    - Email Verification with 5/6-digit OTP
 *    - Password + Confirm Password
 *    - Automatic generation of 5+ digit numeric-only Account ID
 * 2) Smart Login Flow:
 *    - Automatically pre-fills saved Email or Phone Number from previous registration/login
 *    - Supports independent Login Biometric authentication
 */
@Composable
fun OfficialAccountAuthDialog(
    initialModeIsRegister: Boolean = false,
    savedLoginIdentifier: String,
    isLoginBiometricEnabled: Boolean,
    registeredAccounts: List<OfficialAccountProfile>,
    authErrorMessage: String?,
    onLoginWithPassword: (identifier: String, password: String) -> Unit,
    onRequestLoginBiometric: (identifier: String) -> Unit,
    onSendRealEmailOtp: (
        email: String,
        recipientName: String,
        phoneLocalDigits: String,
        fullPhoneDigits: String,
        fullQuadName: String,
        onResult: (otpCode: String, statusMessageAr: String) -> Unit
    ) -> Unit = { _, _, _, _, _, onResult ->
        onResult("", "تعذر الاتصال بخدمة إرسال البريد الإلكتروني الرسمية (EmailJS)")
    },
    onSendPasswordResetEmailOtp: (
        identifierOrEmail: String,
        onResult: (otpCode: String, resolvedEmail: String, statusMessageAr: String) -> Unit
    ) -> Unit = { _, onResult ->
        onResult("", "", "تعذر الاتصال بخدمة استعادة كلمة المرور عبر البريد الإلكتروني (EmailJS)")
    },
    onResetPasswordWithVerifiedOtp: (
        identifierOrEmail: String,
        newPassword: String,
        onResult: (success: Boolean, messageAr: String) -> Unit
    ) -> Unit = { _, _, onResult ->
        onResult(true, "تم إعادة تعيين كلمة المرور بنجاح ✓")
    },
    onRegisterNewOfficialAccount: (
        firstName: String,
        fatherName: String,
        grandfatherName: String,
        familyName: String,
        country: WorldCountry,
        phoneLocalDigits: String,
        email: String,
        password: String
    ) -> Unit,
    onDismissIfAllowed: (() -> Unit)? = null
) {
    var isRegisterMode by rememberSaveable { mutableStateOf(initialModeIsRegister) }

    // Smart Login State (pre-filled with saved Email or Phone Number!)
    var loginIdentifier by rememberSaveable(savedLoginIdentifier) {
        mutableStateOf(savedLoginIdentifier)
    }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var showLoginPassword by rememberSaveable { mutableStateOf(false) }
    var loginSuccessNotice by rememberSaveable { mutableStateOf<String?>(null) }

    // Standalone Forgot Password / Password Reset Modal Trigger
    var showStandalonePasswordResetModal by rememberSaveable { mutableStateOf(false) }

    // Registration State: 4 Mandatory Name Fields
    var firstName by rememberSaveable { mutableStateOf("") }
    var fatherName by rememberSaveable { mutableStateOf("") }
    var grandfatherName by rememberSaveable { mutableStateOf("") }
    var familyName by rememberSaveable { mutableStateOf("") }

    // Country & Phone State
    var selectedCountry by remember { mutableStateOf(WorldCountriesCatalog.allCountries.first()) }
    var showCountryPickerModal by remember { mutableStateOf(false) }
    var phoneDigits by rememberSaveable { mutableStateOf("") }

    // Email & OTP State
    var emailInput by rememberSaveable { mutableStateOf("") }
    var sentOtpCode by rememberSaveable { mutableStateOf("") }
    var enteredOtpCode by rememberSaveable { mutableStateOf("") }
    var isEmailOtpVerified by rememberSaveable { mutableStateOf(false) }
    var otpStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Red Top Notification/Banner State when requesting OTP or encountering verification/duplicate alerts
    var topRedBannerMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Password & Confirm Password State
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regConfirmPassword by rememberSaveable { mutableStateOf("") }
    var showRegPassword by rememberSaveable { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    // Sync external authErrorMessage into topRedBannerMessage whenever it appears
    LaunchedEffect(authErrorMessage) {
        if (!authErrorMessage.isNullOrBlank()) {
            topRedBannerMessage = authErrorMessage
        }
    }

    if (showCountryPickerModal) {
        WorldCountryPickerModal(
            selectedCountry = selectedCountry,
            onSelectCountry = { country ->
                selectedCountry = country
                showCountryPickerModal = false
            },
            onDismiss = { showCountryPickerModal = false }
        )
    }

    if (showStandalonePasswordResetModal) {
        StandalonePasswordResetModal(
            initialEmailOrIdentifier = loginIdentifier.ifBlank { savedLoginIdentifier },
            onSendPasswordResetEmailOtp = onSendPasswordResetEmailOtp,
            onResetPasswordWithVerifiedOtp = { identifierOrEmail, newPassword, onResult ->
                onResetPasswordWithVerifiedOtp(identifierOrEmail.trim(), newPassword.trim()) { ok, msg ->
                    onResult(ok, msg)
                    if (ok) {
                        loginIdentifier = identifierOrEmail.trim()
                        loginPassword = newPassword.trim()
                        localValidationError = null
                        topRedBannerMessage = null
                        loginSuccessNotice = "تم تحديث كلمة المرور في السحابة بنجاح ✓ يمكنك تسجيل الدخول الآن"
                        showStandalonePasswordResetModal = false
                    }
                }
            },
            onDismiss = { showStandalonePasswordResetModal = false }
        )
    }

    Dialog(
        onDismissRequest = { onDismissIfAllowed?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .testTag("official_auth_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = WalletSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // =========================================================================
                // RED TOP NOTIFICATION / BANNER (يظهر في أعلى الشاشة عند طلب OTP أو وجود تنبيه تحقق)
                // =========================================================================
                val activeTopRedAlert = topRedBannerMessage ?: localValidationError ?: authErrorMessage
                AnimatedVisibility(visible = !activeTopRedAlert.isNullOrBlank()) {
                    Column {
                        TopRedVerificationBanner(
                            message = activeTopRedAlert ?: "",
                            onDismiss = {
                                topRedBannerMessage = null
                                localValidationError = null
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Top Brand Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(WalletRoyalBlueDark, WalletRoyalBlue, WalletSkyAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "محفظة فايبرا باي الرسمية",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Hybrid Official E-Wallet (Online & Ultrasonic)",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletRoyalBlue
                            )
                        }
                    }

                    if (onDismissIfAllowed != null) {
                        IconButton(
                            onClick = onDismissIfAllowed,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_auth_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Switcher: تسجيل الدخول الذكي vs إنشاء حساب رسمي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WalletSubtleSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isRegisterMode = false
                                localValidationError = null
                                topRedBannerMessage = null
                            }
                            .testTag("auth_tab_login"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isRegisterMode) WalletRoyalBlue else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (!isRegisterMode) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تسجيل الدخول",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!isRegisterMode) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isRegisterMode = true
                                localValidationError = null
                                topRedBannerMessage = null
                            }
                            .testTag("auth_tab_register"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRegisterMode) WalletEmerald else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (isRegisterMode) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إنشاء حساب جديد",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isRegisterMode) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isRegisterMode) {
                    // =========================================================================
                    // SMART LOGIN FLOW (حفظ بيانات الدخول + البصمة المستقلة للدخول)
                    // =========================================================================
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!loginSuccessNotice.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WalletEmerald.copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        WalletEmerald.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = WalletEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = loginSuccessNotice!!,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WalletEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (savedLoginIdentifier.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WalletEmerald.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        WalletEmerald.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = WalletEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تم استرجاع بيانات الدخول المحفوظة تلقائياً • أدخل كلمة المرور أو البصمة للدخول الفوري",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WalletEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = {
                                    loginIdentifier = it
                                    localValidationError = null
                                    topRedBannerMessage = null
                                    loginSuccessNotice = null
                                },
                                label = { Text("البريد الإلكتروني أو رقم الهاتف أو رقم الحساب") },
                                placeholder = { Text("مثال: 771234567 أو ahmed.shami@vibrapay.com") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = WalletRoyalBlue
                                    )
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_identifier_input")
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    localValidationError = null
                                    topRedBannerMessage = null
                                    loginSuccessNotice = null
                                },
                                label = { Text("كلمة المرور") },
                                placeholder = { Text("أدخل كلمة المرور الخاصة بحسابك") },
                                singleLine = true,
                                visualTransformation = if (showLoginPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = WalletRoyalBlue
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                        Icon(
                                            imageVector = if (showLoginPassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = "إظهار كلمة المرور"
                                        )
                                    }
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            val displayErr = localValidationError ?: authErrorMessage
                            if (!displayErr.isNullOrBlank()) {
                                Text(
                                    text = displayErr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WalletDanger,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val cleanId = loginIdentifier.trim()
                                    val cleanPass = loginPassword.trim()
                                    if (cleanId.isBlank()) {
                                        localValidationError = "يرجى إدخال البريد الإلكتروني أو رقم الهاتف"
                                        topRedBannerMessage = localValidationError
                                    } else if (cleanPass.isBlank()) {
                                        localValidationError = "يرجى إدخال كلمة المرور"
                                        topRedBannerMessage = localValidationError
                                    } else {
                                        localValidationError = null
                                        topRedBannerMessage = null
                                        onLoginWithPassword(cleanId, cleanPass)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WalletRoyalBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("smart_login_submit_button")
                            ) {
                                Text(
                                    text = "تسجيل الدخول للمحفظة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Independent Biometric Login Button
                            if (isLoginBiometricEnabled) {
                                OutlinedButton(
                                    onClick = {
                                        val targetId = loginIdentifier.trim().ifBlank {
                                            registeredAccounts.firstOrNull()?.phoneNumberLocal ?: "584920"
                                        }
                                        onRequestLoginBiometric(targetId)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("biometric_login_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = WalletEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الدخول السريع ببصمة الإصبع (Login Biometric)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WalletEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Opens Standalone Password Reset Modal (Separate Screen/Modal)
                            OutlinedButton(
                                onClick = {
                                    localValidationError = null
                                    topRedBannerMessage = null
                                    showStandalonePasswordResetModal = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WalletRoyalBlue.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("forgot_password_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = WalletRoyalBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نسيت كلمة المرور / إعادة تعيين كلمة المرور",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WalletRoyalBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Switch / Saved Accounts Directory Preview
                    if (registeredAccounts.isNotEmpty()) {
                        Text(
                            text = "الحسابات الرسمية المسجلة (اضغط لتعبئة بيانات الدخول تلقائياً):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            registeredAccounts.take(4).forEach { acc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            loginIdentifier = acc.phoneNumberLocal.trim()
                                            loginPassword = acc.passwordPlainOrHash.trim()
                                            localValidationError = null
                                            topRedBannerMessage = null
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = WalletSubtleSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = acc.fullQuadName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${acc.countryFlag} ${acc.formattedInternationalPhone} • ${acc.email}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WalletRoyalBlue.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "ID: ${acc.accountId}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                color = WalletRoyalBlue,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // =========================================================================
                    // OFFICIAL REGISTRATION FLOW (الاسم الرباعي + الدول + OTP + كلمة المرور)
                    // =========================================================================
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. الاسم الرباعي الإجباري (4 خانات منفصلة)
                            Text(
                                text = "1. الاسم الرباعي الرسمي (4 خانات إجبارية):",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = {
                                        firstName = it
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("الاسم الأول *") },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_first_name_input")
                                )

                                OutlinedTextField(
                                    value = fatherName,
                                    onValueChange = {
                                        fatherName = it
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("اسم الأب *") },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_father_name_input")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = grandfatherName,
                                    onValueChange = {
                                        grandfatherName = it
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("اسم الجد *") },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_grandfather_name_input")
                                )

                                OutlinedTextField(
                                    value = familyName,
                                    onValueChange = {
                                        familyName = it
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("اللقب / العائلة *") },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_family_name_input")
                                )
                            }

                            HorizontalDivider(color = BorderSubtle)

                            // 2. قائمة الدول وأرقام الهواتف
                            Text(
                                text = "2. الدولة ورقم الهاتف الرسمي:",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Country Picker Button (Opens Modal with All World Countries & Live Search)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showCountryPickerModal = true }
                                        .testTag("open_country_picker_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "اختر الدولة",
                                            tint = WalletRoyalBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = phoneDigits,
                                    onValueChange = { input ->
                                        phoneDigits = input.filter { it.isDigit() }.take(15)
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("رقم الهاتف (${selectedCountry.nameAr})") },
                                    placeholder = { Text("771234567") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_phone_input")
                                )
                            }

                            HorizontalDivider(color = BorderSubtle)

                            // 3. التحقق من البريد الإلكتروني (Email OTP)
                            Text(
                                text = "3. التحقق من البريد الإلكتروني (Email OTP):",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {
                                        emailInput = it.trim()
                                        sentOtpCode = ""
                                        enteredOtpCode = ""
                                        isEmailOtpVerified = false
                                        otpStatusMessage = null
                                        localValidationError = null
                                        topRedBannerMessage = null
                                    },
                                    label = { Text("البريد الإلكتروني الرسمي (Gmail) *") },
                                    placeholder = { Text("yourname@gmail.com") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_email_input")
                                )

                                Button(
                                    onClick = {
                                        val candidateQuadName =
                                            "${firstName.trim()} ${fatherName.trim()} ${grandfatherName.trim()} ${familyName.trim()}"
                                                .replace(Regex("\\s+"), " ")
                                                .trim()
                                        val cleanLocalPhone = phoneDigits.filter { it.isDigit() }
                                        val dialDigits = selectedCountry.dialCode.filter { it.isDigit() }
                                        val fullPhoneCandidate = "$dialDigits$cleanLocalPhone"

                                        // Strict 3-Field OR Duplicate Check BEFORE sending OTP
                                        val isDuplicateInList = registeredAccounts.any { acc ->
                                            val existingQuad = acc.fullQuadName.trim().replace(Regex("\\s+"), " ")
                                            (emailInput.isNotBlank() && acc.email.trim().equals(emailInput.trim(), ignoreCase = true)) ||
                                                (cleanLocalPhone.isNotBlank() && (
                                                    acc.phoneNumberLocal == cleanLocalPhone ||
                                                        acc.fullPhoneDigits == cleanLocalPhone ||
                                                        acc.fullPhoneDigits == fullPhoneCandidate ||
                                                        (cleanLocalPhone.length >= 6 && acc.fullPhoneDigits.endsWith(cleanLocalPhone)) ||
                                                        (acc.phoneNumberLocal.length >= 6 && cleanLocalPhone.endsWith(acc.phoneNumberLocal))
                                                    )) ||
                                                (candidateQuadName.isNotBlank() &&
                                                    firstName.isNotBlank() && fatherName.isNotBlank() &&
                                                    grandfatherName.isNotBlank() && familyName.isNotBlank() &&
                                                    existingQuad.equals(candidateQuadName, ignoreCase = true))
                                        }

                                        when {
                                            firstName.isBlank() || fatherName.isBlank() ||
                                                grandfatherName.isBlank() || familyName.isBlank() -> {
                                                val err = "يرجى إدخال الاسم الرباعي كاملاً قبل طلب رمز التحقق (OTP)"
                                                localValidationError = err
                                                topRedBannerMessage = err
                                            }
                                            cleanLocalPhone.length < 6 -> {
                                                val err = "يرجى إدخال رقم الهاتف الرسمي قبل طلب رمز التحقق (OTP)"
                                                localValidationError = err
                                                topRedBannerMessage = err
                                            }
                                            !emailInput.contains("@") || !emailInput.contains(".") -> {
                                                val err = "يرجى إدخال بريد إلكتروني حقيقي وصحيح (Gmail) أولاً"
                                                localValidationError = err
                                                topRedBannerMessage = err
                                            }
                                            isDuplicateInList -> {
                                                val duplicateErr =
                                                    "يوجد حساب مسجل بالفعل بهذه البيانات (الاسم أو رقم الهاتف أو البريد الإلكتروني)"
                                                localValidationError = duplicateErr
                                                otpStatusMessage = duplicateErr
                                                topRedBannerMessage = duplicateErr
                                            }
                                            else -> {
                                                localValidationError = null
                                                sentOtpCode = ""
                                                enteredOtpCode = ""
                                                isEmailOtpVerified = false
                                                val sendingAlert =
                                                    "تنبيه التحقق: جاري فحص السحابة وإرسال رمز التحقق (OTP) إلى بريدك الإلكتروني ($emailInput)"
                                                otpStatusMessage = sendingAlert
                                                topRedBannerMessage = sendingAlert

                                                onSendRealEmailOtp(
                                                    emailInput.trim(),
                                                    candidateQuadName,
                                                    cleanLocalPhone,
                                                    fullPhoneCandidate,
                                                    candidateQuadName
                                                ) { otpCode, statusAr ->
                                                    if (otpCode.isNotBlank()) {
                                                        sentOtpCode = otpCode
                                                        enteredOtpCode = ""
                                                        isEmailOtpVerified = false
                                                        otpStatusMessage = statusAr
                                                        topRedBannerMessage =
                                                            "تنبيه هام: تم إرسال رمز التحقق (OTP) إلى بريدك ($emailInput) — افتح Gmail وأدخل الكود المكون من 6 أرقام"
                                                    } else {
                                                        sentOtpCode = ""
                                                        enteredOtpCode = ""
                                                        isEmailOtpVerified = false
                                                        otpStatusMessage = statusAr
                                                        localValidationError = statusAr
                                                        topRedBannerMessage = statusAr
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WalletRoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("send_email_otp_button")
                                ) {
                                    Text(
                                        text = if (sentOtpCode.isEmpty()) "إرسال OTP" else "إعادة إرسال",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Dedicated OTP Field appears after entering Email or clicking Send OTP
                            AnimatedVisibility(visible = emailInput.contains("@") || sentOtpCode.isNotEmpty() || otpStatusMessage != null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = enteredOtpCode,
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }.take(6)
                                                enteredOtpCode = digits
                                                localValidationError = null
                                                if (sentOtpCode.isNotBlank() && digits == sentOtpCode) {
                                                    isEmailOtpVerified = true
                                                    otpStatusMessage = "تم التحقق من مطابقة رمز البريد الإلكتروني (OTP) بنجاح ✓"
                                                    topRedBannerMessage = null
                                                } else {
                                                    isEmailOtpVerified = false
                                                }
                                            },
                                            label = { Text("رمز التحقق (6 أرقام من رسالتك في Gmail)") },
                                            placeholder = { Text("انسخ الكود الوارد لبريدك الإلكتروني وأدخله هنا") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            colors = authTextFieldColors(),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("reg_email_otp_input")
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isEmailOtpVerified) WalletEmerald.copy(alpha = 0.14f)
                                            else WalletAmber.copy(alpha = 0.14f),
                                            modifier = Modifier.clickable {
                                                if (sentOtpCode.isNotBlank() && enteredOtpCode.trim() == sentOtpCode) {
                                                    isEmailOtpVerified = true
                                                    localValidationError = null
                                                    topRedBannerMessage = null
                                                    otpStatusMessage = "تم تأكيد رمز التحقق (OTP) بنجاح ✓"
                                                } else {
                                                    isEmailOtpVerified = false
                                                    val msg = if (sentOtpCode.isBlank()) {
                                                        "يرجى الضغط على (إرسال OTP) أولاً لإرسال الرمز إلى بريدك الإلكتروني (Gmail)"
                                                    } else {
                                                        "رمز التحقق المدخل غير صحيح! يرجى نسخ الكود من رسالتك في Gmail وإدخاله بدقة"
                                                    }
                                                    otpStatusMessage = msg
                                                    topRedBannerMessage = msg
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = if (isEmailOtpVerified) "موثق ✓" else "تحقق",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isEmailOtpVerified) WalletEmerald else WalletAmber,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                                            )
                                        }
                                    }

                                    otpStatusMessage?.let { msg ->
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isEmailOtpVerified || sentOtpCode.isNotBlank()) WalletEmerald else WalletDanger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            // 4. تعيين كلمة المرور وتأكيدها
                            Text(
                                text = "4. تعيين كلمة المرور للحساب:",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = {
                                    regPassword = it
                                    localValidationError = null
                                    topRedBannerMessage = null
                                },
                                label = { Text("كلمة المرور *") },
                                singleLine = true,
                                visualTransformation = if (showRegPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                        Icon(
                                            imageVector = if (showRegPassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null
                                        )
                                    }
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input")
                            )

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = {
                                    regConfirmPassword = it
                                    localValidationError = null
                                    topRedBannerMessage = null
                                },
                                label = { Text("تأكيد كلمة المرور *") },
                                singleLine = true,
                                visualTransformation = if (showRegPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_confirm_password_input")
                            )

                            val displayError = localValidationError ?: authErrorMessage
                            if (!displayError.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WalletDanger.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WalletDanger.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("duplicate_account_error_banner")
                                ) {
                                    Text(
                                        text = displayError,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WalletDanger,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val candidateQuadName =
                                        "${firstName.trim()} ${fatherName.trim()} ${grandfatherName.trim()} ${familyName.trim()}"
                                            .replace(Regex("\\s+"), " ")
                                            .trim()
                                    val cleanLocalPhone = phoneDigits.filter { it.isDigit() }
                                    val dialDigits = selectedCountry.dialCode.filter { it.isDigit() }
                                    val fullPhoneCandidate = "$dialDigits$cleanLocalPhone"
                                    val isDuplicateInList = registeredAccounts.any { acc ->
                                        val existingQuad = acc.fullQuadName.trim().replace(Regex("\\s+"), " ")
                                        (emailInput.isNotBlank() && acc.email.trim().equals(emailInput.trim(), ignoreCase = true)) ||
                                            (cleanLocalPhone.isNotBlank() && (
                                                acc.phoneNumberLocal == cleanLocalPhone ||
                                                    acc.fullPhoneDigits == cleanLocalPhone ||
                                                    acc.fullPhoneDigits == fullPhoneCandidate ||
                                                    (cleanLocalPhone.length >= 6 && acc.fullPhoneDigits.endsWith(cleanLocalPhone)) ||
                                                    (acc.phoneNumberLocal.length >= 6 && cleanLocalPhone.endsWith(acc.phoneNumberLocal))
                                                )) ||
                                            (candidateQuadName.isNotBlank() && existingQuad.equals(candidateQuadName, ignoreCase = true))
                                    }

                                    when {
                                        firstName.isBlank() || fatherName.isBlank() ||
                                            grandfatherName.isBlank() || familyName.isBlank() -> {
                                            val err = "يرجى إدخال الاسم الرباعي كاملاً (الاسم الأول، الأب، الجد، اللقب)"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        phoneDigits.length < 6 -> {
                                            val err = "يرجى إدخال رقم هاتف صحيح"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        !emailInput.contains("@") -> {
                                            val err = "يرجى إدخال البريد الإلكتروني"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        isDuplicateInList -> {
                                            val err =
                                                "يوجد حساب مسجل بالفعل بهذه البيانات (الاسم أو رقم الهاتف أو البريد الإلكتروني)"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        sentOtpCode.isBlank() -> {
                                            val err =
                                                "يرجى الضغط على زر (إرسال OTP) أولاً لإرسال رمز التحقق إلى بريدك الإلكتروني الحقيقي (Gmail)"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        enteredOtpCode.trim() != sentOtpCode || !isEmailOtpVerified -> {
                                            val err =
                                                "رمز التحقق (OTP) غير صحيح! لا يمكن تفعيل الحساب إلا بفتح بريدك الإلكتروني (Gmail) وإدخال الكود الوارد في الرسالة الرسمية"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        regPassword.trim().length < 4 -> {
                                            val err = "كلمة المرور يجب ألا تقل عن 4 خانات"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        regPassword.trim() != regConfirmPassword.trim() -> {
                                            val err = "كلمة المرور وتأكيد كلمة المرور غير متطابقين"
                                            localValidationError = err
                                            topRedBannerMessage = err
                                        }
                                        else -> {
                                            localValidationError = null
                                            topRedBannerMessage = null
                                            onRegisterNewOfficialAccount(
                                                firstName.trim(),
                                                fatherName.trim(),
                                                grandfatherName.trim(),
                                                familyName.trim(),
                                                selectedCountry,
                                                phoneDigits.trim(),
                                                emailInput.trim(),
                                                regPassword.trim()
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WalletEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("register_official_account_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إتمام التسجيل وتوليد رقم الحساب المالي",
                                    style = MaterialTheme.typography.titleMedium,
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

/**
 * Prominent Red Top Notification/Banner displayed at the very top of the screen
 * when requesting an OTP verification code or alerting the user about verification/duplicate status.
 */
@Composable
fun TopRedVerificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("red_top_verification_banner"),
        shape = RoundedCornerShape(16.dp),
        color = WalletDanger,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق التنبيه",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Separate, Standalone Modal dedicated exclusively to Password Reset (إعادة تعيين كلمة المرور):
 * - Step A: Enter registered Email (or phone) & click "إرسال رمز التحقق" via dedicated EmailJS Reset template.
 * - Step B: After sending, reveals 6-digit OTP input (received in Gmail) + "كلمة المرور الجديدة" + "تأكيد كلمة المرور الجديدة".
 * - Step C: Upon confirmation, updates the password immediately in the Cloud Database & local storage so the user can log in right away.
 */
@Composable
fun StandalonePasswordResetModal(
    initialEmailOrIdentifier: String,
    onSendPasswordResetEmailOtp: (
        identifierOrEmail: String,
        onResult: (otpCode: String, resolvedEmail: String, statusMessageAr: String) -> Unit
    ) -> Unit,
    onResetPasswordWithVerifiedOtp: (
        identifierOrEmail: String,
        newPassword: String,
        onResult: (success: Boolean, messageAr: String) -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var resetEmailOrIdentifier by rememberSaveable { mutableStateOf(initialEmailOrIdentifier) }
    var resetSentOtpCode by rememberSaveable { mutableStateOf("") }
    var resetEnteredOtpCode by rememberSaveable { mutableStateOf("") }
    var resetNewPassword by rememberSaveable { mutableStateOf("") }
    var resetConfirmPassword by rememberSaveable { mutableStateOf("") }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var resetStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var topRedAlert by rememberSaveable { mutableStateOf<String?>(null) }
    var isResetOtpVerified by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("standalone_password_reset_modal"),
            shape = RoundedCornerShape(26.dp),
            color = WalletSurface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Red Top Notification Banner inside Password Reset Modal
                AnimatedVisibility(visible = !topRedAlert.isNullOrBlank()) {
                    TopRedVerificationBanner(
                        message = topRedAlert ?: "",
                        onDismiss = { topRedAlert = null }
                    )
                }

                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(WalletRoyalBlueDark, WalletRoyalBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إعادة تعيين كلمة المرور",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "نافذة استعادة الحساب السحابية المستقلة عبر Gmail",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletRoyalBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_password_reset_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Step A: Enter Registered Email & Send Verification Code
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WalletSubtleSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "أ) أدخل البريد الإلكتروني المسجل لإرسال رمز التحقق:",
                            style = MaterialTheme.typography.labelLarge,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = resetEmailOrIdentifier,
                            onValueChange = {
                                resetEmailOrIdentifier = it
                                resetSentOtpCode = ""
                                resetEnteredOtpCode = ""
                                isResetOtpVerified = false
                                resetStatusMessage = null
                                topRedAlert = null
                            },
                            label = { Text("البريد الإلكتروني المسجل (Gmail) أو رقم الهاتف") },
                            placeholder = { Text("example@gmail.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = WalletRoyalBlue
                                )
                            },
                            colors = authTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_password_email_input")
                        )

                        Button(
                            onClick = {
                                val target = resetEmailOrIdentifier.trim()
                                if (target.isBlank()) {
                                    val msg = "يرجى إدخال البريد الإلكتروني المسجل أو رقم الهاتف أولاً"
                                    resetStatusMessage = msg
                                    topRedAlert = msg
                                } else {
                                    resetSentOtpCode = ""
                                    resetEnteredOtpCode = ""
                                    isResetOtpVerified = false
                                    val sendingMsg =
                                        "تنبيه التحقق: جاري إرسال رمز إعادة تعيين كلمة المرور عبر EmailJS إلى بريدك الإلكتروني..."
                                    resetStatusMessage = sendingMsg
                                    topRedAlert = sendingMsg
                                    onSendPasswordResetEmailOtp(target) { otp, resolvedEmail, msg ->
                                        if (otp.isNotBlank()) {
                                            resetSentOtpCode = otp
                                            resetEnteredOtpCode = ""
                                            isResetOtpVerified = false
                                            if (resolvedEmail.isNotBlank()) {
                                                resetEmailOrIdentifier = resolvedEmail
                                            }
                                            topRedAlert =
                                                "تنبيه هام: تم إرسال رمز التحقق المكون من 6 أرقام إلى بريدك ($resetEmailOrIdentifier) — افتح Gmail وأدخله بالأسفل"
                                        } else {
                                            resetSentOtpCode = ""
                                            resetEnteredOtpCode = ""
                                            isResetOtpVerified = false
                                            topRedAlert = msg
                                        }
                                        resetStatusMessage = msg
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletRoyalBlueDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_password_reset_otp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (resetSentOtpCode.isBlank()) {
                                    "إرسال رمز التحقق"
                                } else {
                                    "إعادة إرسال رمز التحقق"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!resetStatusMessage.isNullOrBlank()) {
                            Text(
                                text = resetStatusMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (resetSentOtpCode.isNotBlank()) WalletEmerald else WalletDanger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Step B & C: After OTP is sent, show 6-digit OTP field + New Password + Confirm New Password + Submit
                AnimatedVisibility(visible = resetSentOtpCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = WalletRoyalBlue.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            WalletRoyalBlue.copy(alpha = 0.30f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ب) أدخل رمز التحقق (6 أرقام من Gmail) وكلمة المرور الجديدة:",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlueDark,
                                fontWeight = FontWeight.ExtraBold
                            )

                            OutlinedTextField(
                                value = resetEnteredOtpCode,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(6)
                                    resetEnteredOtpCode = digits
                                    isResetOtpVerified = (digits == resetSentOtpCode)
                                    if (isResetOtpVerified) {
                                        topRedAlert = null
                                    }
                                },
                                label = { Text("رمز التحقق (6 أرقام من رسالتك في Gmail)") },
                                placeholder = { Text("أدخل الكود المكون من 6 أرقام") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_password_otp_input")
                            )

                            OutlinedTextField(
                                value = resetNewPassword,
                                onValueChange = {
                                    resetNewPassword = it
                                    topRedAlert = null
                                },
                                label = { Text("كلمة المرور الجديدة") },
                                singleLine = true,
                                visualTransformation = if (showNewPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                        Icon(
                                            imageVector = if (showNewPassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null
                                        )
                                    }
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_new_password_input")
                            )

                            OutlinedTextField(
                                value = resetConfirmPassword,
                                onValueChange = {
                                    resetConfirmPassword = it
                                    topRedAlert = null
                                },
                                label = { Text("تأكيد كلمة المرور الجديدة") },
                                singleLine = true,
                                visualTransformation = if (showNewPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_confirm_password_input")
                            )

                            Button(
                                onClick = {
                                    val cleanNewPass = resetNewPassword.trim()
                                    val cleanConfirmPass = resetConfirmPassword.trim()
                                    when {
                                        resetSentOtpCode.isBlank() || resetEnteredOtpCode.trim() != resetSentOtpCode -> {
                                            val err =
                                                "كود الاستعادة غير صحيح! يرجى فتح بريدك الإلكتروني (Gmail) ونسخ الرمز المكون من 6 أرقام بدقة"
                                            resetStatusMessage = err
                                            topRedAlert = err
                                        }
                                        cleanNewPass.length < 4 -> {
                                            val err = "يرجى إدخال كلمة مرور جديدة (4 خانات على الأقل)"
                                            resetStatusMessage = err
                                            topRedAlert = err
                                        }
                                        cleanNewPass != cleanConfirmPass -> {
                                            val err = "كلمتا المرور الجديدتان غير متطابقتين"
                                            resetStatusMessage = err
                                            topRedAlert = err
                                        }
                                        else -> {
                                            onResetPasswordWithVerifiedOtp(
                                                resetEmailOrIdentifier.trim(),
                                                cleanNewPass
                                            ) { ok, msg ->
                                                resetStatusMessage = msg
                                                if (!ok) {
                                                    topRedAlert = msg
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WalletEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("confirm_password_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تأكيد وتحديث كلمة المرور في السحابة ✓",
                                    style = MaterialTheme.typography.labelLarge,
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

/**
 * Modal / Bottom Sheet containing all World Countries with:
 * (Flag + Country Name + International Dial Code e.g. +967, +966)
 * and a Live Search Bar filtering by Country Name or Dial Code.
 */
@Composable
fun WorldCountryPickerModal(
    selectedCountry: WorldCountry,
    onSelectCountry: (WorldCountry) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        WorldCountriesCatalog.search(searchQuery)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 16.dp)
                .testTag("country_picker_modal"),
            shape = RoundedCornerShape(26.dp),
            color = WalletSurface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "اختر الدولة ومفتاح الاتصال الدولي",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ابحث باسم الدولة أو مفتاح الاتصال (مثل +967 أو +966)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث حي باسم الدولة أو رمز المفتاح (+967)") },
                    placeholder = { Text("اكتب اسم الدولة أو +967...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = WalletRoyalBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث"
                                )
                            }
                        }
                    },
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("country_live_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCountries, key = { "${it.isoCode}_${it.dialCode}" }) { country ->
                        val isSelected = country.isoCode == selectedCountry.isoCode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectCountry(country) }
                                .testTag("country_item_${country.isoCode.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) WalletRoyalBlue.copy(alpha = 0.10f) else WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) WalletRoyalBlue else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = country.flag,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = country.nameAr,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = country.nameEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                androidx.compose.runtime.CompositionLocalProvider(
                                    LocalLayoutDirection provides LayoutDirection.Ltr
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) WalletRoyalBlue else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Text(
                                            text = country.dialCode,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = if (isSelected) Color.White else WalletRoyalBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Official Recipient Verification Modal (نافذة مراجعة المستلم):
 * Shows:
 * - Full Quadruple Name (الاسم الرباعي الكامل للشخص المستلم المسجل في النظام)
 * - Recipient Account ID & Phone Number
 * - Transfer Amount and Currency
 * - Two clear buttons: [إتمام] and [إلغاء]
 */
@Composable
fun RecipientVerificationModal(
    verification: PendingTransferVerification,
    onConfirmProceedToAuth: () -> Unit,
    onCancelTransfer: () -> Unit
) {
    Dialog(onDismissRequest = onCancelTransfer) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, WalletRoyalBlue, RoundedCornerShape(26.dp))
                .testTag("recipient_verification_modal"),
            shape = RoundedCornerShape(26.dp),
            color = WalletSurface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(WalletRoyalBlue.copy(alpha = 0.12f))
                        .border(1.5.dp, WalletRoyalBlue.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = WalletRoyalBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "مراجعة بيانات المستلم قبل التحويل",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تأكد من الاسم الرباعي الكامل للمستلم والمبلغ المراد تحويله",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Full Quadruple Name Highlight Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(WalletEmerald.copy(alpha = 0.10f))
                        .border(1.5.dp, WalletEmerald.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الاسم الرباعي الكامل للمستلم المسجل في النظام:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verification.recipientAccount.fullQuadName,
                        style = MaterialTheme.typography.titleLarge,
                        color = WalletEmerald,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("verified_recipient_quad_name")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transfer & Recipient Details Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WalletSubtleSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ المراد إرساله:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "${verification.formattedAmount} ${verification.currency.code}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("verified_transfer_amount")
                        )
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("معرف الحساب المالي (Account ID):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = verification.recipientAccount.accountId,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم هاتف المستلم:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "${verification.recipientAccount.countryFlag} ${verification.recipientAccount.formattedInternationalPhone}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مصدر التحقق من الاسم الرباعي:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = if (verification.isOnlineAvailable) {
                                "السحابة الموحدة (Live Cloud Lookup ✓)"
                            } else {
                                "الدليل المحلي + الموجات الصوتية (Offline)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (verification.isOnlineAvailable) WalletEmerald else WalletAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("آلية التنفيذ المزدوجة:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = if (verification.isOnlineAvailable) {
                                "تحويل سحابي ذري فوري (Atomic Cloud + Ultrasonic)"
                            } else {
                                "تحويل صوتي أوفلاين + مزامنة تلقائية"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Two Clear Action Buttons: [إتمام] and [إلغاء]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelTransfer,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletDanger.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(52.dp)
                            .testTag("cancel_recipient_transfer_button")
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.titleMedium,
                            color = WalletDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirmProceedToAuth,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletRoyalBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("confirm_recipient_transfer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إتمام",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Celebratory Modal shown immediately after registering a new Official Account,
 * highlighting the newly generated 5+ digit numeric Account ID.
 */
@Composable
fun NewlyRegisteredAccountModal(
    account: OfficialAccountProfile,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, WalletEmerald, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = WalletSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(WalletEmerald.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = WalletEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تم إنشاء حسابك الرسمي بنجاح! 🎉",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = account.fullQuadName,
                    style = MaterialTheme.typography.titleMedium,
                    color = WalletRoyalBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = WalletRoyalBlue.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "المعرف المالي الفريد الخاص بك (Account ID):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = account.accountId,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "يرتبط هذا الرقم بحسابك بجانب رقم هاتفك (${account.formattedInternationalPhone}) لاستقبال التحويلات المباشرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("بدء استخدام المحفظة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WalletRoyalBlue,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = WalletRoyalBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = WalletRoyalBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
