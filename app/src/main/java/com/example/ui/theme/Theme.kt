package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = Color(0xFF042F2E),
    onPrimaryContainer = NeonCyan,
    secondary = CyberAmber,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = CyberAmber,
    tertiary = CyberPink,
    background = ObsidianBlack,
    onBackground = Color(0xFFF8FAFC),
    surface = TerminalSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = TerminalBorder
)

private val LightColorScheme = lightColorScheme(
    primary = RetroLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = CyberOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = CyberPink,
    background = RetroLightBg,
    onBackground = Color(0xFF0F172A),
    surface = RetroLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = RetroLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = RetroLightOutline
)

@Composable
fun CleanLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
