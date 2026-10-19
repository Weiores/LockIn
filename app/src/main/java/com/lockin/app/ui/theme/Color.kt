package com.lockin.app.ui.theme

import androidx.compose.ui.graphics.Color

// === Surface & Background ===
val SurfaceDark = Color(0xFF0D0F14)
val SurfaceDarkEnd = Color(0xFF141721)
val SurfaceCard = Color(0xFF1A1D26)

// === Glass Tokens ===
val GlassFillStart = Color.White.copy(alpha = 0.06f)
val GlassFillEnd = Color.White.copy(alpha = 0.03f)
val GlassBorderTop = Color.White.copy(alpha = 0.12f)
val GlassBorderBottom = Color.White.copy(alpha = 0.02f)
val GlassShadow = Color.Black.copy(alpha = 0.5f)

// === Accent Colors (Apple System) ===
val AccentBlue = Color(0xFF0A84FF)
val AccentCyan = Color(0xFF64D2FF)
val AccentGreen = Color(0xFF34C759)
val AccentRed = Color(0xFFFF453A)
val AccentOrange = Color(0xFFFF9F0A)
val AccentYellow = Color(0xFFFFD60A)

// === Text ===
val TextPrimary = Color.White.copy(alpha = 0.92f)
val TextSecondary = Color.White.copy(alpha = 0.55f)
val TextTertiary = Color.White.copy(alpha = 0.35f)

// === Module Tag Colors (Predefined palette) ===
val ModuleColors = listOf(
    Color(0xFF0A84FF), // Blue
    Color(0xFFFF6B35), // Orange
    Color(0xFF34C759), // Green
    Color(0xFFBF5AF2), // Purple
    Color(0xFFFF453A), // Red
    Color(0xFFFFD60A), // Yellow
    Color(0xFF64D2FF), // Cyan
    Color(0xFFFF375F), // Pink
    Color(0xFFAC8E68), // Tan
    Color(0xFF30D158), // Mint
)
