package ru.dev.gitnews.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * GitNews Master Color Tokens
 * Quiet Luxury & Engineering Precision Design System
 */
object GitNewsPalette {
    // Dark Graphite Palette
    val DarkCanvas = Color(0xFF0B0C0E)
    val DarkSubtle = Color(0xFF111317)
    val DarkPanel = Color(0xFF181A1F)
    val DarkElevated = Color(0xFF22242A)
    val DarkGlass = Color(0xBF181A1F) // rgba(24, 26, 31, 0.75)

    // Warm Alabaster Palette
    val LightCanvas = Color(0xFFF5F5F3)
    val LightSubtle = Color(0xFFECECE9)
    val LightPanel = Color(0xFFFFFFFF)
    val LightElevated = Color(0xFFE2E2DE)
    val LightGlass = Color(0xD9FFFFFF) // rgba(255, 255, 255, 0.85)

    // Dark Borders (Sub-pixel 1px)
    val DarkBorderGhost = Color(0x0FFFFFFF)   // 6% white
    val DarkBorderSubtle = Color(0x1FFFFFFF)  // 12% white
    val DarkBorderStrong = Color(0x3DFFFFFF)  // 24% white
    val DarkBorderAccent = Color(0x80ECEEF2)  // 50% bone

    // Light Borders (Sub-pixel 1px)
    val LightBorderGhost = Color(0x0F000000)  // 6% black
    val LightBorderSubtle = Color(0x1A000000) // 10% black
    val LightBorderStrong = Color(0x33000000) // 20% black
    val LightBorderAccent = Color(0x66111215) // 40% carbon

    // Typography
    val BoneWhite = Color(0xFFECEEF2)
    val MutedTitanium = Color(0xFF8A8F98)
    val AshSlate = Color(0xFF5A5F6B)
    val CarbonInvert = Color(0xFF0B0C0E)

    val DeepCarbon = Color(0xFF111215)
    val WarmSlate = Color(0xFF5C6068)
    val MutedAsh = Color(0xFF8C9099)
    val StudioWhite = Color(0xFFFFFFFF)

    // Functional Signals (Strictly dosed 2%)
    val SignalEmerald = Color(0xFF10B981)
    val SignalEmeraldDark = Color(0xFF059669)
    val SignalGold = Color(0xFFFBBF24)
    val SignalGoldDark = Color(0xFFD97706)
    val SignalCrimson = Color(0xFFEF4444)
    val SignalCrimsonDark = Color(0xFFDC2626)
    val SignalAmber = Color(0xFFF59E0B)
    val SignalCyan = Color(0xFF06B6D4)
}

@Immutable
data class GitNewsColors(
    val surfaceCanvas: Color,
    val surfaceSubtle: Color,
    val surfacePanel: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val borderGhost: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverse: Color,
    val signalEmerald: Color,
    val signalGold: Color,
    val signalCrimson: Color,
    val signalAmber: Color,
    val signalCyan: Color,
    val isDark: Boolean
)

val DarkColorPalette = GitNewsColors(
    surfaceCanvas = GitNewsPalette.DarkCanvas,
    surfaceSubtle = GitNewsPalette.DarkSubtle,
    surfacePanel = GitNewsPalette.DarkPanel,
    surfaceElevated = GitNewsPalette.DarkElevated,
    surfaceGlass = GitNewsPalette.DarkGlass,
    borderGhost = GitNewsPalette.DarkBorderGhost,
    borderSubtle = GitNewsPalette.DarkBorderSubtle,
    borderStrong = GitNewsPalette.DarkBorderStrong,
    borderAccent = GitNewsPalette.DarkBorderAccent,
    textPrimary = GitNewsPalette.BoneWhite,
    textSecondary = GitNewsPalette.MutedTitanium,
    textTertiary = GitNewsPalette.AshSlate,
    textInverse = GitNewsPalette.CarbonInvert,
    signalEmerald = GitNewsPalette.SignalEmerald,
    signalGold = GitNewsPalette.SignalGold,
    signalCrimson = GitNewsPalette.SignalCrimson,
    signalAmber = GitNewsPalette.SignalAmber,
    signalCyan = GitNewsPalette.SignalCyan,
    isDark = true
)

val LightColorPalette = GitNewsColors(
    surfaceCanvas = GitNewsPalette.LightCanvas,
    surfaceSubtle = GitNewsPalette.LightSubtle,
    surfacePanel = GitNewsPalette.LightPanel,
    surfaceElevated = GitNewsPalette.LightElevated,
    surfaceGlass = GitNewsPalette.LightGlass,
    borderGhost = GitNewsPalette.LightBorderGhost,
    borderSubtle = GitNewsPalette.LightBorderSubtle,
    borderStrong = GitNewsPalette.LightBorderStrong,
    borderAccent = GitNewsPalette.LightBorderAccent,
    textPrimary = GitNewsPalette.DeepCarbon,
    textSecondary = GitNewsPalette.WarmSlate,
    textTertiary = GitNewsPalette.MutedAsh,
    textInverse = GitNewsPalette.StudioWhite,
    signalEmerald = GitNewsPalette.SignalEmeraldDark,
    signalGold = GitNewsPalette.SignalGoldDark,
    signalCrimson = GitNewsPalette.SignalCrimsonDark,
    signalAmber = GitNewsPalette.SignalAmber,
    signalCyan = GitNewsPalette.SignalCyan,
    isDark = false
)

val LocalGitNewsColors = staticCompositionLocalOf { DarkColorPalette }
