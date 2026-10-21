package com.lockin.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lockin.app.R

/**
 * Editorial serif — Playfair Display (SIL OFL 1.1, see
 * `assets/licenses/playfair-display-OFL.txt`).
 *
 * It is a display face: high stroke contrast, fine hairlines, tight apertures.
 * That makes it excellent at 22sp and above and muddy below, so it is scoped to
 * display type, screen titles, card titles and the big timers. Body copy, body
 * labels and the tracked-out micro type stay on the system sans.
 *
 * Playfair starts at Regular — there is no Light — so anything asking for
 * [FontWeight.Light] resolves to 400 rather than a synthesised thin.
 */
val LockInSerif = FontFamily(
    Font(R.font.playfair_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.playfair_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.playfair_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.playfair_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.playfair_italic, FontWeight.Normal, FontStyle.Italic),
)
