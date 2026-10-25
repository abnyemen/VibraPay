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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfficialAccountProfile
import com.example.protocol.EncodedPacket
import com.example.protocol.MicroOpType
import com.example.ui.TemplateTab
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface

@Composable
fun QuickTemplatesTransmitterCard(
    selectedTemplate: TemplateTab,
    microOpType: MicroOpType,
    recipientDigitsInput: String,
    liveCloudSearchRecipient: OfficialAccountProfile? = null,
    recipientLookupError: String?,
    directoryAccounts: List<OfficialAccountProfile>,
    activeSenderAccountId: String?,
    invoiceStore: String,
    invoiceAmount: String,
    invoiceCurrency: String,
    encodedPacket: EncodedPacket,
    currentBurstIndex: Int,
    totalPlannedBursts: Int,
    burstStatusTextAr: String?,
    isTransmitting: Boolean,
    transmitProgress: Float,
    onSelectMicroOpType: (MicroOpType) -> Unit,
    onUpdateRecipientDigits: (String) -> Unit,
    onUpdateAmount: (String) -> Unit,
    onInitiateOfficialTransfer: () -> Unit,
    onTransmitCashierInvoice: () -> Unit,
    onStopTransmit: () -> Unit,
    onOpenQrScanner: () -> Unit = {},
    onOpenMyQrDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDirectTransferMode = microOpType == MicroOpType.PAYMENT
    val cleanRecipientDigits = recipientDigitsInput.filter { it.isDigit() }
    val amountValue = invoiceAmount.toDoubleOrNull() ?: 0.0
    val isReadyToTransfer = cleanRecipientDigits.length >= 5 && amountValue > 0.0
    val previewRecipient = liveCloudSearchRecipient ?: directoryAccounts.firstOrNull {
        cleanRecipientDigits.length >= 5 &&
            (it.accountId == cleanRecipientDigits ||
                it.phoneNumberLocal == cleanRecipientDigits ||
                it.fullPhoneDigits == cleanRecipientDigits)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transmitter_card"),
        shape = RoundedCornerShape(24.dp),
        color = WalletSurface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WalletRoyalBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = WalletRoyalBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "التحويل المالي السحابي المباشر (Online Cloud Transfer)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عملية ذرية مشفرة (Atomic Cloud Transaction) + مصادقة البصمة و Keystore",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Operation Mode Toggle: (تحويل أموال مباشر 🚀) vs (طلب فاتورة كاشير 🧾)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WalletSubtleSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMicroOpType(MicroOpType.PAYMENT) }
                        .testTag("micro_op_pay"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDirectTransferMode) WalletRoyalBlue else Color.Transparent,
                    shadowElevation = if (isDirectTransferMode) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحويل أموال مباشر 🚀",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDirectTransferMode) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMicroOpType(MicroOpType.SELLER_REQUEST) }
                        .testTag("micro_op_req"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isDirectTransferMode) WalletEmerald else Color.Transparent,
                    shadowElevation = if (!isDirectTransferMode) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "طلب فاتورة كاشير 🧾",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isDirectTransferMode) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. حقل إدخال المبلغ (Large Numeric Amount Input)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = WalletSubtleSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    WalletRoyalBlue.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. كمية المبلغ المراد تحويله ($invoiceCurrency)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = WalletEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تشفير و OTT تلقائي",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = invoiceAmount,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() || it == '.' }
                            onUpdateAmount(clean)
                        },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        suffix = {
                            Text(
                                text = invoiceCurrency,
                                style = MaterialTheme.typography.titleMedium,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WalletRoyalBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = WalletSurface,
                            unfocusedContainerColor = WalletSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Amount Chips
                    val quickPresets = listOf("100", "500", "1000", "5000", "10000")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateAmount(preset) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (invoiceAmount == preset) WalletRoyalBlue.copy(alpha = 0.14f) else WalletSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (invoiceAmount == preset) WalletRoyalBlue else BorderSubtle
                                )
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = if (invoiceAmount == preset) WalletRoyalBlue else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isDirectTransferMode) {
                // 2. حقل إدخال وجهة التحويل + زر "مسح رمز QR" للتعبئة التلقائية الفورية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. وجهة التحويل (رقم الهاتف أو المعرف المالي):",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Prominent "مسح رمز QR" Button inside the Transfer UI
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenQrScanner() }
                            .testTag("scan_qr_code_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = WalletEmerald,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح رمز QR",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مسح رمز QR",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientDigitsInput,
                    onValueChange = { rawInput ->
                        // Strictly enforce digits ONLY (numbers only, no letters!)
                        val digitsOnly = rawInput.filter { it.isDigit() }.take(16)
                        onUpdateRecipientDigits(digitsOnly)
                    },
                    label = { Text("رقم هاتف المستلم أو المعرف المالي (أرقام فقط)") },
                    placeholder = { Text("مثال: 620145 أو 771234567") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = WalletRoyalBlue
                        )
                    },
                    trailingIcon = {
                        Surface(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenQrScanner() }
                                .testTag("field_trailing_scan_qr_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = WalletRoyalBlue.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "مسح رمز QR للمستلم",
                                    tint = WalletRoyalBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = if (!recipientLookupError.isNullOrBlank()) {
                                recipientLookupError
                            } else {
                                "أدخل رقم الهاتف/المعرف يدوياً أو اضغط [مسح رمز QR] لتعبئة بيانات المستلم تلقائياً"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!recipientLookupError.isNullOrBlank()) WalletDanger else TextMuted,
                            fontWeight = if (!recipientLookupError.isNullOrBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WalletRoyalBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = WalletSubtleSurface,
                        unfocusedContainerColor = WalletSubtleSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipient_destination_digits_input")
                )

                // Live Cloud Search Result Preview Card (ميزة البحث السحابي الفوري للاسم الرباعي)
                AnimatedVisibility(visible = previewRecipient != null && cleanRecipientDigits.length >= 5) {
                    previewRecipient?.let { rec ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .testTag("live_cloud_search_preview_card"),
                            shape = RoundedCornerShape(14.dp),
                            color = WalletEmerald.copy(alpha = 0.09f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                WalletEmerald.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = WalletEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "البحث السحابي الفوري • مستلم موثق ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WalletEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = rec.fullQuadName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WalletRoyalBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "ID: ${rec.accountId}",
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

                // Quick Directory Recipients for 1-Tap Testing
                val otherAccounts = directoryAccounts.filter { it.accountId != activeSenderAccountId }.take(3)
                if (otherAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "مستفيدون مسجلون في النظام (اضغط للاختيار السريع):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        otherAccounts.forEach { acc ->
                            val isSelected = recipientDigitsInput == acc.accountId ||
                                recipientDigitsInput == acc.phoneNumberLocal
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateRecipientDigits(acc.accountId) }
                                    .testTag("quick_recipient_${acc.accountId}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) WalletEmerald.copy(alpha = 0.14f) else WalletSubtleSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) WalletEmerald else BorderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${acc.firstName} ${acc.familyName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "ID: ${acc.accountId}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = WalletRoyalBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Ultrasonic Broadcast Progress Bar (< 2 seconds)
            AnimatedVisibility(visible = isTransmitting || burstStatusTextAr != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WalletRoyalBlue.copy(alpha = 0.08f))
                        .border(1.dp, WalletRoyalBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    if (isTransmitting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalPlannedBursts > 1 && currentBurstIndex > 0) {
                                    "جاري البث فوق الصوتي المشفر ($currentBurstIndex/$totalPlannedBursts)..."
                                } else {
                                    "جاري تنفيذ التحويل والبث الصوتي..."
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = WalletRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(transmitProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = WalletEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { transmitProgress.coerceIn(0.02f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = WalletRoyalBlue,
                            trackColor = BorderSubtle
                        )
                    }
                    burstStatusTextAr?.let { msg ->
                        if (isTransmitting) Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("burst_status_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Button: [تحويل] (Opens Recipient Verification Modal) or [طلب فاتورة كاشير]
            if (!isTransmitting) {
                if (isDirectTransferMode) {
                    Button(
                        onClick = onInitiateOfficialTransfer,
                        enabled = isReadyToTransfer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletRoyalBlue,
                            contentColor = Color.White,
                            disabledContainerColor = BorderSubtle,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("transmit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "تحويل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isReadyToTransfer) {
                                    "مراجعة اسم المستلم الرباعي وإتمام تحويل $invoiceAmount $invoiceCurrency"
                                } else {
                                    "أدخل المبلغ ورقم هاتف أو معرف المستلم (5 أرقام فأكثر) للمتابعة"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReadyToTransfer) Color.White.copy(alpha = 0.92f) else TextMuted
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onTransmitCashierInvoice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("transmit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "بث طلب فاتورة كاشير 🧾",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "طلب مبلغ $invoiceAmount $invoiceCurrency عبر الموجات فوق الصوتية",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = onStopTransmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("stop_transmit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إيقاف البث الصوتي فوراً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
