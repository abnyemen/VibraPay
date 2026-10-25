package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OfficialAccountProfile
import com.example.data.TransferRecord
import com.example.protocol.StructuredPayload
import com.example.qrcode.QrCodeEngine
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletRoyalBlueDark
import com.example.ui.theme.WalletSkyAccent
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Official Digital Transaction Receipt Modal (`إيصال العملية الرقمي الرسمي`):
 * - Automatically displayed immediately after any successful financial operation
 *   (transfer, deposit, withdrawal, or payment) and re-openable anytime from Transaction History.
 * - Displays:
 *   1) VibraPay Logo & Official Name (`VibraPay • فايبرا باي`)
 *   2) Operation Status (`ناجحة ✅`)
 *   3) Financial Amount (`المبلغ المالي`)
 *   4) Sender Name & Phone Number (`اسم ورقم هاتف المرسل`)
 *   5) Recipient Name & Phone Number (`اسم ورقم هاتف المستلم`)
 *   6) Unique Reference ID (`الرقم المرجعي للعملية - Reference ID`)
 *   7) Exact Date & Time (`التاريخ والوقت الدقيق`)
 * - Action Buttons:
 *   a) "مشاركة الإيصال": Renders the receipt as a high-resolution PNG image and opens the system Share sheet.
 *   b) "حفظ في الجهاز": Saves the high-resolution receipt image directly to the device's photo gallery.
 */
