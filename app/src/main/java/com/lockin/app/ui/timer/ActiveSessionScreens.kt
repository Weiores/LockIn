package com.lockin.app.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.domain.model.GoalProgress
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.model.displayName
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassMicroLabel
import com.lockin.app.ui.components.GlassPill
import com.lockin.app.ui.components.GlowingRingCanvas
import com.lockin.app.ui.components.StatusDot
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette

/**
 * Screen 2 — Active Locked (fullscreen overlay).
 *
 * The countdown, the module you are supposed to be studying, the weekly goal
 * you set for it, and the physical key required to get out early.
 */
@Composable
fun ActiveLockedScreen(
    state: TimerState.ActiveLocked,
    moduleCode: String?,
    goal: GoalProgress?,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onUnlockEarly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (state.totalMs > 0L) {
        (state.remainingMs.toFloat() / state.totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // The current session counts towards the weekly goal the moment it starts.
    val elapsedMinutes = ((state.totalMs - state.remainingMs).coerceAtLeast(0L) / 60_000L).toInt()
    val liveGoal = goal?.let { it.copy(focusedMinutes = it.focusedMinutes + elapsedMinutes) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ---------------------------------------------------------- header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatusDot(color = LockInPalette.Mint, size = 5.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCKED IN",
                    style = GlassType.Micro,
                    color = LockInPalette.HeaderInk.copy(alpha = 0.85f),
                )
            }

            // ---------------------------------------------------------- centre
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer halo ring (the second ring of the double dial)
                    GlowingRingCanvas(
                        progress = progress,
                        size = 292.dp,
                        strokeWidth = 1.dp,
                        glowStrength = 0.10f,
                        trackColor = LockInPalette.SurfaceTint.copy(alpha = 0.07f),
                        showNode = false,
                    )

                    // Primary dial
                    GlowingRingCanvas(
                        progress = progress,
                        size = 264.dp,
                        strokeWidth = 2.5.dp,
                        glowStrength = 0.22f,
                        trackColor = LockInPalette.SurfaceTint.copy(alpha = 0.09f),
                        showNode = false,
                    ) {
                        Text(
                            text = formatCountdown(state.remainingMs),
                            style = GlassType.GiantTimer,
                            color = LockInPalette.TextPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                GlassPill(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    StatusDot(color = LockInPalette.Cyan, size = 4.dp, halo = false)
                    Text(
                        text = "Studying: ${moduleCode ?: "General Focus"}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = LockInPalette.TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                GoalCard(
                    goal = liveGoal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---------------------------------------------------------- bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostIconPill(
                        icon = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        label = if (state.isPaused) "Resume" else "Pause",
                        onClick = onTogglePause,
                        tint = LockInPalette.TextSecondary,
                    )
                    GhostIconPill(
                        icon = Icons.Filled.Stop,
                        label = "Stop",
                        onClick = onStop,
                        tint = LockInPalette.Coral.copy(alpha = 0.85f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    contentPadding = 15.dp,
                    onClick = onUnlockEarly,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = if (state.triggerType == TriggerType.NFC) {
                                    Icons.Filled.Contactless
                                } else {
                                    Icons.Filled.QrCodeScanner
                                },
                                contentDescription = null,
                                tint = LockInPalette.Cyan,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(11.dp))
                            Text(
                                text = "Unlock Early",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = LockInPalette.TextSecondary,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.triggerType.displayName,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = LockInPalette.Cyan.copy(alpha = 0.85f),
                                maxLines = 1,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = LockInPalette.TextTertiary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
//  Goal card
// ============================================================================

/**
 * The module's weekly intention, kept on screen for the whole session.
 * Shows the written goal, the minutes already banked and a live progress bar.
 */
@Composable
private fun GoalCard(
    goal: GoalProgress?,
    modifier: Modifier = Modifier,
) {
    val accent = LockInPalette.Cyan
    val isSet = goal?.isSet == true

    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = 14.dp,
        tint = if (isSet) accent.copy(alpha = 0.06f) else null,
        borderBrush = if (isSet) GlassTokens.rimLight(accent) else GlassTokens.RimLight,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = if (isSet) accent else LockInPalette.TextTertiary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            GlassMicroLabel(
                text = "THIS WEEK'S GOAL",
                color = LockInPalette.TextTertiary,
                style = GlassType.Eyebrow,
                modifier = Modifier.weight(1f),
            )
            if (goal != null && goal.targetMinutes > 0) {
                Text(
                    text = "${goal.focusedMinutes}m / ${goal.targetMinutes}m",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSet) accent else LockInPalette.TextTertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = when {
                goal == null -> "No module selected for this session."
                goal.note.isNotBlank() -> goal.note
                goal.targetMinutes > 0 -> "Bank ${goal.targetMinutes} focused minutes this week."
                else -> "No goal set yet — add one from the Focus Hub."
            },
            fontSize = 12.5.sp,
            fontWeight = if (isSet) FontWeight.Medium else FontWeight.Normal,
            lineHeight = 17.sp,
            color = if (isSet) LockInPalette.TextPrimary else LockInPalette.TextTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (goal != null && goal.targetMinutes > 0) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(GlassRadius.Pill))
                    .background(LockInPalette.SurfaceTint.copy(alpha = 0.07f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(goal.fraction.coerceAtLeast(0.015f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(GlassRadius.Pill))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.55f), accent),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    goal.fraction >= 1f -> "Goal reached — anything else is a bonus."
                    goal.remainingMinutes > 0 -> "${goal.remainingMinutes}m left to hit this week's target."
                    else -> "Keep the streak alive."
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = LockInPalette.TextTertiary,
                maxLines = 1,
            )
        }
    }
}

// ============================================================================
//  Pieces
// ============================================================================

@Composable
private fun GhostIconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .background(LockInPalette.SurfaceTint.copy(alpha = 0.045f))
            .border(
                width = 1.dp,
                brush = GlassTokens.RimLight,
                shape = RoundedCornerShape(GlassRadius.Pill),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
    }
}

// ============================================================================
//  Formatting
// ============================================================================

internal fun formatCountdown(remainingMs: Long): String {
    val safeMs = remainingMs.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
