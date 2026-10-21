package com.lockin.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.domain.model.AccentScheme

/**
 * LockIn — "Daylight" design tokens.
 *
 * The language is a soft vertical wash (a tinted sky at the top that fades to
 * paper), one white card per screen, near-black ink for type, and a single
 * solid ink pill for the primary action. No glass, no rims, no glows: depth
 * comes from a wide, low-contrast shadow instead of a lit edge.
 *
 * Every surface is resolved from the active [LockInColors], which is the
 * product of a light/dark mode and one of three accent schemes. Existing
 * screens keep writing `LockInPalette.Cyan` / `GlassTokens.CardFill`; those
 * accessors are `@Composable` and read the theme from the composition.
 */

/**
 * Every colour the UI is allowed to use, resolved for one
 * (scheme, mode) combination.
 */
@Immutable
data class LockInColors(
    val isDark: Boolean,
    // Canvas — the vertical wash behind everything
    val canvasTop: Color,
    val canvasMid: Color,
    val canvasBottom: Color,
    // Ambient blooms layered over the wash
    val ambientPrimary: Color,
    val ambientSecondary: Color,
    val ambientAlert: Color,
    // Card surfaces
    val glassTop: Color,
    val glassBase: Color,
    val glassBottom: Color,
    val navSurface: Color,
    val sheetSurface: Color,
    // Accents
    val accent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    // Status
    val mint: Color,
    val amber: Color,
    val coral: Color,
    val violet: Color,
    // Ink
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Tint used for chips, hairlines and rims — ink on light, white on dark. */
    val surfaceTint: Color,
    /** Foreground that sits on top of a solid accent fill. */
    val onAccent: Color,
    val hairline: Color,
    /** Type that sits directly on the tinted top of the wash (the "Today" header). */
    val headerInk: Color,
    /** The solid pill used for primary actions. */
    val inkButton: Color,
    val onInkButton: Color,
)

// ============================================================================
//  Palette construction
// ============================================================================

private data class AccentTriple(
    val accent: Color,
    val soft: Color,
    val deep: Color,
    /** Tint of the sky at the very top of the page. */
    val sky: Color,
    /** Where the sky hands over to paper. */
    val skyFade: Color,
    /** Middle stop of the wash. */
    val mid: Color,
    /** Bottom of the wash — the page the cards sit on. */
    val bottom: Color,
    /** Bloom colour layered behind the header. */
    val bloom: Color,
)

private val BlueAccents = AccentTriple(
    accent = Color(0xFF6F93FF),
    soft = Color(0xFF9DB4FF),
    deep = Color(0xFF3E6DF3),
    sky = Color(0xFF0A1018),
    skyFade = Color(0xFF131C2C),
    mid = Color(0xFF0A1220),
    bottom = Color(0xFF101A2C),
    bloom = Color(0xFF1B2C4E),
)

private val GreenAccents = AccentTriple(
    accent = Color(0xFF5CC49A),
    soft = Color(0xFF8FD9BB),
    deep = Color(0xFF2E9E77),
    sky = Color(0xFF080F0D),
    skyFade = Color(0xFF111C19),
    mid = Color(0xFF0A1714),
    bottom = Color(0xFF0F211C),
    bloom = Color(0xFF16302A),
)

private val RedAccents = AccentTriple(
    accent = Color(0xFFF06A70),
    soft = Color(0xFFF79BA0),
    deep = Color(0xFFC43F46),
    sky = Color(0xFF0F0A0C),
    skyFade = Color(0xFF1C1317),
    mid = Color(0xFF150F12),
    bottom = Color(0xFF22171B),
    bloom = Color(0xFF33202A),
)

/** Brass / ochre — warm, ink-on-old-paper. */
private val AmberAccents = AccentTriple(
    accent = Color(0xFFE0AE5C),
    soft = Color(0xFFF0C87E),
    deep = Color(0xFFB4802F),
    sky = Color(0xFF12100A),
    skyFade = Color(0xFF1D1A10),
    mid = Color(0xFF17140C),
    bottom = Color(0xFF221D11),
    bloom = Color(0xFF332A16),
)

/** Plum — the violet end of the accent range. */
private val VioletAccents = AccentTriple(
    accent = Color(0xFFA98BFF),
    soft = Color(0xFFC4AEFF),
    deep = Color(0xFF7B57E8),
    sky = Color(0xFF0D0A16),
    skyFade = Color(0xFF171227),
    mid = Color(0xFF110D1D),
    bottom = Color(0xFF1B1530),
    bloom = Color(0xFF2A1F4A),
)