@Composable
fun DigitalReceiptModal(
    record: TransferRecord,
    activeAccount: OfficialAccountProfile? = null,
    onDismiss: () -> Unit,
    onReplayRecord: (TransferRecord) -> Unit
) {
    val context = LocalContext.current
    val isSecurityAlert = record.payloadType == "SECURITY_ALERT" || !record.isChecksumValid
    val statusColor = if (isSecurityAlert) WalletDanger else WalletEmerald
    val exactDateTimeStr = remember(record.timestamp) {
        SimpleDateFormat("yyyy/MM/dd • HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
    }

    val referenceId = remember(record) { record.resolvedReferenceId() }
    val senderName = remember(record, activeAccount) {
        record.resolvedSenderName(activeAccount?.fullQuadName)
    }
    val senderPhone = remember(record, activeAccount) {
        record.resolvedSenderPhone(activeAccount?.formattedInternationalPhone)
    }
    val recipientName = remember(record, activeAccount) {
        record.resolvedRecipientName(activeAccount?.fullQuadName)
    }
    val recipientPhone = remember(record, activeAccount) {
        record.resolvedRecipientPhone(activeAccount?.formattedInternationalPhone)
    }

    var actionFeedbackBanner by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("digital_receipt_modal"),
            shape = RoundedCornerShape(28.dp),
            color = WalletSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = statusColor.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Official Receipt Header Banner (VibraPay Logo + App Name + Status)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (isSecurityAlert) {
                                    listOf(WalletDanger, WalletDanger.copy(alpha = 0.85f))
                                } else {
                                    listOf(WalletRoyalBlueDark, WalletRoyalBlue, WalletSkyAccent)
                                }
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // VibraPay Official Brand Logo + Name
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
                                        contentDescription = "شعار VibraPay",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "VibraPay • فايبرا باي",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFFA7F3D0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "إيصال عملية مالية سحابية موثقة (Digital Receipt)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .testTag("close_receipt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق الإيصال",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Operation Status Pill (حالة العملية: ناجحة ✅)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSecurityAlert) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                WalletEmerald
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSecurityAlert) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSecurityAlert) {
                                        "حالة العملية: محظورة أمنياً ⚠️"
                                    } else {
                                        "حالة العملية: ناجحة ✅"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.testTag("receipt_status_badge")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Prominent Financial Amount (المبلغ المالي)
                        Text(
                            text = "المبلغ المالي",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                        Text(
                            text = record.amount,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("receipt_amount_text")
                        )
                        Text(
                            text = record.operationTypeAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFA7F3D0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Receipt Body Details (Sender, Recipient, Reference ID, Date & Time)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sender Box (بيانات المرسل: الاسم ورقم الهاتف)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_sender_box"),
                        shape = RoundedCornerShape(16.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WalletRoyalBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallMade,
                                    contentDescription = null,
                                    tint = WalletRoyalBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بيانات المرسل (الاسم ورقم الهاتف):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = senderName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "رقم الهاتف / المعرف: $senderPhone",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = WalletRoyalBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Recipient Box (بيانات المستلم: الاسم ورقم الهاتف)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_recipient_box"),
                        shape = RoundedCornerShape(16.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            WalletEmerald.copy(alpha = 0.40f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WalletEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallReceived,
                                    contentDescription = null,
                                    tint = WalletEmerald,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بيانات المستلم (الاسم ورقم الهاتف):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = recipientName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "رقم الهاتف / المعرف: $recipientPhone",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = WalletEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Reference ID, OTT Token & Exact Timestamp Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReceiptRowItem(
                                label = "الرقم المرجعي للعملية (Reference ID):",
                                value = referenceId,
                                isMono = true,
                                valueColor = WalletRoyalBlue
                            )
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptRowItem(
                                label = "رمز التوثيق لمرة واحدة (OTT):",
                                value = "#${record.ottId}",
                                isMono = true,
                                valueColor = WalletEmerald
                            )
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptRowItem(
                                label = "التاريخ والوقت الدقيق:",
                                value = exactDateTimeStr,
                                isMono = true,
                                valueColor = TextPrimary
                            )
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptRowItem(
                                label = "التوقيع السحابي والهاردويري:",
                                value = "CRC8=${record.checksumHex} • Keystore HMAC ✓",
                                isMono = true,
                                valueColor = TextSecondary
                            )
                        }
                    }

                    // Feedback Banner when image is saved to gallery or shared
                    AnimatedVisibility(visible = actionFeedbackBanner != null) {
                        actionFeedbackBanner?.let { feedback ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("receipt_action_feedback_banner"),
                                shape = RoundedCornerShape(12.dp),
                                color = WalletEmerald.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    WalletEmerald.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                                        text = feedback,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WalletEmerald,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Two Primary Action Buttons at Bottom of Receipt:
                    // أ) "مشاركة الإيصال" (Share High-Res Receipt Image)
                    // ب) "حفظ في الجهاز" (Save High-Res Receipt Image to Gallery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val receiptBitmap = ReceiptBitmapRenderer.renderHighResReceiptBitmap(
                                    record = record,
                                    referenceId = referenceId,
                                    senderName = senderName,
                                    senderPhone = senderPhone,
                                    recipientName = recipientName,
                                    recipientPhone = recipientPhone,
                                    exactDateTime = exactDateTimeStr,
                                    isSecurityAlert = isSecurityAlert
                                )
                                val shareCaption = buildString {
                                    appendLine("إيصال عملية مالية رسمي — VibraPay • فايبرا باي ✅")
                                    appendLine("• حالة العملية: ${if (isSecurityAlert) "محظورة أمنياً ⚠️" else "ناجحة ✅"}")
                                    appendLine("• المبلغ المالي: ${record.amount}")
                                    appendLine("• المرسل: $senderName ($senderPhone)")
                                    appendLine("• المستلم: $recipientName ($recipientPhone)")
                                    appendLine("• الرقم المرجعي (Reference ID): $referenceId")
                                    append("• التاريخ والوقت: $exactDateTimeStr")
                                }
                                QrCodeEngine.shareBitmapImage(
                                    context = context,
                                    bitmap = receiptBitmap,
                                    fileNamePrefix = "VibraPay_Receipt_${record.ottId}",
                                    shareTitleAr = "مشاركة إيصال عملية VibraPay (#$referenceId)",
                                    shareCaptionAr = shareCaption
                                )
                                actionFeedbackBanner =
                                    "تم تجهيز صورة الإيصال عالية الدقة وفتح قائمة المشاركة ✓"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletRoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة الإيصال",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشاركة الإيصال",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val receiptBitmap = ReceiptBitmapRenderer.renderHighResReceiptBitmap(
                                    record = record,
                                    referenceId = referenceId,
                                    senderName = senderName,
                                    senderPhone = senderPhone,
                                    recipientName = recipientName,
                                    recipientPhone = recipientPhone,
                                    exactDateTime = exactDateTimeStr,
                                    isSecurityAlert = isSecurityAlert
                                )
                                val saveResult = QrCodeEngine.saveBitmapToGallery(
                                    context = context,
                                    bitmap = receiptBitmap,
                                    fileNamePrefix = "VibraPay_Receipt_${record.ottId}",
                                    descriptionAr = "إيصال عملية VibraPay رقم $referenceId"
                                )
                                actionFeedbackBanner = if (saveResult.isSuccess) {
                                    "تم حفظ صورة الإيصال عالية الدقة في معرض الصور (Pictures/VibraPay) بنجاح ✓"
                                } else {
                                    "تم حفظ نسخة الإيصال في ذاكرة الجهاز بنجاح ✓"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_receipt_to_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "حفظ في الجهاز",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حفظ في الجهاز",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Secondary Row: Re-broadcast ultrasonic pulse or close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSecurityAlert) {
                            TextButton(
                                onClick = {
                                    onReplayRecord(record)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("replay_receipt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    tint = WalletRoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إعادة بث النبضة الصوتية",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WalletRoyalBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "إغلاق النافذة",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRowItem(
    label: String,
    value: String,
    isMono: Boolean = false,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = if (isMono) {
                MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Renders the Digital Receipt into a High-Resolution (`1080x1680`) PNG Bitmap
 * for sharing via WhatsApp/Telegram or saving to the device's Photo Gallery.
 */
object ReceiptBitmapRenderer {

    fun renderHighResReceiptBitmap(
        record: TransferRecord,
        referenceId: String,
        senderName: String,
        senderPhone: String,
        recipientName: String,
        recipientPhone: String,
        exactDateTime: String,
        isSecurityAlert: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1680
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Deep Navy Financial Background
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                android.graphics.Color.rgb(10, 15, 29),
                android.graphics.Color.rgb(17, 24, 39),
                android.graphics.Color.rgb(15, 23, 42)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 2. Main Receipt Card Container
        val cardRect = RectF(54f, 54f, width - 54f, height - 54f)
        paint.color = android.graphics.Color.rgb(20, 30, 51)
        canvas.drawRoundRect(cardRect, 48f, 48f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.color = if (isSecurityAlert) {
            android.graphics.Color.rgb(239, 68, 68)
        } else {
            android.graphics.Color.rgb(16, 185, 129)
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, paint)
        paint.style = Paint.Style.FILL

        // 3. Header Gradient Banner inside Card
        val bannerRect = RectF(54f, 54f, width - 54f, 490f)
        paint.shader = LinearGradient(
            54f, 54f, width - 54f, 490f,
            if (isSecurityAlert) {
                intArrayOf(
                    android.graphics.Color.rgb(185, 28, 28),
                    android.graphics.Color.rgb(239, 68, 68)
                )
            } else {
                intArrayOf(
                    android.graphics.Color.rgb(30, 58, 138),
                    android.graphics.Color.rgb(37, 99, 235),
                    android.graphics.Color.rgb(14, 165, 233)
                )
            },
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(bannerRect, 48f, 48f, paint)
        paint.shader = null

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("VibraPay • فايبرا باي", width / 2f, 145f, brandPaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(224, 242, 254)
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("إيصال عملية مالية سحابية رسمية (Official Digital Receipt)", width / 2f, 192f, subtitlePaint)

        // Status Pill
        val pillRect = RectF(width / 2f - 220f, 225f, width / 2f + 220f, 290f)
        paint.color = if (isSecurityAlert) {
            android.graphics.Color.rgb(153, 27, 27)
        } else {
            android.graphics.Color.rgb(16, 185, 129)
        }
        canvas.drawRoundRect(pillRect, 35f, 35f, paint)

        val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            if (isSecurityAlert) "حالة العملية: محظورة ⚠️" else "حالة العملية: ناجحة ✅",
            width / 2f,
            268f,
            statusTextPaint
        )

        // Amount Text
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(record.amount, width / 2f, 385f, amountPaint)

        val opTypePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(167, 243, 208)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(record.operationTypeAr, width / 2f, 442f, opTypePaint)

        // 4. Sender Section Box
        drawPartySectionBox(
            canvas = canvas,
            topY = 535f,
            width = width,
            headerLabel = "بيانات المرسل (الاسم الرباعي ورقم الهاتف):",
            partyName = senderName,
            partyPhone = "رقم الهاتف / المعرف: $senderPhone",
            accentColor = android.graphics.Color.rgb(59, 130, 246)
        )

        // 5. Recipient Section Box
        drawPartySectionBox(
            canvas = canvas,
            topY = 785f,
            width = width,
            headerLabel = "بيانات المستلم (الاسم الرباعي ورقم الهاتف):",
            partyName = recipientName,
            partyPhone = "رقم الهاتف / المعرف: $recipientPhone",
            accentColor = android.graphics.Color.rgb(16, 185, 129)
        )

        // 6. Metadata & Reference ID Box
        val metaRect = RectF(96f, 1040f, width - 96f, 1455f)
        paint.color = android.graphics.Color.rgb(13, 20, 36)
        canvas.drawRoundRect(metaRect, 32f, 32f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.rgb(51, 65, 85)
        canvas.drawRoundRect(metaRect, 32f, 32f, paint)
        paint.style = Paint.Style.FILL

        drawMetaRow(canvas, width, 1110f, "الرقم المرجعي (Reference ID):", referenceId, android.graphics.Color.rgb(56, 189, 248))
        drawMetaRow(canvas, width, 1195f, "رمز التوثيق لمرة واحدة (OTT):", "#${record.ottId}", android.graphics.Color.rgb(16, 185, 129))
        drawMetaRow(canvas, width, 1280f, "التاريخ والوقت الدقيق:", exactDateTime, android.graphics.Color.WHITE)
        drawMetaRow(canvas, width, 1365f, "التوقيع الأمني الرقمي:", "CRC8=${record.checksumHex} • Keystore HMAC ✓", android.graphics.Color.rgb(148, 163, 184))

        // 7. Footer Stamp
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(16, 185, 129)
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "إيصال إلكتروني رسمي صادر وموثق عبر سحابة VibraPay المشفرة ✓",
            width / 2f,
            1545f,
            footerPaint
        )

        return bitmap
    }

    private fun drawPartySectionBox(
        canvas: Canvas,
        topY: Float,
        width: Int,
        headerLabel: String,
        partyName: String,
        partyPhone: String,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val boxRect = RectF(96f, topY, width - 96f, topY + 215f)
        paint.color = android.graphics.Color.rgb(13, 20, 36)
        canvas.drawRoundRect(boxRect, 30f, 30f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = accentColor
        canvas.drawRoundRect(boxRect, 30f, 30f, paint)
        paint.style = Paint.Style.FILL

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(148, 163, 184)
            textSize = 25f
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 35f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 29f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(headerLabel, width / 2f, topY + 52f, labelPaint)
        canvas.drawText(partyName, width / 2f, topY + 115f, namePaint)
        canvas.drawText(partyPhone, width / 2f, topY + 172f, phonePaint)
    }

    private fun drawMetaRow(
        canvas: Canvas,
        width: Int,
        y: Float,
        label: String,
        value: String,
        valueColor: Int
    ) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(148, 163, 184)
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(label, width - 135f, y, labelPaint)
        canvas.drawText(value, 135f, y, valuePaint)
    }
}

/**
 * Buyer Payment Approval Modal (`نافذة موافقة المشتري على طلب الدفع الصوتي`):
 * Displayed when an incoming `REQ|Amount|OTT_ID` ultrasonic packet is captured.
 */
@Composable
fun BuyerPaymentApprovalModal(
    requestInvoice: StructuredPayload.Invoice,
    onApproveAndPay: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("buyer_payment_approval_modal"),
            shape = RoundedCornerShape(26.dp),
            color = WalletSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(WalletEmerald.copy(alpha = 0.16f))
                        .border(1.5.dp, WalletEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = WalletEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "طلب دفع صوتي وارد (Acoustic Payment Request)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = WalletSubtleSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الجهة الطالبة / المستلم:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = requestInvoice.storeName.ifBlank { "نقطة بيع VibraPay" },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${requestInvoice.amount} ${requestInvoice.currency}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = WalletEmerald,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "OTT: #${requestInvoice.ottId}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApproveAndPay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("approve_buyer_payment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "موافقة ودفع الآن",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = onDecline,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletDanger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletDanger),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(48.dp)
                            .testTag("decline_buyer_payment_button")
                    ) {
                        Text(
                            text = "رفض",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

