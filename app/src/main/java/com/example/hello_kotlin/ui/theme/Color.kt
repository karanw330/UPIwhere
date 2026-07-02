package com.example.hello_kotlin.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object AppThemeState {
    var isDark by mutableStateOf(false)
}

// Backgrounds
val DarkBackground: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFF0D0D0F)
val DarkSurface: Color
    get() = if (AppThemeState.isDark) Color(0xFF121217) else Color(0xFF1A1A2E)
val DarkSurfaceVariant: Color
    get() = if (AppThemeState.isDark) Color(0xFF181822) else Color(0xFF16213E)

// Accents
val AccentPrimary: Color
    get() = if (AppThemeState.isDark) Color(0xFF888AFF) else Color(0xFF667EEA)
val AccentSecondary: Color
    get() = if (AppThemeState.isDark) Color(0xFFFFAE33) else Color(0xFF764BA2)
val AccentGreen: Color
    get() = if (AppThemeState.isDark) Color(0xFF00E676) else Color(0xFF00C9A7)
val AccentRed: Color
    get() = if (AppThemeState.isDark) Color(0xFFFF5252) else Color(0xFFFF6B6B)
val AccentOrange: Color
    get() = if (AppThemeState.isDark) Color(0xFFFFB74D) else Color(0xFFF39C12)

// Text
val TextPrimary: Color
    get() = if (AppThemeState.isDark) Color(0xFFECECF2) else Color(0xFFE8E8E8)
val TextSecondary: Color
    get() = if (AppThemeState.isDark) Color(0xFF9EA1B0) else Color(0xFF8B8B9A)

// Stitch Light/Dark Theme Colors
val StitchBackground: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFF8F9FF)
val StitchSurface: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFF8F9FF)
val StitchSurfaceContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFF22222E) else Color(0xFFE5EEFF)
val StitchSurfaceContainerLowest: Color
    get() = if (AppThemeState.isDark) Color(0xFF121217) else Color(0xFFFFFFFF)
val StitchSurfaceContainerLow: Color
    get() = if (AppThemeState.isDark) Color(0xFF181822) else Color(0xFFEFF4FF)
val StitchSurfaceContainerHigh: Color
    get() = if (AppThemeState.isDark) Color(0xFF2B2B3A) else Color(0xFFDCE9FF)
val StitchSurfaceContainerHighest: Color
    get() = if (AppThemeState.isDark) Color(0xFF353549) else Color(0xFFD3E4FE)
val StitchPrimary: Color
    get() = if (AppThemeState.isDark) Color(0xFF888AFF) else Color(0xFF4648D4)
val StitchPrimaryContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFFA2A4FF) else Color(0xFF6063EE)
val StitchOnPrimary: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFFFFFFF)
val StitchOnSurface: Color
    get() = if (AppThemeState.isDark) Color(0xFFECECF2) else Color(0xFF0B1C30)
val StitchOnSurfaceVariant: Color
    get() = if (AppThemeState.isDark) Color(0xFF9EA1B0) else Color(0xFF464554)
val StitchOutline: Color
    get() = if (AppThemeState.isDark) Color(0xFF5E6070) else Color(0xFF767586)
val StitchOutlineVariant: Color
    get() = if (AppThemeState.isDark) Color(0xFF3E4050) else Color(0xFFC7C4D7)
val StitchSecondary: Color
    get() = if (AppThemeState.isDark) Color(0xFFFFAE33) else Color(0xFF855300)
val StitchSecondaryContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFFFFC066) else Color(0xFFFEA619)
val StitchError: Color
    get() = if (AppThemeState.isDark) Color(0xFFFF5252) else Color(0xFFBA1A1A)
val StitchErrorContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFF3F0002) else Color(0xFFFFDAD6)
val StitchOnSecondary: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFFFFFFF)
val StitchOnSecondaryContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFFFFAE33) else Color(0xFF684000)
val StitchOnPrimaryContainer: Color
    get() = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFFFFFFF)
val StitchOnBackground: Color
    get() = if (AppThemeState.isDark) Color(0xFFECECF2) else Color(0xFF0B1C30)
val StitchAccentGreen: Color
    get() = if (AppThemeState.isDark) Color(0xFF00E676) else Color(0xFF4CAF50)

// Legacy (keeping for compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)