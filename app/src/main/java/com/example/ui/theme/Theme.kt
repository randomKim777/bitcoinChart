package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CleanMinimalColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalPrimaryLight,
    tertiary = KimchiGoldAccent,
    background = MinimalBg,
    surface = CardStandardBg,
    onPrimary = Color.White,
    onSecondary = MinimalTextPrimary,
    onBackground = MinimalTextPrimary,
    onSurface = MinimalTextPrimary,
    surfaceVariant = CardUpbitBg,
    onSurfaceVariant = MinimalTextSecondary,
    outline = BorderMinimal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Clean Minimalism Light Theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = CleanMinimalColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MinimalBg.toArgb()
            window.navigationBarColor = MinimalBg.toArgb()
            // Set status bar and navigation bar to light style (dark icons)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