/** Deep teal — cooler than green, calmer than blue. */
private val TealAccents = AccentTriple(
    accent = Color(0xFF4FC3C0),
    soft = Color(0xFF85DAD7),
    deep = Color(0xFF2A9A97),
    sky = Color(0xFF07110F),
    skyFade = Color(0xFF0F1F1D),
    mid = Color(0xFF0A1815),
    bottom = Color(0xFF0F221E),
    bloom = Color(0xFF153430),
)

/** Light-mode accents are deeper so they hold up on paper. */
private val BlueAccentsLight = AccentTriple(
    accent = Color(0xFF3E6DF3),
    soft = Color(0xFF7C9BFA),
    deep = Color(0xFF2748C4),
    sky = Color(0xFF6E9CB6),
    skyFade = Color(0xFFA9C2D3),
    mid = Color(0xFFDCE5EF),
    bottom = Color(0xFFFBFCFE),
    bloom = Color(0xFFBFD6E4),
)

private val GreenAccentsLight = AccentTriple(
    accent = Color(0xFF2E9E77),
    soft = Color(0xFF68C4A2),
    deep = Color(0xFF1F7657),
    sky = Color(0xFF6E9A8B),
    skyFade = Color(0xFFA9C4B8),
    mid = Color(0xFFDAE7E1),
    bottom = Color(0xFFF9FCFB),
    bloom = Color(0xFFC4DACE),
)

private val RedAccentsLight = AccentTriple(
    accent = Color(0xFFDC4B52),
    soft = Color(0xFFEE868B),
    deep = Color(0xFFA82F36),
    sky = Color(0xFF9C8187),
    skyFade = Color(0xFFC9B6B9),
    mid = Color(0xFFE9DEE0),
    bottom = Color(0xFFFDFBFC),
    bloom = Color(0xFFE2CFD2),
)

private val AmberAccentsLight = AccentTriple(
    accent = Color(0xFFB4802F),
    soft = Color(0xFFD2A45E),
    deep = Color(0xFF8A5F1E),
    sky = Color(0xFFB99A62),
    skyFade = Color(0xFFD8C9A6),
    mid = Color(0xFFEBE3D3),
    bottom = Color(0xFFFDFBF5),
    bloom = Color(0xFFE8DCC2),
)

private val VioletAccentsLight = AccentTriple(
    accent = Color(0xFF6B4FD1),
    soft = Color(0xFF9A85E8),
    deep = Color(0xFF4E36A6),
    sky = Color(0xFF8F82BE),
    skyFade = Color(0xFFC0B7DD),
    mid = Color(0xFFE2DDEF),
    bottom = Color(0xFFFBFAFE),
    bloom = Color(0xFFD8CFEE),
)

private val TealAccentsLight = AccentTriple(
    accent = Color(0xFF1F8B88),
    soft = Color(0xFF55B3B0),
    deep = Color(0xFF146360),
    sky = Color(0xFF6E9C9A),
    skyFade = Color(0xFFA6C6C3),
    mid = Color(0xFFD6E6E4),
    bottom = Color(0xFFF8FCFB),
    bloom = Color(0xFFC6E0DE),
)

private fun accentsFor(scheme: AccentScheme, isDark: Boolean): AccentTriple = when (scheme) {
    AccentScheme.BLUE -> if (isDark) BlueAccents else BlueAccentsLight
    AccentScheme.GREEN -> if (isDark) GreenAccents else GreenAccentsLight
    AccentScheme.RED -> if (isDark) RedAccents else RedAccentsLight
    AccentScheme.AMBER -> if (isDark) AmberAccents else AmberAccentsLight
    AccentScheme.VIOLET -> if (isDark) VioletAccents else VioletAccentsLight
    AccentScheme.TEAL -> if (isDark) TealAccents else TealAccentsLight
}

/**
 * Resolve the full palette. Callers normally read it from the composition via
 * [LockInPalette] instead of invoking this directly.
 */
