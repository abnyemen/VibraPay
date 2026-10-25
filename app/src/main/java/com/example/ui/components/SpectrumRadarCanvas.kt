package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.ReceiverStatus
import com.example.audio.SpectrumFrame
import com.example.protocol.AcousticMode
import com.example.protocol.MfskProtocol
import com.example.protocol.ProximityLevel
import com.example.protocol.ScheduledTone
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
import com.example.ui.theme.WalletDanger
import com.example.ui.theme.WalletEmerald
import com.example.ui.theme.WalletPrimary
import com.example.ui.theme.WalletRoyalBlue
import com.example.ui.theme.WalletSecondary
import com.example.ui.theme.WalletSkyAccent
import com.example.ui.theme.WalletSubtleSurface
import com.example.ui.theme.WalletSurface

@Composable
fun SpectrumRadarCard(
    acousticMode: AcousticMode,
    spectrumFrame: SpectrumFrame,
    threshold: Int,
    isListening: Boolean,
    listeningRemainingSeconds: Int,
    isTransmitting: Boolean,
    activeTransmitTone: ScheduledTone?,
    receiverStatus: ReceiverStatus,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExcellentContact = spectrumFrame.dominantMagnitude >= threshold ||
        spectrumFrame.proximityLevel == ProximityLevel.EXCELLENT_CONTACT

    val activeBorderColor by animateColorAsState(
        targetValue = when {
            isExcellentContact && isListening -> WalletEmerald
            receiverStatus == ReceiverStatus.RECEIVING -> WalletEmerald
            isListening -> WalletRoyalBlue.copy(alpha = 0.55f)
            isTransmitting -> WalletRoyalBlue
            else -> BorderSubtle
        },
        label = "radarBorder"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "acousticRadarPulse")
    val pulsePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulsePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 730, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val pulsePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 1460, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    val radarAccentColor by animateColorAsState(
        targetValue = when {
            isExcellentContact && isListening -> WalletEmerald
            isListening -> WalletRoyalBlue
            isTransmitting -> WalletSkyAccent
            else -> WalletRoyalBlue.copy(alpha = 0.65f)
        },
        label = "radarCircleColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, activeBorderColor, RoundedCornerShape(24.dp))
            .testTag("spectrum_radar_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row + 20-Second Circular Auto-Timeout Badge
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isExcellentContact && isListening) WalletEmerald.copy(alpha = 0.14f)
                                else WalletRoyalBlue.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = if (isExcellentContact && isListening) WalletEmerald else WalletRoyalBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "رادار التلامس الصوتي (Acoustic NFC)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isListening) {
                                "الاستماع نشط • قرّب الهاتف الآخر لمسافة أقل من 10 سم"
                            } else {
                                "اضغط لتفعيل الاستلام الآمن في نطاق التلامس القريب"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isListening) WalletRoyalBlue else TextSecondary
                        )
                    }
                }

                // 20-Second Circular Countdown Timer Widget
                val timerFraction = if (isListening) {
                    (listeningRemainingSeconds.toFloat() / MfskProtocol.AUTO_LISTEN_TIMEOUT_SECONDS.toFloat())
                        .coerceIn(0f, 1f)
                } else 0f

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isListening) WalletRoyalBlue.copy(alpha = 0.08f) else WalletSubtleSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isListening) WalletRoyalBlue.copy(alpha = 0.3f) else BorderSubtle
                    ),
                    modifier = Modifier.testTag("auto_timeout_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { if (isListening) timerFraction else 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = if (listeningRemainingSeconds in 1..5) WalletDanger
                                else if (isListening) WalletRoyalBlue
                                else BorderSubtle,
                                trackColor = BorderSubtle.copy(alpha = 0.4f),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (isListening) "${listeningRemainingSeconds}s" else "20s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = if (isListening) WalletRoyalBlue else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "مؤقت الأمان",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (isListening) "إغلاق ذاتي" else "حماية الزحام",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isListening) WalletRoyalBlue else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pure White Concentric Animated Radar Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderSubtle.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("spectrum_canvas")
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (minOf(size.width, size.height) / 2f) * 0.90f

                    // Static subtle guide rings
                    for (step in 1..3) {
                        drawCircle(
                            color = WalletRoyalBlue.copy(alpha = 0.08f),
                            radius = maxRadius * (step / 3f),
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Smoothly animated concentric radar ripples when listening or transmitting
                    if (isListening || isTransmitting) {
                        val phases = listOf(pulsePhase1, pulsePhase2, pulsePhase3)
                        phases.forEach { phase ->
                            val currentRadius = maxRadius * (0.22f + phase * 0.78f)
                            val alpha = (1f - phase).coerceIn(0f, 1f) * 0.45f
                            drawCircle(
                                color = radarAccentColor.copy(alpha = alpha),
                                radius = currentRadius,
                                center = center,
                                style = Stroke(width = 4f)
                            )
                            drawCircle(
                                color = radarAccentColor.copy(alpha = alpha * 0.18f),
                                radius = currentRadius,
                                center = center
                            )
                        }
                    }

                    // Signal energy glow ring responsive to real microphone magnitude
                    val magRatio = (spectrumFrame.dominantMagnitude / 255f).coerceIn(0.05f, 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                radarAccentColor.copy(alpha = 0.25f * magRatio + 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * (0.4f + 0.5f * magRatio)
                        ),
                        radius = maxRadius * (0.4f + 0.5f * magRatio),
                        center = center
                    )
                }

                // Center Radar Hub Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(74.dp),
                        shape = CircleShape,
                        color = if (isExcellentContact && isListening) {
                            WalletEmerald
                        } else if (isListening) {
                            WalletRoyalBlue
                        } else {
                            WalletSubtleSurface
                        },
                        shadowElevation = if (isListening) 8.dp else 0.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isListening) Color.White else BorderSubtle
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = if (isListening) Color.White else WalletRoyalBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            isExcellentContact && isListening -> "✨ تلامس آمن ممتاز (< 10 سم) ✅"
                            isListening -> "جاري المسح الصوتي... (${listeningRemainingSeconds} ثانية)"
                            receiverStatus == ReceiverStatus.AUTO_TIMEOUT_CLOSED ->
                                "تم إغلاق الميكروفون تلقائياً بعد 20 ثانية لحمايتك"
                            else -> "الرادار الصوتي في وضع الاستعداد"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isExcellentContact && isListening -> WalletEmerald
                            isListening -> WalletRoyalBlue
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Proximity Meter (مقياس مسافة التلامس الحي)
            RadarProximityDistanceMeter(
                proximityLevel = spectrumFrame.proximityLevel,
                dominantMagnitude = spectrumFrame.dominantMagnitude,
                roomPeakMagnitude = 0,
                threshold = threshold,
                isListening = isListening,
                isTransmitting = isTransmitting
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Manual Stop / Start Listening Button
            Button(
                onClick = onToggleListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) WalletDanger else WalletRoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("radar_toggle_listening_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) {
                        "إيقاف الاستماع يدوياً (${listeningRemainingSeconds} ث)"
                    } else {
                        "تفعيل التلامس الصوتي الآن (20 ثانية)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RadarProximityDistanceMeter(
    proximityLevel: ProximityLevel,
    dominantMagnitude: Int,
    roomPeakMagnitude: Int,
    threshold: Int,
    isListening: Boolean,
    isTransmitting: Boolean
) {
    val effectiveLevel = when {
        dominantMagnitude >= threshold -> ProximityLevel.EXCELLENT_CONTACT
        dominantMagnitude >= 120 -> ProximityLevel.GET_CLOSER
        else -> proximityLevel
    }

    val progressFraction by animateFloatAsState(
        targetValue = when {
            effectiveLevel == ProximityLevel.EXCELLENT_CONTACT -> 1f
            effectiveLevel == ProximityLevel.GET_CLOSER -> 0.58f
            isListening -> (dominantMagnitude / 255f).coerceIn(0.14f, 0.45f)
            else -> 0.08f
        },
        animationSpec = tween(durationMillis = 180),
        label = "proximityProgress"
    )

    val statusColor = when (effectiveLevel) {
        ProximityLevel.EXCELLENT_CONTACT -> WalletEmerald
        ProximityLevel.GET_CLOSER -> WalletAmber
        ProximityLevel.FAR -> WalletRoyalBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WalletSubtleSurface)
            .border(
                width = 1.dp,
                color = if (effectiveLevel == ProximityLevel.EXCELLENT_CONTACT) WalletEmerald else BorderSubtle,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("radar_proximity_meter")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مقياس مسافة التلامس الحي:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = when (effectiveLevel) {
                    ProximityLevel.EXCELLENT_CONTACT -> "✨ تلامس آمن ممتاز < 10 سم ✅"
                    ProximityLevel.GET_CLOSER -> "اقترب أكثر..."
                    ProximityLevel.FAR -> "ابحث عن القريب..."
                },
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("radar_proximity_status_text")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BorderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(statusColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stages = listOf(
                Triple(ProximityLevel.FAR, "ابحث عن القريب...", WalletRoyalBlue),
                Triple(ProximityLevel.GET_CLOSER, "اقترب أكثر...", WalletAmber),
                Triple(ProximityLevel.EXCELLENT_CONTACT, "✨ تلامس آمن < 10 سم ✅", WalletEmerald)
            )

            stages.forEach { (level, title, color) ->
                val isCurrent = effectiveLevel == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) color.copy(alpha = 0.14f) else WalletSurface)
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) color else BorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) color else TextMuted,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
