package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Three official App Theme Modes for VibraPay:
 * 1) DARK: الوضع المظلم (Dark Mode)
 * 2) LIGHT: الوضع الفاتح (Light Mode)
 * 3) SYSTEM: حسب وضع الهاتف العام (System Default)
 */
enum class AppThemeMode(
    val code: String,
    val titleAr: String,
    val shortLabelAr: String,
    val subtitleAr: String
) {
    DARK(
        code = "DARK",
        titleAr = "الوضع المظلم (Dark Mode)",
        shortLabelAr = "الوضع المظلم",
        subtitleAr = "تصميم داكن أنيق ومريح للعين مع تباين عالي الدقة"
    ),
    LIGHT(
        code = "LIGHT",
        titleAr = "الوضع الفاتح (Light Mode)",
        shortLabelAr = "الوضع الفاتح",
        subtitleAr = "واجهة ساطعة وواضحة للاستخدام النهاري"
    ),
    SYSTEM(
        code = "SYSTEM",
        titleAr = "حسب وضع الهاتف العام (System Default)",
        shortLabelAr = "حسب وضع الهاتف العام",
        subtitleAr = "التكيف التلقائي الذكي مع ثيم النظام في هاتفك"
    );

    companion object {
        fun fromCode(code: String?): AppThemeMode {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: DARK
        }
    }
}

private val VibraPayLightColorScheme = lightColorScheme(
    primary = WalletRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = WalletRoyalBlueDark,
    secondary = WalletEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = WalletEmeraldDark,
    tertiary = WalletAmber,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = WalletDanger,
    onError = Color.White,
    outline = Color(0xFFE2E8F0)
)

private val VibraPayDarkColorScheme = darkColorScheme(
    primary = WalletRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = WalletEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = WalletAmber,
    onTertiary = Color.White,
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = WalletDanger,
    onError = Color.White,
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    appThemeMode: AppThemeMode = AppThemeMode.DARK,
    rtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val useDarkTheme = when (appThemeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
    }
    isVibraPayDarkTheme = useDarkTheme

    val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) VibraPayDarkColorScheme else VibraPayLightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