fun lockInColors(scheme: AccentScheme, isDark: Boolean): LockInColors {
    val a = accentsFor(scheme, isDark)

    return if (isDark) {
        LockInColors(
            isDark = true,
            canvasTop = a.sky,
            canvasMid = a.mid,
            canvasBottom = a.bottom,
            ambientPrimary = a.bloom,
            ambientSecondary = a.bloom,
            ambientAlert = Color(0xFF2A1619),
            glassTop = blend(a.bottom, Color.White, 0.07f),
            glassBase = blend(a.bottom, Color.White, 0.045f),
            glassBottom = blend(a.bottom, Color.Black, 0.02f),
            navSurface = blend(a.bottom, Color.White, 0.045f),
            sheetSurface = blend(a.bottom, Color.White, 0.06f),
            accent = a.accent,
            accentSoft = a.soft,
            accentDeep = a.deep,
            mint = Color(0xFF5CC49A),
            amber = Color(0xFFE0AE5C),
            coral = Color(0xFFF06A70),
            violet = Color(0xFF9E92FF),
            textPrimary = Color(0xFFEEF3FB),
            textSecondary = Color(0xFFA9B5C6),
            textTertiary = Color(0xFF8B97A9),
            surfaceTint = Color.White,
            onAccent = Color(0xFF08111C),
            hairline = Color.White.copy(alpha = 0.08f),
            headerInk = Color(0xFFF4F8FF),
            inkButton = Color(0xFFEDF2FA),
            onInkButton = Color(0xFF0B1526),
        )
    } else {
        LockInColors(
            isDark = false,
            canvasTop = a.sky,
            canvasMid = a.mid,
            canvasBottom = a.bottom,
            ambientPrimary = a.bloom,
            ambientSecondary = a.bloom,
            ambientAlert = Color(0xFFF3DDE0),
            glassTop = Color.White,
            glassBase = blend(a.bottom, Color.White, 0.72f),
            glassBottom = blend(a.bottom, Color.White, 0.55f),
            navSurface = Color.White,
            sheetSurface = Color.White,
            accent = a.accent,
            accentSoft = a.soft,
            accentDeep = a.deep,
            mint = Color(0xFF2E9E77),
            amber = Color(0xFFB47C1E),
            coral = Color(0xFFDC4B52),
            violet = Color(0xFF6B5CE0),
            textPrimary = Color(0xFF0B1526),
            textSecondary = Color(0xFF5C6A7E),
            textTertiary = Color(0xFF7B8598),
            surfaceTint = Color(0xFF0B1526),
            onAccent = Color.White,
            hairline = Color(0xFF0B1526).copy(alpha = 0.08f),
            headerInk = Color.White,
            inkButton = Color(0xFF0B1526),
            onInkButton = Color.White,
        )
    }
}

/** Default palette so previews and `@Preview` never crash without a provider. */
val LocalLockInColors = staticCompositionLocalOf { lockInColors(AccentScheme.BLUE, true) }

// ============================================================================
//  Palette accessors
// ============================================================================

/**
 * Semantic colour accessors. Each property resolves from [LocalLockInColors],
 * so any screen that reads them is automatically theme-aware.
 */
object LockInPalette {

    private val c: LockInColors
        @Composable @ReadOnlyComposable get() = LocalLockInColors.current

    val IsDark: Boolean @Composable @ReadOnlyComposable get() = c.isDark

    // ---------------------------------------------------------------- Canvas
    val Obsidian: Color @Composable @ReadOnlyComposable get() = c.canvasTop
    val Midnight: Color @Composable @ReadOnlyComposable get() = c.canvasBottom
    val CanvasMid: Color @Composable @ReadOnlyComposable get() = c.canvasMid

    /** White in light mode, off-white in dark — for type on the tinted sky. */
    val HeaderInk: Color @Composable @ReadOnlyComposable get() = c.headerInk

    // ------------------------------------------------------- Ambient glows
    val AmbientBlue: Color @Composable @ReadOnlyComposable get() = c.ambientPrimary
    val AmbientSecondary: Color @Composable @ReadOnlyComposable get() = c.ambientSecondary
    val AmbientCrimson: Color @Composable @ReadOnlyComposable get() = c.ambientAlert

    // ------------------------------------------------------------ Surfaces
    val GlassBase: Color @Composable @ReadOnlyComposable get() = c.glassBase
    val GlassTop: Color @Composable @ReadOnlyComposable get() = c.glassTop
    val GlassBottom: Color @Composable @ReadOnlyComposable get() = c.glassBottom
    val NavSurface: Color @Composable @ReadOnlyComposable get() = c.navSurface
    val SheetSurface: Color @Composable @ReadOnlyComposable get() = c.sheetSurface

    // ----------------------------------------------------------- Accents
    val Cyan: Color @Composable @ReadOnlyComposable get() = c.accent
    val SkyBlue: Color @Composable @ReadOnlyComposable get() = c.accentSoft
    val CtaBlue: Color @Composable @ReadOnlyComposable get() = c.accentDeep

    val Mint: Color @Composable @ReadOnlyComposable get() = c.mint
    val Amber: Color @Composable @ReadOnlyComposable get() = c.amber
    val Coral: Color @Composable @ReadOnlyComposable get() = c.coral
    val Violet: Color @Composable @ReadOnlyComposable get() = c.violet

    // -------------------------------------------------------------- Text
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = c.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = c.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = c.textTertiary

    /** Ink on light, white on dark — for chips, hairlines and rims. */
    val SurfaceTint: Color @Composable @ReadOnlyComposable get() = c.surfaceTint

    /** Foreground for solid accent fills (badges, selected dots). */
    val OnAccent: Color @Composable @ReadOnlyComposable get() = c.onAccent

