package com.example.hello_kotlin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.ColorScheme

private val DarkColorScheme: ColorScheme
    get() = darkColorScheme(
        primary = AccentPrimary,
        secondary = AccentSecondary,
        tertiary = AccentGreen,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        error = AccentRed
    )

private val LightColorScheme: ColorScheme
    get() = lightColorScheme(
        primary = StitchPrimary,
        onPrimary = StitchOnPrimary,
        primaryContainer = StitchPrimaryContainer,
        onPrimaryContainer = StitchOnPrimaryContainer,
        secondary = StitchSecondary,
        onSecondary = StitchOnSecondary,
        secondaryContainer = StitchSecondaryContainer,
        onSecondaryContainer = StitchOnSecondaryContainer,
        background = StitchBackground,
        onBackground = StitchOnBackground,
        surface = StitchSurface,
        onSurface = StitchOnSurface,
        surfaceVariant = StitchSurfaceContainerLow,
        onSurfaceVariant = StitchOnSurfaceVariant,
        outline = StitchOutline,
        outlineVariant = StitchOutlineVariant,
        error = StitchError,
        errorContainer = StitchErrorContainer,
        onTertiary = Color.White
    )

@Composable
fun Hello_kotlinTheme(
    darkTheme: Boolean = false, // Default to light mode (Stitch style)
    dynamicColor: Boolean = false, // Disable dynamic color for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = StitchBackground.toArgb()
            window.navigationBarColor = StitchSurfaceContainerLowest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}