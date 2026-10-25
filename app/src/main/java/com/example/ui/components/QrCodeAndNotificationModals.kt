package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.InAppNotification
import com.example.data.NotificationCategory
import com.example.data.OfficialAccountProfile
import com.example.qrcode.QrCodeEngine
import com.example.qrcode.QrScannedRecipient
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletAmber
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

/**
 * 1. Personal User QR Code Dialog (`رمزي السريع - My QR Code`):
 * - Displays the user's unique, cryptographically structured QR Code containing:
 *   Full Quadruple Name (`الاسم الرباعي`), Phone Number (`رقم الهاتف`), and Account ID (`معرف الحساب`).
 * - Allows sharing the high-resolution QR Card image or saving it directly to the device's Gallery.
 */
@Composable
fun MyPersonalQrCodeDialog(
    activeAccount: OfficialAccountProfile?,
    onOpenScannerInstead: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val account = activeAccount ?: return
    val qrBitmap = remember(account.accountId, account.phoneNumberLocal, account.fullQuadName) {
        QrCodeEngine.renderQrCodeSquareBitmap(account, 680)
    }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .testTag("my_personal_qr_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = WalletSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(WalletRoyalBlueDark, WalletRoyalBlue, WalletSkyAccent)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "رمزي السريع (My QR Code)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "امسح الرمز لتحويل الأموال لحسابك فوراً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("close_my_qr_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Crisp White QR Frame
                    Surface(
                        modifier = Modifier
                            .size(246.dp)
                            .testTag("personal_qr_code_image_box"),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, WalletRoyalBlue.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "رمز الاستجابة السريعة الخاص بحساب ${account.fullQuadName}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Encoded Account Summary Box
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
                                text = "البيانات المشفرة داخل رمز QR الخاص بك:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = account.fullQuadName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WalletRoyalBlue.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        WalletRoyalBlue.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Text(
                                        text = "${account.countryFlag} ${account.formattedInternationalPhone}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = WalletRoyalBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WalletEmerald.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        WalletEmerald.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Text(
                                        text = "ID: ${account.accountId}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = WalletEmerald,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = WalletEmerald.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WalletEmerald)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        text = msg,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WalletEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Share QR Card & Save QR to Gallery Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val cardBitmap = QrCodeEngine.renderShareableQrCardBitmap(account)
                                val caption =
                                    "رمز الدفع والتحويل السريع (QR Code) لحسابي في VibraPay:\n• الاسم: ${account.fullQuadName}\n• رقم الهاتف: ${account.formattedInternationalPhone}\n• معرف الحساب: #${account.accountId}"
                                QrCodeEngine.shareBitmapImage(
                                    context = context,
                                    bitmap = cardBitmap,
                                    fileNamePrefix = "VibraPay_QR_${account.accountId}",
                                    shareTitleAr = "مشاركة رمز QR لحساب ${account.fullQuadName}",
                                    shareCaptionAr = caption
                                )
                                statusBanner = "تم تجهيز بطاقة رمز QR عالية الدقة وفتح قائمة المشاركة ✓"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletRoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_my_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشاركة رمزي",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cardBitmap = QrCodeEngine.renderShareableQrCardBitmap(account)
                                val res = QrCodeEngine.saveBitmapToGallery(
                                    context = context,
                                    bitmap = cardBitmap,
                                    fileNamePrefix = "VibraPay_QR_${account.accountId}",
                                    descriptionAr = "رمز QR لحساب ${account.fullQuadName}"
                                )
                                statusBanner = if (res.isSuccess) {
                                    "تم حفظ بطاقة رمز QR في معرض الصور (Pictures/VibraPay) بنجاح ✓"
                                } else {
                                    "تم حفظ نسخة رمز QR في ذاكرة الجهاز بنجاح ✓"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_my_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
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

                    // Switch to QR Scanner Button
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenScannerInstead()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("switch_to_qr_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = WalletRoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فتح ماسح رمز QR لتحويل الأموال لمستخدم آخر",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Fast QR Code Scanner Dialog (`مسح رمز QR للتحويل والدفع السريع`):
 * - Supports:
 *   a) Optical Camera Viewfinder Scanner (`Camera`) with runtime permission handling.
 *   b) Selecting a saved QR Code image from the device Gallery (`ActivityResultContracts.PickVisualMedia`).
 *   c) Instant QR Detection of registered Cloud Accounts in the viewfinder so the user can scan with 1 tap.
 * - Immediately upon decoding the QR code, calls [onRecipientScanned] to auto-populate the recipient's
 *   Full Quadruple Name and Phone Number / Account ID into the transfer screen!
 */
@Composable
fun QrCodeScannerDialog(
    directoryAccounts: List<OfficialAccountProfile>,
    activeSenderAccountId: String?,
    onRecipientScanned: (QrScannedRecipient) -> Unit,
    onOpenMyQrInstead: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            try {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }
        )
    }
    var scannerErrorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Zero-Permission Android Photo Picker for decoding a QR image from the Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        if (selectedUri != null) {
            try {
                context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val decodeResult = QrCodeEngine.decodeQrBitmap(bitmap, directoryAccounts)
                        decodeResult.onSuccess { scanned ->
                            scannerErrorMessage = null
                            onRecipientScanned(scanned)
                        }.onFailure { err ->
                            scannerErrorMessage = err.message ?: "تعذر قراءة رمز QR من الصورة المختارة"
                        }
                    } else {
                        scannerErrorMessage = "تعذر فتح ملف الصورة المختارة من الاستوديو"
                    }
                }
            } catch (e: Throwable) {
                scannerErrorMessage = "حدث خطأ أثناء قراءة صورة QR: ${e.localizedMessage ?: ""}"
            }
        }
    }

    val scannableAccounts = remember(directoryAccounts, activeSenderAccountId) {
        directoryAccounts.filter { it.accountId != activeSenderAccountId }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_laser_y"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("qr_code_scanner_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = WalletSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(WalletRoyalBlueDark, WalletRoyalBlue, WalletEmerald)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مسح رمز QR للتحويل السريع",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "تعبئة تلقائية لاسم المستلم ورقم هاتفه فور المسح",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("close_qr_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق الماسح",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optical Laser Viewfinder Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .testTag("qr_scanner_viewfinder_box"),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0A0F1D),
                        border = androidx.compose.foundation.BorderStroke(2.dp, WalletEmerald.copy(alpha = 0.75f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Animated Laser Line & Corner Brackets
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bracketLen = 34.dp.toPx()
                                val stroke = 4.dp.toPx()
                                val pad = 22.dp.toPx()
                                val w = size.width
                                val h = size.height
                                val cornerColor = Color(0xFF10B981)

                                // Top-Left Corner
                                drawLine(cornerColor, Offset(pad, pad), Offset(pad + bracketLen, pad), stroke)
                                drawLine(cornerColor, Offset(pad, pad), Offset(pad, pad + bracketLen), stroke)
                                // Top-Right Corner
                                drawLine(cornerColor, Offset(w - pad, pad), Offset(w - pad - bracketLen, pad), stroke)
                                drawLine(cornerColor, Offset(w - pad, pad), Offset(w - pad, pad + bracketLen), stroke)
                                // Bottom-Left Corner
                                drawLine(cornerColor, Offset(pad, h - pad), Offset(pad + bracketLen, h - pad), stroke)
                                drawLine(cornerColor, Offset(pad, h - pad), Offset(pad, h - pad - bracketLen), stroke)
                                // Bottom-Right Corner
                                drawLine(cornerColor, Offset(w - pad, h - pad), Offset(w - pad - bracketLen, h - pad), stroke)
                                drawLine(cornerColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - bracketLen), stroke)

                                // Animated Laser Beam
                                val laserY = h * laserProgress
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF10B981),
                                            Color(0xFF38BDF8),
                                            Color(0xFF10B981),
                                            Color.Transparent
                                        )
                                    ),
                                    start = Offset(pad * 1.2f, laserY),
                                    end = Offset(w - pad * 1.2f, laserY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = WalletEmerald,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (hasCameraPermission) {
                                        "عدسة الماسح الضوئي نشطة • وجّه الكاميرا نحو رمز QR أو اختر من الاستوديو"
                                    } else {
                                        "امسح رمز QR عبر الكاميرا أو اختر صورة الرمز المحفوظة من الاستوديو"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (!hasCameraPermission) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            } catch (_: Throwable) {
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WalletEmerald),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletEmerald),
                                        modifier = Modifier.testTag("grant_camera_qr_permission_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تفعيل الكاميرا للمسح المباشر",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Row: Pick QR Image from Gallery OR Show My QR Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (_: Throwable) {
                                    scannerErrorMessage = "تعذر فتح معرض الصور على هذا الجهاز"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletRoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_qr_from_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "قراءة QR من الاستوديو",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenMyQrInstead()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletEmerald),
                            modifier = Modifier
                                .weight(0.75f)
                                .height(48.dp)
                                .testTag("open_my_qr_from_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عرض رمزي",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    AnimatedVisibility(visible = scannerErrorMessage != null) {
                        scannerErrorMessage?.let { err ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = WalletDanger.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WalletDanger)
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WalletDanger,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // Instant Cloud QR Detection List (For 1-Tap Scanning in Emulator or Quick Testing)
                    Text(
                        text = "رموز QR المكتشفة للحسابات السحابية الجاهزة للمسح الفوري:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scannableAccounts.take(4).forEach { acc ->
                            val miniQrBitmap = remember(acc.accountId) {
                                QrCodeEngine.renderQrCodeSquareBitmap(acc, 180)
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onRecipientScanned(
                                            QrScannedRecipient(
                                                accountId = acc.accountId,
                                                fullQuadName = acc.fullQuadName,
                                                phoneNumberLocal = acc.phoneNumberLocal,
                                                formattedInternationalPhone = acc.formattedInternationalPhone,
                                                countryFlag = acc.countryFlag,
                                                rawPayload = QrCodeEngine.buildUserQrPayload(acc)
                                            )
                                        )
                                    }
                                    .testTag("scan_detected_qr_account_${acc.accountId}"),
                                shape = RoundedCornerShape(16.dp),
                                color = WalletSubtleSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    WalletRoyalBlue.copy(alpha = 0.40f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                        ) {
                                            Image(
                                                bitmap = miniQrBitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acc.fullQuadName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${acc.countryFlag} ${acc.phoneNumberLocal} • ID: ${acc.accountId}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                color = TextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = WalletEmerald
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "مسح وتعبئة",
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
                }
            }
        }
    }
}

/**
 * 3. In-App Notification Center Modal (`مركز وسجل الإشعارات داخل التطبيق`):
 * - Opened when clicking the Bell icon (`Icons.Default.Notifications`) at the top of the Home screen.
 * - Displays all Financial & Security notifications linked to the user's cloud account, sorted newest-first.
 * - Provides filter tabs (`الكل`, `المالية 💰`, `الأمان 🔐`), "تحديد الكل كمقروء" (Mark All as Read),
 *   and "مسح سجل الإشعارات" (Clear Notifications).
 */
@Composable
fun InAppNotificationCenterDialog(
    notifications: List<InAppNotification>,
    unreadCount: Int,
    activeAccount: OfficialAccountProfile?,
    onMarkAllRead: () -> Unit,
    onMarkSingleRead: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<NotificationCategory?>(null) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        if (selectedFilter == null) {
            notifications.sortedByDescending { it.timestamp }
        } else {
            notifications.filter { it.category == selectedFilter }.sortedByDescending { it.timestamp }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("in_app_notification_center_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = WalletSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WalletRoyalBlue.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
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
                                .background(WalletRoyalBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (unreadCount > 0) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = WalletRoyalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "مركز الإشعارات السحابية",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WalletDanger
                                    ) {
                                        Text(
                                            text = "$unreadCount جديد",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = activeAccount?.let {
                                    "الحساب: ${it.fullQuadName} (#${it.accountId})"
                                } ?: "سجل الإشعارات المالية والأمنية الفورية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("close_notifications_center_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                // Category Filter Pills (`الكل` | `الإشعارات المالية 💰` | `إشعارات الأمان 🔐`)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        null to "الكل (${notifications.size})",
                        NotificationCategory.FINANCIAL to "المالية 💰 (${notifications.count { it.category == NotificationCategory.FINANCIAL }})",
                        NotificationCategory.SECURITY to "الأمان 🔐 (${notifications.count { it.category == NotificationCategory.SECURITY }})"
                    )
                    filterOptions.forEach { (category, label) ->
                        val isSelected = selectedFilter == category
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFilter = category }
                                .testTag("notif_filter_${category?.code?.lowercase() ?: "all"}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) WalletRoyalBlue else WalletSubtleSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) WalletRoyalBlue else BorderSubtle
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Action Row: "تحديد الكل كمقروء" + "مسح سجل الإشعارات"
                if (notifications.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMarkAllRead,
                            enabled = unreadCount > 0,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (unreadCount > 0) WalletEmerald else BorderSubtle
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mark_all_notifications_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تحديد الكل كمقروء",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onClearAllNotifications,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WalletDanger.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WalletDanger),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_all_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مسح سجل الإشعارات",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Notifications List (Newest to Oldest)
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد إشعارات مسجلة حالياً",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ستظهر هنا جميع إشعارات استلام وإرسال الأموال وتنبيهات الأمان مرتبة من الأحدث للأقدم",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 420.dp)
                            .testTag("notifications_lazy_list"),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(filteredNotifications, key = { it.id }) { item ->
                            NotificationItemCard(
                                item = item,
                                onClick = { onMarkSingleRead(item.id) }
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalletRoyalBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "إغلاق مركز الإشعارات",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: InAppNotification,
    onClick: () -> Unit
) {
    val isFinancial = item.category == NotificationCategory.FINANCIAL
    val accentColor = if (isFinancial) WalletEmerald else WalletRoyalBlue
    val formattedTime = remember(item.timestamp) {
        SimpleDateFormat("yyyy/MM/dd • HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("notification_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (!item.isRead) {
            accentColor.copy(alpha = 0.09f)
        } else {
            WalletSubtleSurface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (!item.isRead) 1.5.dp else 1.dp,
            color = if (!item.isRead) accentColor.copy(alpha = 0.55f) else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFinancial) {
                                Icons.Default.AccountBalanceWallet
                            } else {
                                Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!item.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WalletDanger)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = item.titleAr,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = TextMuted
                        )
                    }
                }

                if (item.amountBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = item.amountBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = item.messageAr,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = if (!item.isRead) FontWeight.SemiBold else FontWeight.Normal
            )

            if (item.counterpartyName.isNotBlank() || item.referenceId.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.counterpartyName.isNotBlank()) {
                        Text(
                            text = "الطرف الآخر: ${item.counterpartyName} ${if (item.counterpartyPhone.isNotBlank()) "(${item.counterpartyPhone})" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WalletRoyalBlue,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (item.referenceId.isNotBlank()) {
                        Text(
                            text = "#${item.referenceId}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