    /** The near-black (light) / near-white (dark) pill fill. */
    val InkButton: Color @Composable @ReadOnlyComposable get() = c.inkButton
    val OnInkButton: Color @Composable @ReadOnlyComposable get() = c.onInkButton

    val Hairline: Color @Composable @ReadOnlyComposable get() = c.hairline
}

/** Corner radius scale — 30 for the one big card, 22 for rows, pill for chips. */
object GlassRadius {
    val Card = 30.dp
    val Item = 22.dp
    val Tile = 20.dp
    val Pill = 100.dp
}

// ============================================================================
//  Brushes
// ============================================================================

/**
 * Pre-built brushes so surfaces stay perfectly consistent across screens.
 * They are computed per read because the stops depend on the active theme.
 */
object GlassTokens {

    private val c: LockInColors
        @Composable @ReadOnlyComposable get() = LocalLockInColors.current

    /** Card fill — an almost-flat white sheet, slightly cooler at the bottom. */
    val CardFill: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(
            0f to c.glassTop,
            0.62f to c.glassBase,
            1f to c.glassBottom,
        )

    /** Fill for the floating nav bar. */
    val NavFill: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(
            0f to c.glassTop,
            1f to c.glassBase,
        )

    /** 1dp hairline: ink on paper, white on night. */
    val RimLight: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(
            0f to c.surfaceTint.copy(alpha = 0.10f),
            1f to c.surfaceTint.copy(alpha = 0.05f),
        )

    /** Barely-there edge for the big cards — depth comes from the shadow. */
    val CardEdge: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(
            0f to c.surfaceTint.copy(alpha = 0.05f),
            1f to c.surfaceTint.copy(alpha = 0.02f),
        )

    /** Hairline used when a card is active/selected. */
    @Composable
    @ReadOnlyComposable
    fun rimLight(accent: Color) = Brush.verticalGradient(
        0f to accent.copy(alpha = 0.55f),
        1f to accent.copy(alpha = 0.18f),
    )

    /** Primary action fill — one solid ink pill, no gradient. */
    val CtaGradient: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(0f to c.inkButton, 1f to c.inkButton)

    /** Accent gradient for small highlights. */
    val AccentGradient: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.horizontalGradient(
            0f to c.accentSoft,
            1f to c.accent,
        )

    /** Ring sweep gradient. */
    val RingSweep: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.sweepGradient(
            listOf(c.accentSoft, c.accent, c.accent, c.accentSoft),
        )

    /**
     * The page wash: tinted sky at the top, handing over to paper by ~45%.
     * Stops are explicit so the fade is smooth on every screen height.
     */
    val Canvas: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.verticalGradient(
            0f to c.canvasTop,
            0.16f to blend(c.canvasTop, c.canvasMid, 0.55f),
            0.36f to c.canvasMid,
            0.62f to blend(c.canvasMid, c.canvasBottom, 0.72f),
            1f to c.canvasBottom,
        )

    /** Bar-chart bar gradient (bright top → deep bottom). */
    @Composable
    @ReadOnlyComposable
    fun barGradient(accent: Color, highlighted: Boolean = false) = Brush.verticalGradient(
        listOf(
            if (highlighted) accent else accent.copy(alpha = 0.95f),
            accent.copy(alpha = if (highlighted) 0.80f else 0.55f),
            accent.copy(alpha = if (highlighted) 0.45f else 0.22f),
        ),
    )

    /** Shape helper for pill chips. */
    val PillShape = RoundedCornerShape(GlassRadius.Pill)
}

/** Linear blend of two opaque colours, used to shape the canvas gradient. */
private fun blend(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = 1f,
    )
}

// ============================================================================
//  Typography
// ============================================================================

/** Typography used by the surfaces (big, quiet headings; small dense labels). */
object GlassType {

    /** Page title, e.g. "Today". Editorial serif, tight tracking. */
    val Display = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.9).sp,
    )

    /** Tiny tracked-out label, e.g. "TODAY'S FOCUS". */
    val Micro = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    )

    /** Section eyebrow, e.g. "LAST 12 WEEKS". */
    val Eyebrow = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.4.sp,
    )

    /** Headline inside a card, e.g. the module name. */
    val CardTitle = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    )

    /** Running copy inside a card. */
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    )

    /** Hero countdown numerals. */
    val HeroTimer = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 58.sp,
        lineHeight = 62.sp,
        letterSpacing = (-1.6).sp,
    )

    /** Giant overlay numerals (Active Locked). */
    val GiantTimer = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 88.sp,
        lineHeight = 92.sp,
        letterSpacing = (-2.6).sp,
    )

    /** Screen title, e.g. "Trigger Vault". */
    val ScreenTitle = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Large metric value, e.g. "24.5h". */
    val MetricValue = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    )

    /** Italic serif whisper line. */
    val Whisper = TextStyle(
        fontFamily = LockInSerif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    )
}
