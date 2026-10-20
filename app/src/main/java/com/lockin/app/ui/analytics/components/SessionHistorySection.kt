package com.lockin.app.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.ui.analytics.SessionFilter
import com.lockin.app.ui.analytics.util.AnalyticsFormatters
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleGreen
import com.lockin.app.ui.theme.AppleOrange
import com.lockin.app.ui.theme.AppleRed
import com.lockin.app.ui.theme.LocalDarkTheme

/**
 * Session history section with filter chips and detailed session cards.
 */
@Composable
fun SessionHistorySection(
    sessions: List<StudySession>,
    modules: List<CourseModule>,
    selectedFilter: SessionFilter,
    onSelectFilter: (SessionFilter) -> Unit,
    onSeedSampleData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current
    val moduleMap = modules.associateBy { it.id }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // --- Header Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RECENT SESSIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )

            Text(
                text = "${sessions.size} recorded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // --- Filter Chips Row ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filterOptions = listOf(
                SessionFilter.ALL to "All",
                SessionFilter.COMPLETED to "Completed",
                SessionFilter.FLIP to "Flip to Lock",
                SessionFilter.BARCODE to "Barcode",
                SessionFilter.NFC to "NFC Card",
            )

            items(filterOptions) { (filter, label) ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFilter(filter) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.8f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        // --- Sessions List or Empty State ---
        if (sessions.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 24.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "No study sessions match this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Complete a focus session from the Focus Hub to see logs here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Helpful Seed action for instant UI testing/demonstration
                    GlassButton(
                        onClick = onSeedSampleData,
                        cornerRadius = 12.dp,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "Load Sample Study Data",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sessions.forEach { session ->
                    val module = session.moduleId?.let { moduleMap[it] }
                    SessionCard(
                        session = session,
                        module = module,
                        isDark = isDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: StudySession,
    module: CourseModule?,
    isDark: Boolean,
) {
    val moduleColor = if (module != null) {
        AnalyticsFormatters.parseModuleColor(module.colorHex)
    } else {
        Color(0xFF8E8E93)
    }

    val (outcomeColor, outcomeLabel, outcomeIcon) = when (session.outcome) {
        SessionOutcome.COMPLETED -> Triple(AppleGreen, "Completed", Icons.Filled.Check)
        SessionOutcome.FAILED -> Triple(AppleRed, "Interrupted", Icons.Filled.Close)
        SessionOutcome.CANCELLED -> Triple(AppleOrange, "Cancelled", Icons.Filled.Close)
    }

    val (triggerIcon, triggerLabel) = when (session.triggerType) {
        TriggerType.FLIP -> Icons.Filled.ScreenRotation to "Flip to Lock"
        TriggerType.BARCODE -> Icons.Filled.QrCodeScanner to "Barcode Scan"
        TriggerType.NFC -> Icons.Filled.Contactless to "NFC Tag"
        TriggerType.WIFI -> Icons.Filled.Wifi to "Study Zone"
        TriggerType.MANUAL -> Icons.Filled.Lock to "Manual Lock"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 16.dp,
    ) {
        // --- Row 1: Module Pill + Outcome Chip ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Module code pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(moduleColor),
                )
                Text(
                    text = module?.code ?: "General",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (module != null) {
                    Text(
                        text = "• ${module.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Outcome badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(outcomeColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = outcomeIcon,
                    contentDescription = outcomeLabel,
                    tint = outcomeColor,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = outcomeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = outcomeColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Row 2: Duration & Trigger ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Focus duration
            val durationText = if (session.outcome == SessionOutcome.COMPLETED) {
                AnalyticsFormatters.formatDuration(session.actualDurationMs)
            } else {
                "${AnalyticsFormatters.formatDuration(session.actualDurationMs)} / ${AnalyticsFormatters.formatDuration(session.plannedDurationMs)}"
            }

            Text(
                text = durationText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Trigger badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = triggerIcon,
                    contentDescription = triggerLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = triggerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Row 3: Timestamp & Note ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AnalyticsFormatters.formatSessionTimestamp(session.startTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (session.breakCount > 0) {
                Text(
                    text = "${session.breakCount} ${if (session.breakCount == 1) "break" else "breaks"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
