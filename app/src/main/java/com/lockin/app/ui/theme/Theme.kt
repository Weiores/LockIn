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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode

/** Legacy flag kept for screens that only need to know the resolved mode. */
val LocalDarkTheme = compositionLocalOf { true }

/**
 * LockIn theming.
 *
 * Two independent axes:
 *  - [ThemeMode]  — follow the system, or pin light / dark.
 *  - [AccentScheme] — blue, green or red, all rendered with muted saturation so
 *    the glass stays calm instead of glowing.
 *
 * Everything downstream reads `LockInPalette.*` / `GlassTokens.*`, which pull
 * their values out of [LocalLockInColors], so a mode or scheme change repaints
 * the whole app without touching a single screen.
 */
@Composable
fun LockInTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    scheme: AccentScheme = AccentScheme.BLUE,
    content: @Composable () -> Unit,
) {
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = lockInColors(scheme, isDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            // Both bars stay transparent: the page wash is painted at window
            // level, so it runs edge to edge behind the status and nav bars
            // instead of stopping at a solid band.
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Android adds a contrast scrim over a transparent status bar when
            // this is left at its default (true) — in light mode that scrim is
            // white, which is the pale band that hid the clock. Opt out of both
            // so the wash is what actually shows.
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false

            WindowCompat.getInsetsController(window, view).apply {
                // The sky is a mid tone: ink icons read at ~4.3:1 in light mode,
                // white would sit at ~2.7:1. Icons follow the mode.
                isAppearanceLightStatusBars = !colors.isDark
                isAppearanceLightNavigationBars = !colors.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalLockInColors provides colors,
        LocalDarkTheme provides colors.isDark,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = LockInTypography,
            content = content,
        )
    }
}

/** Bridge the glass palette into a Material 3 scheme (dialogs, text fields, switches). */
private fun LockInColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = accent,
        secondary = mint,
        onSecondary = onAccent,
        tertiary = amber,
        onTertiary = onAccent,
        error = coral,
        onError = onAccent,
        background = canvasBottom,
        onBackground = textPrimary,
        surface = sheetSurface,
        onSurface = textPrimary,
        surfaceVariant = glassTop,
        onSurfaceVariant = textSecondary,
        outline = surfaceTint.copy(alpha = 0.10f),
        outlineVariant = surfaceTint.copy(alpha = 0.04f),
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.14f),
        onPrimaryContainer = accentDeep,
        secondary = mint,
        onSecondary = onAccent,
        tertiary = amber,
        onTertiary = onAccent,
        error = coral,
        onError = onAccent,
        background = canvasBottom,
        onBackground = textPrimary,
        surface = sheetSurface,
        onSurface = textPrimary,
        surfaceVariant = glassBottom,
        onSurfaceVariant = textSecondary,
        outline = surfaceTint.copy(alpha = 0.12f),
        outlineVariant = surfaceTint.copy(alpha = 0.06f),
    )
}
