package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BurnedTokenInfo
import com.example.data.TransferRecord
import com.example.protocol.AcousticMode
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferHistoryScreen(
    records: List<TransferRecord>,
    burnedTokens: List<BurnedTokenInfo>,
    onOpenReceipt: (TransferRecord) -> Unit,
    onRetransmit: (TransferRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("history_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سجل المعاملات والسندات الرقمية",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "العمليات الناجحة (✅ أخضر زمردي) والمحظورة أمنياً (⚠️ أحمر)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (records.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletDanger),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Text("مسح السجل", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Burned Anti-Replay Tokens Summary Pill
        if (burnedTokens.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = WalletRoyalBlue.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WalletRoyalBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رموز العمليات المحروقة ضد التكرار (OTT):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${burnedTokens.size} عملية محمية",
                        style = MaterialTheme.typography.labelMedium,
                        color = WalletRoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(WalletSurface)
                            .border(1.dp, BorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد معاملات مسجلة حتى الآن",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جميع الحوالات المرسلة والمستلمة وطلبات الدفع ستظهر هنا",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    TransferHistoryItemCard(
                        record = record,
                        onOpenReceipt = { onOpenReceipt(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun TransferHistoryItemCard(
    record: TransferRecord,
    onOpenReceipt: () -> Unit,
    onDelete: () -> Unit
) {
    val isSecurityBlocked = !record.isChecksumValid || record.payloadType == "SECURITY_ALERT"
    val isReceived = record.direction == "RECEIVED"
    val statusColor = when {
        isSecurityBlocked -> WalletDanger
        isReceived -> WalletEmerald
        else -> WalletRoyalBlue
    }
    val timeStr = SimpleDateFormat("HH:mm • yyyy/MM/dd", Locale.getDefault())
        .format(Date(record.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenReceipt() }
            .border(
                width = if (isSecurityBlocked) 1.5.dp else 1.dp,
                color = if (isSecurityBlocked) WalletDanger.copy(alpha = 0.55f) else BorderSubtle,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("history_item_${record.id}"),
        shape = RoundedCornerShape(18.dp),
        color = WalletSurface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSecurityBlocked -> Icons.Default.Warning
                                isReceived -> Icons.Default.CallReceived
                                else -> Icons.Default.CallMade
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isSecurityBlocked -> "محظورة أمنياً ⚠️"
                            isReceived -> "مستلمة ✅"
                            else -> "مرسلة ✅"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WalletSubtleSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "OTT #${record.ottId}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (record.amount != "0.00" && record.amount.isNotBlank()) {
                    Text(
                        text = if (isReceived && !isSecurityBlocked) "+${record.amount}" else record.amount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = if (isSecurityBlocked) WalletDanger else WalletEmerald,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!isSecurityBlocked) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (!isSecurityBlocked) WalletEmerald else WalletDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isSecurityBlocked) {
                            "ناجحة وموقعة رقمياً ✅ • Salt: 0x${record.saltHex}"
                        } else {
                            "محظورة بسبب التكرار أو اختلاف المفتاح ⚠️"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isSecurityBlocked) WalletEmerald else WalletDanger,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenReceipt, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "عرض السند الرقمي",
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolAndTestingGuideScreen(
    acousticMode: AcousticMode,
    onRunQuickTest: (AcousticMode, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize())
}
