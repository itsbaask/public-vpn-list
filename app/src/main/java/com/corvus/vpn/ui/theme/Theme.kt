package com.corvus.vpn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CrowAccent,
    onPrimary = CrowBlack,
    primaryContainer = CrowAccentBorder,
    onPrimaryContainer = CrowAccentText,
    secondary = CrowMuted,
    onSecondary = CrowText,
    tertiary = CrowGold,
    onTertiary = CrowBlack,
    background = CrowBlack,
    onBackground = CrowText,
    surface = CrowSurface,
    onSurface = CrowText,
    surfaceVariant = CrowCore,
    onSurfaceVariant = CrowMuted,
    outline = CrowBorder,
    outlineVariant = CrowBorder,
    error = CrowBlood,
    onError = CrowText
)

private val LightColorScheme = lightColorScheme(
    primary = CrowAccent,
    onPrimary = CrowBlack,
    primaryContainer = CrowAccentBorder,
    onPrimaryContainer = CrowAccentText,
    secondary = CrowMuted,
    onSecondary = CrowText,
    tertiary = CrowGold,
    onTertiary = CrowBlack,
    background = CrowBlack,
    onBackground = CrowText,
    surface = CrowSurface,
    onSurface = CrowText,
    surfaceVariant = CrowCore,
    onSurfaceVariant = CrowMuted,
    outline = CrowBorder,
    outlineVariant = CrowBorder,
    error = CrowBlood,
    onError = CrowText
)

@Composable
fun CorvusVPNTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDark
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}