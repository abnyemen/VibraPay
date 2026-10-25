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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.WalletCurrency
import com.example.security.HardwareSecurityStatus
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletNavyStart
import com.example.ui.theme.WalletPrimary
import com.example.ui.theme.WalletSecondary
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface
import com.example.ui.theme.WalletTealEnd
import java.util.Locale

@Composable
fun WalletManagerCard(
    selectedCurrency: WalletCurrency,
    currentBalance: Double,
    allBalances: Map<WalletCurrency, Double>,
    isWalletPinEnabled: Boolean,
    walletPin: String,
    hardwareSecurityStatus: HardwareSecurityStatus,
    lastTopUpBanner: String?,
    onSelectCurrency: (WalletCurrency) -> Unit,
    onTopUpAmount: (Double) -> Unit,
    onToggleWalletPinLock: (Boolean) -> Unit,
    onOpenPinSetupDialog: () -> Unit,
    onSimulateRootTamperAttack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wallet_manager_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Multi-Currency Accounts Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                .background(WalletPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = WalletPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "حسابات العملات في المحفظة",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اختر العملة الافتراضية للإرسال والاستلام",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الرصيد النشط",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatCurrencyAmount(currentBalance)} ${selectedCurrency.code}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WalletPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("wallet_card_balance_value")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Currency Account Cards (YER | SAR | USD)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletCurrency.entries.forEach { currency ->
                        val isSelected = currency == selectedCurrency
                        val bal = allBalances[currency] ?: currency.defaultInitialBalance

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectCurrency(currency) }
                                .testTag("currency_tab_${currency.code.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) WalletPrimary.copy(alpha = 0.08f) else WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) WalletPrimary else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currency.nameAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isSelected) "الحساب النشط حالياً ✓" else "اضغط للتفعيل كعملة رئيسية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) WalletPrimary else TextSecondary
                                    )
                                }

                                Text(
                                    text = "${formatCurrencyAmount(bal)} ${currency.code}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = if (isSelected) WalletPrimary else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Balance Deposit & Top-Up Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            var customDepositInput by remember(selectedCurrency) { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WalletSecondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = null,
                            tint = WalletSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إيداع وتغذية الرصيد (${selectedCurrency.nameAr})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أدخل المبلغ أو اختر من فئات الإيداع السريع",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customDepositInput,
                        onValueChange = { customDepositInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("مبلغ الإيداع (${selectedCurrency.code})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WalletPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = WalletPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = WalletPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = WalletSubtleSurface,
                            unfocusedContainerColor = WalletSubtleSurface
                        ),
                        modifier = Modifier.weight(1.3f)
                    )

                    val defaultTopUp = selectedCurrency.quickTopUpAmounts.getOrElse(1) { 500.0 }
                    val depositValue = customDepositInput.toDoubleOrNull() ?: defaultTopUp

                    Button(
                        onClick = {
                            onTopUpAmount(depositValue)
                            customDepositInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("wallet_topup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إيداع",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCurrency.quickTopUpAmounts.forEachIndexed { idx, amt ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTopUpAmount(amt) }
                                .testTag("topup_chip_$idx"),
                            shape = RoundedCornerShape(10.dp),
                            color = WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${formatCompactAmount(amt)} ${selectedCurrency.code}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = WalletSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !lastTopUpBanner.isNullOrBlank()) {
                    lastTopUpBanner?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WalletPrimary.copy(alpha = 0.1f))
                                .border(1.dp, WalletPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WalletPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = WalletPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Total Hardware Keystore, SQLCipher & Cloud Security Architecture Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hardware_security_architecture_card"),
            shape = RoundedCornerShape(20.dp),
            color = WalletSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, WalletPrimary.copy(alpha = 0.35f))
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WalletPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = WalletPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "درع التشفير الهاردويري والسحابي المطلق",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Keystore TEE • SQLCipher 256-bit • R8 Obfuscation",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WalletPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (hardwareSecurityStatus.isSignatureValid) "مؤمّن 100% ✓" else "تحذير أمني",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hardwareSecurityStatus.isSignatureValid) WalletPrimary else WalletDanger,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletSubtleSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• المرجع السحابي المركزي: Atomic Cloud Transactions (منع التعديل المحلي للأرصدة)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• شريحة الأمان الصلبة: ${hardwareSecurityStatus.encryptionCipherSuite}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• تشفير قاعدة البيانات المحلية: ${hardwareSecurityStatus.sqlCipherSuite}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• حماية الكود والبيانات: ${hardwareSecurityStatus.manifestSecuritySummaryAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• ختم Keystore HMAC-SHA256: ${hardwareSecurityStatus.currentLedgerHmacHex.take(24)}...",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = WalletSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المصادقة الإجبارية قبل التحويل (Biometric / PIN)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hardwareSecurityStatus.biometricStatusLabelAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenPinSetupDialog,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("wallet_card_configure_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إدارة PIN", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareTamperFreezeScreen(
    securityStatus: HardwareSecurityStatus,
    onHardwareFactoryReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalletSubtleSurface)
            .padding(20.dp)
            .testTag("hardware_tamper_freeze_screen"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, WalletDanger, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
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
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(WalletDanger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GppBad,
                        contentDescription = null,
                        tint = WalletDanger,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "تم إيقاف المحفظة مؤقتاً لحماية أموالك",
                    style = MaterialTheme.typography.titleLarge,
                    color = WalletDanger,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = securityStatus.tamperReasonAr
                        ?: "تم رصد تغير غير مصرح به في بيانات الرصيد المحفوظة على الجهاز.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onHardwareFactoryReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("hardware_reset_from_freeze_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعادة ضبط المحفظة واستعادة النظام",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WalletPinSetupDialog(
    currentPin: String,
    onSavePin: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf(currentPin) }
    var enableLockNow by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
                .testTag("wallet_pin_setup_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = WalletSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WalletPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = WalletPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "تعيين رمز PIN للمحفظة",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اختر رمزاً سرياً من 4 إلى 6 أرقام لتأمين عمليات الدفع",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { newVal ->
                        pinInput = newVal.filter { it.isDigit() }.take(6)
                        errorMsg = null
                    },
                    label = { Text("رمز PIN الجديد (4-6 أرقام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WalletPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = WalletPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = WalletPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = WalletSubtleSurface,
                        unfocusedContainerColor = WalletSubtleSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_text_field")
                )

                errorMsg?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = WalletDanger,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفعيل القفل قبل كل عملية دفع",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = enableLockNow,
                        onCheckedChange = { enableLockNow = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("إلغاء", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (pinInput.length < 4) {
                                errorMsg = "يرجى إدخال 4 أرقام على الأقل"
                            } else {
                                onSavePin(pinInput, enableLockNow)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("save_wallet_pin_button")
                    ) {
                        Text(
                            text = "حفظ الرمز",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletPinAuthDialog(
    actionTitleAr: String,
    errorMessage: String?,
    isTransactionBiometricEnabled: Boolean = true,
    onRequestBiometricPrompt: () -> Unit,
    onVerifyPin: (String) -> Unit,
    onCancel: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("wallet_pin_auth_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = WalletSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(WalletPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = WalletPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "التأكيد الأمني لإتمام العملية",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actionTitleAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalletSecondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isTransactionBiometricEnabled) {
                    OutlinedButton(
                        onClick = onRequestBiometricPrompt,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("biometric_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = WalletSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التحقق وإتمام التحويل ببصمة الإصبع",
                            style = MaterialTheme.typography.labelLarge,
                            color = WalletSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Visual PIN Dots Display
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotCount = maxOf(4, enteredPin.length)
                        for (i in 0 until dotCount) {
                            val filled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) WalletPrimary else WalletSubtleSurface
                                    )
                                    .border(
                                        1.5.dp,
                                        if (filled) WalletPrimary else BorderSubtle,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { newVal ->
                        enteredPin = newVal.filter { it.isDigit() }.take(6)
                    },
                    label = { Text("أو أدخل رمز PIN (الافتراضي: 1234)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WalletPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = WalletPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = WalletPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = WalletSubtleSurface,
                        unfocusedContainerColor = WalletSubtleSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_pin_verify_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Clean Numeric Keypad
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "⌫")
                        )
                        rows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowKeys.forEach { key ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                when (key) {
                                                    "C" -> enteredPin = ""
                                                    "⌫" -> if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                    else -> if (enteredPin.length < 6) {
                                                        enteredPin += key
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = WalletSubtleSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = when (key) {
                                                    "C" -> WalletDanger
                                                    "⌫" -> WalletAmber
                                                    else -> TextPrimary
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WalletDanger.copy(alpha = 0.1f))
                            .border(1.dp, WalletDanger.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                            .testTag("wallet_pin_error_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = WalletDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = WalletDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(48.dp)
                            .testTag("cancel_wallet_pin_button")
                    ) {
                        Text("إلغاء", color = TextSecondary)
                    }

                    Button(
                        onClick = { onVerifyPin(enteredPin) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("confirm_wallet_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأكيد الدفع",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatCurrencyAmount(value: Double): String {
    return if (value >= 1000.0 && value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value)
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}

private fun formatCompactAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value)
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}
