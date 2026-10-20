package com.lockin.app.ui.theme

import androidx.compose.ui.graphics.Color

// === Background & Surface (Pure Apple Neutrals, No Harsh Artificial Gradients) ===
val BackgroundDark = Color(0xFF000000) // Pure OLED Black
val SurfaceDark = Color(0xFF121214)
val SurfaceCardDark = Color(0xFF1C1C1E)

val BackgroundLight = Color(0xFFF2F2F7) // Apple System Grouped Background
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceCardLight = Color(0xFFFFFFFF)

// === Glass Translucency Tokens ===
val GlassFillDark = Color(0xFF1C1C1E).copy(alpha = 0.72f)
val GlassFillLight = Color(0xFFFFFFFF).copy(alpha = 0.78f)

val GlassBorderDark = Color.White.copy(alpha = 0.08f)
val GlassBorderLight = Color.Black.copy(alpha = 0.05f)

// === Authentic Apple System Accents ===
val AppleBlue = Color(0xFF007AFF)
val AppleBlueDark = Color(0xFF0A84FF)

val AppleGreen = Color(0xFF34C759)
val AppleGreenDark = Color(0xFF30D158)

val AppleRed = Color(0xFFFF3B30)
val AppleRedDark = Color(0xFFFF453A)

val AppleOrange = Color(0xFFFF9500)
val AppleOrangeDark = Color(0xFFFF9F0A)

val ApplePurple = Color(0xFFAF52DE)
val ApplePurpleDark = Color(0xFFBF5AF2)

// === Typography Colors ===
val TextPrimaryDark = Color(0xFFF5F5F7)
val TextSecondaryDark = Color(0xFF8E8E93)
val TextTertiaryDark = Color(0xFF636366)

val TextPrimaryLight = Color(0xFF1D1D1F)
val TextSecondaryLight = Color(0xFF86868B)
val TextTertiaryLight = Color(0xFFAEAEB2)

// === Course Module Palette (Refined Apple System Pastels/Neutrals) ===
val ModuleColors = listOf(
    Color(0xFF0A84FF), // Blue
    Color(0xFF30D158), // Green
    Color(0xFFFF9F0A), // Orange
    Color(0xFFBF5AF2), // Purple
    Color(0xFFFF453A), // Red
    Color(0xFF64D2FF), // Cyan
    Color(0xFFFF375F), // Pink
    Color(0xFFFFD60A), // Yellow
)
