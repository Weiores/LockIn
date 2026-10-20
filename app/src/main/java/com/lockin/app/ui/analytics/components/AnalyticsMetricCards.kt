package com.lockin.app.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.domain.model.AnalyticsSummary
import com.lockin.app.ui.analytics.util.AnalyticsFormatters
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleBlueDark
import com.lockin.app.ui.theme.AppleGreen
import com.lockin.app.ui.theme.AppleGreenDark
import com.lockin.app.ui.theme.AppleOrange
import com.lockin.app.ui.theme.AppleOrangeDark
import com.lockin.app.ui.theme.ApplePurple
import com.lockin.app.ui.theme.ApplePurpleDark
import com.lockin.app.ui.theme.LocalDarkTheme

/**
 * 2x2 Apple Glass Grid of top summary statistics for student focus habits.
 */
@Composable
fun AnalyticsMetricCards(
    summary: AnalyticsSummary,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current

    val totalSessions = summary.completedSessions + summary.failedSessions
    val completionRate = if (totalSessions > 0) {
        ((summary.completedSessions.toDouble() / totalSessions.toDouble()) * 100).toInt()
    } else {
        100
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Row 1: Focus Time & Current Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(
                icon = Icons.Filled.Timer,
                iconTint = if (isDark) AppleBlueDark else AppleBlue,
                value = AnalyticsFormatters.formatDuration(summary.totalFocusTimeMs),
                label = "Focus Time",
                subtext = "Total completed study",
                modifier = Modifier.weight(1f),
            )

            MetricTile(
                icon = Icons.Filled.LocalFireDepartment,
                iconTint = if (isDark) AppleOrangeDark else AppleOrange,
                value = "${summary.currentStreakDays} ${if (summary.currentStreakDays == 1) "Day" else "Days"}",
                label = "Daily Streak",
                subtext = if (summary.currentStreakDays > 0) "Streak active 🔥" else "Start a session today",
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Completed Sessions & Best Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(
                icon = Icons.Filled.CheckCircle,
                iconTint = if (isDark) AppleGreenDark else AppleGreen,
                value = "${summary.completedSessions}",
                label = "Completed",
                subtext = "$completionRate% success rate",
                modifier = Modifier.weight(1f),
            )

            MetricTile(
                icon = Icons.Filled.EmojiEvents,
                iconTint = if (isDark) ApplePurpleDark else ApplePurple,
                value = "${summary.longestStreakDays} ${if (summary.longestStreakDays == 1) "Day" else "Days"}",
                label = "Best Streak",
                subtext = "All-time record",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    subtext: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current

    GlassCard(
        modifier = modifier,
        contentPadding = 16.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (isDark) 0.20f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
