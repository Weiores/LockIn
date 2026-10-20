package com.lockin.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDarkTheme = compositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.White,
    primaryContainer = AppleBlueDark.copy(alpha = 0.18f),
    onPrimaryContainer = AppleBlueDark,
    secondary = AppleOrangeDark,
    onSecondary = Color.White,
    tertiary = AppleGreenDark,
    onTertiary = Color.White,
    error = AppleRedDark,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorderDark,
    outlineVariant = Color.White.copy(alpha = 0.04f),
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlue.copy(alpha = 0.12f),
    onPrimaryContainer = AppleBlue,
    secondary = AppleOrange,
    onSecondary = Color.White,
    tertiary = AppleGreen,
    onTertiary = Color.White,
    error = AppleRed,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = GlassBorderLight,
    outlineVariant = Color.Black.copy(alpha = 0.03f),
)

@Composable
fun LockInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LockInTypography,
            content = content,
        )
    }
}
