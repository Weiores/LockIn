package com.lockin.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LockInColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = AccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = AccentCyan,
    secondary = AccentCyan,
    onSecondary = TextPrimary,
    secondaryContainer = AccentCyan.copy(alpha = 0.15f),
    onSecondaryContainer = AccentCyan,
    tertiary = AccentGreen,
    onTertiary = TextPrimary,
    error = AccentRed,
    onError = TextPrimary,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderTop,
    outlineVariant = GlassBorderBottom,
)

@Composable
fun LockInTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDark.toArgb()
            window.navigationBarColor = SurfaceDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = LockInColorScheme,
        typography = LockInTypography,
        content = content,
    )
}
