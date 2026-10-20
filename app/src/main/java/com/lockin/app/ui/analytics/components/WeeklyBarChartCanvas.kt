package com.lockin.app.ui.analytics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.ui.analytics.util.AnalyticsFormatters
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleBlueDark
import com.lockin.app.ui.theme.AppleGreen
import com.lockin.app.ui.theme.AppleGreenDark
import com.lockin.app.ui.theme.LocalDarkTheme
import java.time.LocalDate

/**
 * Custom Apple-style 7-day study bar chart.
 * Features animated rounded pill bars, subtle track backdrops, interactive day selection,
 * and current-day accent highlighting.
 */
@Composable
fun WeeklyBarChartCanvas(
    dailyFocusMs: Map<String, Long>,
    selectedDateKey: String?,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current
    val today = LocalDate.now().toString()
    val activeDateKey = selectedDateKey ?: today

    // Calculate maximum focus time for relative scaling (minimum scale 60 minutes)
    val maxDailyMs = (dailyFocusMs.values.maxOrNull() ?: 0L).coerceAtLeast(3_600_000L)
    val selectedMs = dailyFocusMs[activeDateKey] ?: 0L

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        // --- Header Section: Title + Dynamic Day Inspection Banner ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "WEEKLY FOCUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedContent(
                    targetState = activeDateKey to selectedMs,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "chartInspectBanner",
                ) { (dateKey, ms) ->
                    Text(
                        text = AnalyticsFormatters.formatFullDate(dateKey),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Pill with selected day's duration
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.8f)
                        else Color(0xFFE5E5EA).copy(alpha = 0.85f),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = AnalyticsFormatters.formatDuration(selectedMs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMs > 0L) {
                        if (isDark) AppleBlueDark else AppleBlue
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 7-Day Bar Columns Grid ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            dailyFocusMs.forEach { (dateKey, durationMs) ->
                val isSelected = dateKey == activeDateKey
                val isToday = dateKey == today

                val fraction = (durationMs.toFloat() / maxDailyMs.toFloat()).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 600),
                    label = "barHeight_$dateKey",
                )

                BarColumn(
                    dateKey = dateKey,
                    durationMs = durationMs,
                    animatedFraction = animatedFraction,
                    isSelected = isSelected,
                    isToday = isToday,
                    isDark = isDark,
                    onClick = { onSelectDate(dateKey) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Subtext hint ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Tap any day to inspect focus hours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun BarColumn(
    dateKey: String,
    durationMs: Long,
    animatedFraction: Float,
    isSelected: Boolean,
    isToday: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val activePrimaryColor = if (isToday) {
        if (isDark) AppleGreenDark else AppleGreen
    } else {
        if (isDark) AppleBlueDark else AppleBlue
    }

    val activeSecondaryColor = if (isToday) {
        activePrimaryColor.copy(alpha = 0.82f)
    } else {
        if (isDark) Color(0xFF64D2FF) else Color(0xFF5AC8FA)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // --- Single Bar Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp),
            ) {
                val barWidth = 14.dp.toPx()
                val left = (size.width - barWidth) / 2f
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                // 1. Draw subtle ambient background track (full height)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = cornerRadius,
                )

                // 2. Draw active filled bar (bottom-anchored)
                if (animatedFraction > 0.001f) {
                    val activeHeight = (size.height * animatedFraction).coerceAtLeast(barWidth)
                    val activeTop = size.height - activeHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(activeSecondaryColor, activePrimaryColor),
                            startY = activeTop,
                            endY = size.height,
                        ),
                        topLeft = Offset(left, activeTop),
                        size = Size(barWidth, activeHeight),
                        cornerRadius = cornerRadius,
                    )

                    // Rim highlight for selected bar
                    if (isSelected) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.4f),
                            topLeft = Offset(left, activeTop),
                            size = Size(barWidth, activeHeight),
                            cornerRadius = cornerRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Day Label ---
        Text(
            text = AnalyticsFormatters.formatShortWeekday(dateKey),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> if (isDark) AppleGreenDark else AppleGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        // Today indicator dot
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (isToday) {
                        if (isDark) AppleGreenDark else AppleGreen
                    } else {
                        Color.Transparent
                    },
                ),
        )
    }
}
