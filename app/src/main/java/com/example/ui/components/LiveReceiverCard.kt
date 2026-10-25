package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.ReceiverStatus
import com.example.protocol.DecodedPacketResult
import com.example.protocol.MfskProtocol
import com.example.protocol.ProximityLevel
import com.example.protocol.StructuredPayload
import com.example.protocol.XorStreamCipher
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletPrimary
import com.example.ui.theme.WalletSecondary
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface

@Composable
fun LiveReceiverControlCard(
    isListening: Boolean,
    listeningRemainingSeconds: Int,
    nfcProximityMode: Boolean,
    highPassFilterEnabled: Boolean,
    highPassCutoffHz: Int,
    suppressedNoisePercent: Int,
    proximityLevel: ProximityLevel,
    dominantMagnitude: Int,
    estimatedDistanceCmText: String,
    singleRequestLockActive: Boolean,
    blockedCrowdSignalsCount: Int,
    lastBlockedCrowdAlert: String?,
    receiverStatus: ReceiverStatus,
    threshold: Int,
    loopbackSelfTestEnabled: Boolean,
    liveAccumulatedHex: String,
    livePreviewText: String,
    liveDetectedSymbols: List<String>,
    lastDecodedPacket: DecodedPacketResult?,
    onToggleListening: () -> Unit,
    onToggleNfcProximityMode: (Boolean) -> Unit,
    onToggleHighPassFilter: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onToggleLoopback: (Boolean) -> Unit,
    onSimulateCrowdIntruderAttack: () -> Unit,
    onDismissDecoded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isListening) WalletPrimary.copy(alpha = 0.5f) else BorderSubtle,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("receiver_control_card"),
        shape = RoundedCornerShape(20.dp),
        color = WalletSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status Row
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isListening) WalletPrimary else WalletSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "حالة الاستلام المباشر",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = receiverStatus.titleAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (receiverStatus) {
                                ReceiverStatus.RECEIVING, ReceiverStatus.PACKET_COMPLETE -> WalletPrimary
                                ReceiverStatus.SELLER_WAITING_PAY -> WalletSecondary
                                ReceiverStatus.LISTENING -> WalletAmber
                                ReceiverStatus.AUTO_TIMEOUT_CLOSED, ReceiverStatus.BACKGROUND_KILLED -> WalletDanger
                                ReceiverStatus.IDLE -> TextSecondary
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 20-Second Safety Auto-Timeout Progress Bar when Listening
            AnimatedVisibility(visible = isListening) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletSubtleSurface)
                        .border(1.dp, WalletAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = WalletAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إغلاق تلقائي للأمان خلال $listeningRemainingSeconds ثانية",
                                style = MaterialTheme.typography.labelMedium,
                                color = WalletAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "تلامس قريب < 10 سم",
                            style = MaterialTheme.typography.labelSmall,
                            color = WalletPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeoutFraction = (listeningRemainingSeconds.toFloat() / MfskProtocol.AUTO_LISTEN_TIMEOUT_SECONDS.toFloat())
                        .coerceIn(0.02f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(timeoutFraction)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (listeningRemainingSeconds <= 5) WalletDanger else WalletPrimary)
                        )
                    }
                }
            }

            // Live Incoming Reception Indicator
            AnimatedVisibility(visible = livePreviewText.isNotBlank() && isListening) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletPrimary.copy(alpha = 0.08f))
                        .border(1.dp, WalletPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "جاري استلام البيانات عبر التلامس...",
                        style = MaterialTheme.typography.labelMedium,
                        color = WalletPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = livePreviewText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("live_preview_text")
                    )
                }
            }

            // Decoded Packet Banner / Receipt Card when reception completes
            AnimatedVisibility(visible = lastDecodedPacket != null) {
                lastDecodedPacket?.let { decoded ->
                    DecodedPacketCard(
                        decoded = decoded,
                        onDismiss = onDismissDecoded,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DecodedPacketCard(
    decoded: DecodedPacketResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlert = !decoded.isChecksumValid || decoded.isKeyMismatch || decoded.isReplayBurnedError
    val borderColor = if (isAlert) WalletDanger else WalletPrimary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("decoded_packet_card"),
        shape = RoundedCornerShape(16.dp),
        color = WalletSubtleSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                    Icon(
                        imageVector = when {
                            decoded.isReplayBurnedError -> Icons.Default.Block
                            decoded.isKeyMismatch -> Icons.Default.GppBad
                            decoded.isChecksumValid -> Icons.Default.CheckCircle
                            else -> Icons.Default.WarningAmber
                        },
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when {
                                decoded.isReplayBurnedError -> "عملية مرفوضة: تم تنفيذها مسبقاً"
                                decoded.isKeyMismatch -> "تعذر فك التشفير: مفتاح الأمان غير متطابق"
                                decoded.isChecksumValid -> "تم الاستلام بالتلامس بنجاح ✓"
                                else -> "لم تكتمل القراءة، يرجى تقريب الهاتفين والمحاولة"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = borderColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("dismiss_decoded_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (decoded.isReplayBurnedError || decoded.isKeyMismatch) {
                SecurityAlertBox(
                    message = decoded.securityAlertMessage ?: XorStreamCipher.SECURITY_ALERT_KEY_MISMATCH
                )
            } else {
                when (val structured = decoded.structuredPayload) {
                    is StructuredPayload.Invoice -> {
                        DecodedInvoiceView(invoice = structured, isEncrypted = decoded.isEncrypted)
                    }

                    is StructuredPayload.ContactCard -> {
                        DecodedContactView(contact = structured)
                    }

                    is StructuredPayload.FreeText -> {
                        Text(
                            text = structured.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("decoded_free_text_value")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityAlertBox(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WalletDanger.copy(alpha = 0.08f))
            .border(1.dp, WalletDanger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("security_alert_box")
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = WalletDanger,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DecodedInvoiceView(
    invoice: StructuredPayload.Invoice,
    isEncrypted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WalletSurface)
            .border(1.dp, WalletPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = WalletPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = invoice.opType.labelAr,
                    style = MaterialTheme.typography.titleSmall,
                    color = WalletPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEncrypted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = WalletSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "عملية مشفرة وموثقة",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalletSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "المبلغ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "${invoice.amount} ${invoice.currency}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WalletPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "رقم العملية",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "#${invoice.ottId}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الجهة: ${invoice.storeName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "معتمدة ✓",
                style = MaterialTheme.typography.labelSmall,
                color = WalletPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DecodedContactView(contact: StructuredPayload.ContactCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WalletSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ContactPhone,
                contentDescription = null,
                tint = WalletSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = contact.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "الهاتف: ${contact.phone}",
            style = MaterialTheme.typography.bodyMedium,
            color = WalletPrimary
        )
        if (contact.organization.isNotBlank()) {
            Text(
                text = "الجهة: ${contact.organization}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
