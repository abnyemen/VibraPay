package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Reactive global theme state synchronized by [MyApplicationTheme] in `Theme.kt`.
 * Defaults to `true` (Dark Mode) for a sleek, modern financial aesthetic, and
 * dynamically switches when the user selects Dark Mode, Light Mode, or System Default.
 */
var isVibraPayDarkTheme by mutableStateOf(true)

// Global E-Wallet Visual Identity (Royal Blue #2563EB + Luminous Emerald #10B981 + Adaptive Dark/Light Surfaces)
val WalletBg: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF0B1120) else Color(0xFFF8FAFC)

val WalletSurface: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF111827) else Color(0xFFFFFFFF)

val WalletSubtleSurface: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)

val WalletHighlightSurface: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF172554) else Color(0xFFEFF6FF)

val WalletRoyalBlueDark = Color(0xFF1E3A8A)  // Deep Royal Blue for Hero Card Gradient Start
val WalletRoyalBlue = Color(0xFF2563EB)      // Primary Royal Blue (#2563EB)
val WalletSkyAccent = Color(0xFF0EA5E9)      // Luminous Sky Blue for Hero Card Gradient End

val WalletEmerald = Color(0xFF10B981)        // Success / Accent Emerald Green (#10B981)
val WalletEmeraldDark = Color(0xFF059669)

val WalletNavyStart = WalletRoyalBlueDark
val WalletTealEnd = WalletRoyalBlue

val WalletPrimary = WalletRoyalBlue          // Primary (#2563EB)
val WalletPrimaryDark = WalletRoyalBlueDark
val WalletSecondary = WalletEmerald          // Accent / Success (#10B981)
val WalletAmber = Color(0xFFF59E0B)          // Warm Amber
val WalletPurple = Color(0xFF6366F1)         // Modern Indigo
val WalletDanger = Color(0xFFEF4444)         // Security Alert Red (#EF4444)

val TextPrimary: Color
    get() = if (isVibraPayDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val TextSecondary: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)

val TextMuted: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)

val BorderSubtle: Color
    get() = if (isVibraPayDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

val BorderNeon = WalletRoyalBlue

// Dedicated Sleek Dark Sidebar Settings Palette
val SidebarDarkBg = Color(0xFF0F172A)
val SidebarDarkCard = Color(0xFF1E293B)
val SidebarDarkItemSurface = Color(0xFF0B1120)
val SidebarDarkBorder = Color(0xFF334155)
val SidebarTextPrimary = Color(0xFFF8FAFC)
val SidebarTextSecondary = Color(0xFF94A3B8)

// Legacy aliases mapped to the adaptive Royal Blue & Emerald E-Wallet palette
val CyberObsidian: Color
    get() = WalletSubtleSurface
val CyberDarkSurface: Color
    get() = WalletSurface
val CyberCardSurface: Color
    get() = WalletSurface
val CyberElevatedSurface: Color
    get() = WalletHighlightSurface

val NeonEmerald = WalletEmerald
val NeonEmeraldDim = WalletEmeraldDark
val ElectricCyan = WalletRoyalBlue
val ElectricCyanDim = WalletRoyalBlueDark
val CyberAmber = WalletAmber
val CyberMagenta = WalletPurple
val CyberRed = WalletDanger
