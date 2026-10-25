package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.BurnedTokenInfo
import com.example.data.OfficialAccountProfile
import com.example.data.WalletCurrency
import com.example.protocol.AcousticMode
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SidebarDarkBg
import com.example.ui.theme.SidebarDarkBorder
import com.example.ui.theme.SidebarDarkCard
import com.example.ui.theme.SidebarDarkItemSurface
import com.example.ui.theme.SidebarTextPrimary
import com.example.ui.theme.SidebarTextSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletPurple
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface

/**
 * Composite icon: Fingerprint accompanied by Lock badge ("مصحوبة بأيقونة البصمة والقفل").
 */
@Composable
fun BiometricWithLockIcon(
    primaryTint: Color = WalletRoyalBlue,
    badgeBgColor: Color = SidebarDarkCard,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(38.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primaryTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "بصمة تسجيل الدخول",
                tint = primaryTint,
                modifier = Modifier.size(21.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(17.dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(1.dp, primaryTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "أيقونة القفل",
                tint = primaryTint,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/**
 * Composite icon: Fingerprint accompanied by Checkmark badge ("مصحوبة بأيقونة البصمة وعلامة الصح").
 */
@Composable
fun BiometricWithCheckIcon(
    primaryTint: Color = WalletEmerald,
    badgeBgColor: Color = SidebarDarkCard,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(38.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primaryTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "بصمة تأكيد العمليات",
                tint = primaryTint,
                modifier = Modifier.size(21.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(17.dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(1.dp, primaryTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "علامة الصح",
                tint = primaryTint,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    activeAccount: OfficialAccountProfile?,
    hasMicPermission: Boolean,
    selectedCurrency: WalletCurrency,
    isLoginBiometricEnabled: Boolean,
    isTransactionBiometricEnabled: Boolean,
    isWalletPinEnabled: Boolean,
    walletPin: String,
    appThemeMode: AppThemeMode,
    acousticMode: AcousticMode,
    smartAutoRetryEnabled: Boolean,
    highPassFilterEnabled: Boolean,
    nfcProximityMode: Boolean,
    burnedTokens: List<BurnedTokenInfo>,
    onOpenAuthModal: (registerMode: Boolean) -> Unit,
    onLogoutToSmartLogin: () -> Unit,
    onSelectCurrency: (WalletCurrency) -> Unit,
    onRequestMicPermission: () -> Unit,
    onToggleLoginBiometric: (Boolean) -> Unit,
    onToggleTransactionBiometric: (Boolean) -> Unit,
    onToggleWalletPinLock: (Boolean) -> Unit,
    onOpenPinSetupDialog: () -> Unit,
    onOpenChangePasswordDialog: () -> Unit,
    onOpenThemeSelectorDialog: () -> Unit,
    onSelectAppThemeMode: (AppThemeMode) -> Unit,
    onSelectAcousticMode: (AcousticMode) -> Unit,
    onToggleSmartAutoRetry: (Boolean) -> Unit,
    onToggleHighPassFilter: (Boolean) -> Unit,
    onToggleNfcProximityMode: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetWalletAndTokens: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Page Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(WalletRoyalBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = WalletRoyalBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "قسم الإعدادات ومركز الأمان المتقدم",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "نظام الحماية بالبصمة الحقيقية (BiometricPrompt)، إدارة الرموز، ومظهر التطبيق",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // =========================================================================
        // 0. بطاقة الحساب الرسمي والمعرف المالي الفريد (Official Account & ID)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_official_account_card"),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بيانات الحساب الرسمي المسجل",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    activeAccount?.let { acc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WalletEmerald.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WalletEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ID: ${acc.accountId}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = WalletEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (activeAccount != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "الاسم الرباعي: ${activeAccount.fullQuadName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "رقم الهاتف الدولي: ${activeAccount.countryFlag} ${activeAccount.formattedInternationalPhone} (${activeAccount.countryNameAr})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "البريد الإلكتروني الموثق: ${activeAccount.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "المعرف المالي الفريد (Account ID): ${activeAccount.accountId} (أرقام فقط)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenAuthModal(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletRoyalBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("settings_open_register_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حساب رسمي جديد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onLogoutToSmartLogin,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletRoyalBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_logout_smart_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دخول ذكي / تبديل",
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 1. نظام الحماية المتقدم والبصمة الحقيقية (Biometric & Security)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_biometric_security_card"),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = WalletRoyalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نظام الحماية المتقدم والبصمة الحقيقية (Biometric & Security)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Hardware-backed Keystore Active Badge
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hardware_keystore_badge"),
                    shape = RoundedCornerShape(14.dp),
                    color = WalletEmerald.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        WalletEmerald.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = WalletEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BiometricPrompt & Hardware Keystore: مفعل ونشط",
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مستشعر البصمة الرسمي مرتبط بشريحة المعالج (HMAC-SHA256)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Option A: استخدام البصمة لتسجيل الدخول (مصحوبة بأيقونة البصمة والقفل)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BiometricWithLockIcon(
                            primaryTint = WalletRoyalBlue,
                            badgeBgColor = WalletSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "استخدام البصمة لتسجيل الدخول",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مصادقة البصمة الحقيقية عبر BiometricPrompt للدخول الفوري للحساب",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isLoginBiometricEnabled,
                        onCheckedChange = onToggleLoginBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletRoyalBlue
                        ),
                        modifier = Modifier.testTag("login_biometric_switch")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Option B: استخدام البصمة لتأكيد العمليات (مصحوبة بأيقونة البصمة وعلامة الصح)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BiometricWithCheckIcon(
                            primaryTint = WalletEmerald,
                            badgeBgColor = WalletSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "استخدام البصمة لتأكيد العمليات",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مصادقة البصمة الحقيقية عبر BiometricPrompt لاعتماد التحويلات المالية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isTransactionBiometricEnabled,
                        onCheckedChange = onToggleTransactionBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletEmerald
                        ),
                        modifier = Modifier.testTag("tx_biometric_switch")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Option C: تغيير رمز التأكيد (PIN Code) مع أيقونة التحقق الخاصة به
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenPinSetupDialog() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WalletRoyalBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "أيقونة التحقق لرمز التأكيد",
                                tint = WalletRoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تغيير رمز التأكيد (PIN Code)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الرمز الحالي: ${"*".repeat(walletPin.length.coerceAtLeast(4))} • حماية العمليات المالية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenPinSetupDialog,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue),
                        modifier = Modifier.testTag("change_wallet_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تغيير رمز التأكيد",
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Option D: تغيير كلمة المرور مع أيقونة القفل لتحديث كلمة مرور الحساب السحابي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenChangePasswordDialog() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WalletAmber.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "أيقونة القفل لتغيير كلمة المرور",
                                tint = WalletAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تغيير كلمة المرور",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحديث كلمة مرور الحساب السحابي الموثق بشكل فوري وآمن",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenChangePasswordDialog,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletAmber),
                        modifier = Modifier.testTag("settings_change_password_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = WalletAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تغيير كلمة المرور",
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Microphone Sensor Permission Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (hasMicPermission) WalletEmerald else WalletAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "صلاحية الميكروفون للاستلام الصوتي",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasMicPermission) {
                                    "الصلاحية مفعلة ومحمية بنظام Active-Only ✓"
                                } else {
                                    "اضغط لتفعيل الصلاحية وتشغيل رادار التلامس الصوتي"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasMicPermission) WalletEmerald else WalletAmber
                            )
                        }
                    }

                    if (!hasMicPermission) {
                        Button(
                            onClick = onRequestMicPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletRoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_grant_mic_button")
                        ) {
                            Text("تفعيل", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WalletEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. نظام مظهر التطبيق (App Theme Control)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_app_theme_card"),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenThemeSelectorDialog() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WalletPurple.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "مظهر التطبيق",
                                tint = WalletPurple,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مظهر التطبيق (App Theme Control)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الوضع الحالي المحفوظ: ${appThemeMode.titleAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenThemeSelectorDialog,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletPurple),
                        modifier = Modifier.testTag("settings_open_theme_dialog_button")
                    ) {
                        Text(
                            text = "تغيير المظهر",
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Direct 3-mode radio cards inside SettingsScreen
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = mode == appThemeMode
                    val modeIcon = when (mode) {
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectAppThemeMode(mode) }
                            .testTag("settings_theme_option_${mode.code.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) WalletRoyalBlue.copy(alpha = 0.14f) else WalletSubtleSurface,
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
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = mode.titleAr,
                                    tint = if (isSelected) WalletRoyalBlue else TextSecondary,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.titleAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                    Text(
                                        text = mode.subtitleAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Default.RadioButtonChecked
                                } else {
                                    Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isSelected) WalletEmerald else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. قسم تفضيلات الصوت والتلامس (Audio & Contact Preferences)
        // =========================================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = WalletRoyalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "قسم تفضيلات الصوت والتلامس (Audio & Contact Preferences)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Toggle 1: NFC Proximity < 10cm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "وضع التلامس الآمن في الزحام (NFC Proximity < 10cm)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "يرفض أي إشارات بعيدة ويشترط تلاصق الهاتفين بقوة إشارة (>= 205/255)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = nfcProximityMode,
                        onCheckedChange = onToggleNfcProximityMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletRoyalBlue
                        ),
                        modifier = Modifier.testTag("settings_nfc_proximity_switch")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Toggle 2: High-Pass Noise Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مرشح تصفية الضوضاء البيئية (High-Pass Filter)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "حذف الأصوات البشرية وضجيج الشارع والتركيز على الترددات فوق الصوتية فقط",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = highPassFilterEnabled,
                        onCheckedChange = onToggleHighPassFilter,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletRoyalBlue
                        ),
                        modifier = Modifier.testTag("settings_high_pass_switch")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Toggle 3: Smart Auto-Retry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الإرسال المتسلسل الذكي وأتمتة الـ OTT",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "توليد رمز OTT_ID وتشفير عشوائي فريد لكل عملية تلقائياً في الخلفية",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = smartAutoRetryEnabled,
                        onCheckedChange = onToggleSmartAutoRetry,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletRoyalBlue
                        ),
                        modifier = Modifier.testTag("settings_smart_retry_switch")
                    )
                }
            }
        }

        // =========================================================================
        // 4. قسم الحساب والعملات (Account & Currencies)
        // =========================================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = WalletEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "قسم الحساب والعملات (Account & Currencies)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "تغيير العملة الافتراضية للمحفظة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WalletCurrency.entries.forEach { currency ->
                        val isSelected = currency == selectedCurrency
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectCurrency(currency) }
                                .testTag("settings_currency_${currency.code.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) WalletRoyalBlue else WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) WalletRoyalBlue else BorderSubtle
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${currency.symbolAr} • ${currency.code}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currency.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "إدارة سجل العمليات المحروقة محلياً (${burnedTokens.size} رمز OTT محروق):",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val exportBody = if (burnedTokens.isEmpty()) {
                                "سجل العمليات المحروقة في VibraPay فارغ حالياً."
                            } else {
                                buildString {
                                    appendLine("📋 سجل رموز العمليات المحروقة (VibraPay Anti-Replay Log)")
                                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    burnedTokens.forEachIndexed { idx, token ->
                                        appendLine("${idx + 1}. OTT #${token.ottId} | ${token.operationTypeAr} | ${token.amount} | ${token.recordedTime}")
                                    }
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "سجل العمليات المحروقة VibraPay")
                                putExtra(Intent.EXTRA_TEXT, exportBody)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "تصدير سجل العمليات المحروقة")
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletRoyalBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_burned_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصدير السجل",
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onClearHistory()
                            onResetWalletAndTokens()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletDanger.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletDanger),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_reset_wallet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مسح السجل المحروق",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. قسم حول التطبيق (About VibraPay)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_vibrapay_card"),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حول التطبيق (About VibraPay)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WalletRoyalBlue.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = "VibraPay v2.0 Hybrid SECURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "نظام محفظة إلكترونية رسمية متكاملة (Hybrid Official E-Wallet) تعمل بالإنترنت (Online) وبدون إنترنت عبر الموجات فوق الصوتية (Offline Ultrasonic)، مع معرف مالي فريد (Account ID) وتفويض أمني مزدوج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Dialog for changing the Cloud Account Password ("تغيير كلمة المرور").
 */
@Composable
fun ChangeAccountPasswordDialog(
    activeAccount: OfficialAccountProfile?,
    onConfirmChangePassword: (
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: (Boolean, String) -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SidebarDarkBorder, RoundedCornerShape(24.dp))
                .testTag("change_account_password_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = SidebarDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WalletAmber.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "تغيير كلمة المرور",
                            tint = WalletAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تغيير كلمة المرور",
                            style = MaterialTheme.typography.titleMedium,
                            color = SidebarTextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "تحديث كلمة مرور الحساب السحابي (${activeAccount?.accountId ?: ""})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SidebarTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        feedbackMessage = null
                    },
                    label = { Text("كلمة المرور الحالية") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                            Icon(
                                imageVector = if (showCurrentPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = SidebarTextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SidebarTextPrimary,
                        unfocusedTextColor = SidebarTextPrimary,
                        focusedBorderColor = WalletRoyalBlue,
                        unfocusedBorderColor = SidebarDarkBorder,
                        focusedLabelColor = WalletRoyalBlue,
                        unfocusedLabelColor = SidebarTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_password_current_input")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        feedbackMessage = null
                    },
                    label = { Text("كلمة المرور الجديدة (6+ خانات)") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPass = !showNewPass }) {
                            Icon(
                                imageVector = if (showNewPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = SidebarTextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SidebarTextPrimary,
                        unfocusedTextColor = SidebarTextPrimary,
                        focusedBorderColor = WalletEmerald,
                        unfocusedBorderColor = SidebarDarkBorder,
                        focusedLabelColor = WalletEmerald,
                        unfocusedLabelColor = SidebarTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_password_new_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        feedbackMessage = null
                    },
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SidebarTextPrimary,
                        unfocusedTextColor = SidebarTextPrimary,
                        focusedBorderColor = WalletEmerald,
                        unfocusedBorderColor = SidebarDarkBorder,
                        focusedLabelColor = WalletEmerald,
                        unfocusedLabelColor = SidebarTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_password_confirm_input")
                )

                feedbackMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess) WalletEmerald.copy(alpha = 0.15f) else WalletDanger.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) WalletEmerald else WalletDanger,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirmChangePassword(currentPassword, newPassword, confirmPassword) { ok, msg ->
                                isSuccess = ok
                                feedbackMessage = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletRoyalBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_change_password_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث كلمة المرور", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SidebarDarkBorder),
                        modifier = Modifier.testTag("cancel_change_password_button")
                    ) {
                        Text("إلغاء", color = SidebarTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for selecting the App Theme Mode ("مظهر التطبيق"):
 * 1) الوضع المظلم (Dark Mode)
 * 2) الوضع الفاتح (Light Mode)
 * 3) حسب وضع الهاتف العام (System Default)
 */
@Composable
fun AppThemeSelectorDialog(
    currentMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SidebarDarkBorder, RoundedCornerShape(24.dp))
                .testTag("app_theme_selector_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = SidebarDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WalletPurple.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "مظهر التطبيق",
                            tint = WalletPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مظهر التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            color = SidebarTextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "اختر المظهر المفضل وسيتم حفظه محلياً بشكل دائم",
                            style = MaterialTheme.typography.labelSmall,
                            color = SidebarTextSecondary
                        )
                    }
                }

                AppThemeMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    val modeIcon = when (mode) {
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMode(mode) }
                            .testTag("theme_dialog_option_${mode.code.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) WalletRoyalBlue.copy(alpha = 0.22f) else SidebarDarkItemSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) WalletRoyalBlue else SidebarDarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    tint = if (isSelected) WalletEmerald else SidebarTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.titleAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SidebarTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                    Text(
                                        text = mode.subtitleAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SidebarTextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Default.RadioButtonChecked
                                } else {
                                    Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isSelected) WalletEmerald else SidebarTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SidebarDarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_theme_selector_dialog_button")
                ) {
                    Text("إغلاق", color = SidebarTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
