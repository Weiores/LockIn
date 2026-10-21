package com.lockin.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.AnalyticsSummary
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.ui.analytics.components.AddModuleDialog
import com.lockin.app.ui.analytics.util.AnalyticsFormatters
import com.lockin.app.ui.components.GlassBadge
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassDivider
import com.lockin.app.ui.components.GlassIconButton
import com.lockin.app.ui.components.GlassMicroLabel
import com.lockin.app.ui.components.GlassPill
import com.lockin.app.ui.components.StatusDot
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Screen 5 — Study Analytics.
 *
 * Three headline metrics, a "focus in rhythm" card (12-week heatmap + 7-day
 * bar chart), course breakdown and the recent session log.
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val filteredSessions by viewModel.filteredSessions.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedDateKey by viewModel.selectedDateKey.collectAsState()
    val showAddModuleDialog by viewModel.showAddModuleDialog.collectAsState()

    val totalSessions = summary.completedSessions + summary.failedSessions
    val lockInRate = if (totalSessions > 0) {
        (summary.completedSessions * 100) / totalSessions
    } else {
        0
    }

    val (thisWeekMs, lastWeekMs) = remember(allSessions) { weekOverWeek(allSessions) }
    val deltaLabel = when {
        lastWeekMs <= 0L && thisWeekMs <= 0L -> "No data yet"
        lastWeekMs <= 0L -> "↑ New this week"
        else -> {
            val pct = ((thisWeekMs - lastWeekMs) * 100.0 / lastWeekMs).toInt()
            if (pct >= 0) "↑ +$pct%" else "↓ $pct%"
        }
    }

    val heatmap = remember(allSessions) { buildHeatmap(allSessions) }
    val weeklyKeys = summary.dailyFocusMs.keys.toList()

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ------------------------------------------------------------ header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Study Analytics",
                        style = GlassType.ScreenTitle,
                        color = LockInPalette.HeaderInk,
                    )
                    GlassIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Add course module",
                        onClick = { viewModel.showAddModule(true) },
                        size = 42.dp,
                        tint = LockInPalette.HeaderInk,
                        onSky = true,
                    )
                }
            }

            // ----------------------------------------------------------- metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricTile(
                        icon = Icons.Filled.Timer,
                        iconTint = LockInPalette.Cyan,
                        value = AnalyticsFormatters.formatHoursDecimal(summary.totalFocusTimeMs),
                        label = "Total Focus",
                        subLabel = deltaLabel,
                        subColor = LockInPalette.Cyan,
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        icon = Icons.Filled.CheckCircle,
                        iconTint = LockInPalette.Mint,
                        value = if (totalSessions > 0) "$lockInRate%" else "—",
                        label = "Lock-In Rate",
                        subLabel = when {
                            totalSessions == 0 -> "No sessions"
                            lockInRate >= 80 -> "● On track"
                            else -> "● Needs focus"
                        },
                        subColor = if (totalSessions > 0 && lockInRate >= 80) {
                            LockInPalette.Mint
                        } else {
                            LockInPalette.Amber
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = LockInPalette.Amber,
                        value = "${summary.currentStreakDays}d",
                        label = "Streak",
                        subLabel = "This week",
                        subColor = LockInPalette.Amber,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ------------------------------------------------------------ rhythm
            item {
                FocusRhythmCard(
                    summary = summary,
                    heatmap = heatmap,
                    weeklyKeys = weeklyKeys,
                    selectedDateKey = selectedDateKey,
                    onSelectDate = { viewModel.selectDateKey(it) },
                )
            }

            // ------------------------------------------------- course breakdown
            item {
                CourseBreakdownCard(
                    moduleBreakdown = summary.moduleBreakdown,
                    onAddModule = { viewModel.showAddModule(true) },
                )
            }

            // ---------------------------------------------------- recent sessions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent Sessions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LockInPalette.TextPrimary,
                    )
                    GlassBadge(text = "${filteredSessions.size} LOGGED")
                }
            }

            item {
                FilterChipRow(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { viewModel.selectFilter(it) },
                )
            }

            if (filteredSessions.isEmpty()) {
                item {
                    EmptyAnalyticsCard(
                        hasAnyData = summary.totalFocusTimeMs > 0L,
                        onSeedSampleData = { viewModel.seedSampleStudyData() },
                    )
                }
            } else {
                items(filteredSessions.take(6), key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        moduleCode = modules.firstOrNull { it.id == session.moduleId }?.code,
                    )
                }
            }
        }
    }

    if (showAddModuleDialog) {
        AddModuleDialog(
            onDismiss = { viewModel.showAddModule(false) },
            onConfirm = { code, name, colorHex ->
                viewModel.addModule(code, name, colorHex)
            },
        )
    }
}

// ============================================================================
//  Metrics
// ============================================================================

@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    subLabel: String,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = 0.9f),
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                color = LockInPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = LockInPalette.TextTertiary,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = subColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ============================================================================
//  Rhythm card
// ============================================================================

@Composable
private fun FocusRhythmCard(
    summary: AnalyticsSummary,
    heatmap: List<List<Float>>,
    weeklyKeys: List<String>,
    selectedDateKey: String?,
    onSelectDate: (String) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Card,
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your focus, in rhythm",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
            )
            GlassBadge(text = "THIS WEEK")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeatmapGrid(
                heatmap = heatmap,
                modifier = Modifier.weight(1.25f),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                GlassMicroLabel(
                    text = "LAST 12 WEEKS",
                    color = LockInPalette.TextTertiary,
                    style = GlassType.Eyebrow,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Keep going.",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.3).sp,
                    color = LockInPalette.TextPrimary,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Small sessions. Real progress.",
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = LockInPalette.TextTertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GlassDivider()
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassMicroLabel(
                text = "HOURS STUDIED",
                color = LockInPalette.TextTertiary,
                style = GlassType.Eyebrow,
            )
            Text(
                text = "${AnalyticsFormatters.formatHoursDecimal(summary.dailyFocusMs.values.sum())} this week",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = LockInPalette.Cyan,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        WeeklyBarRow(
            weeklyKeys = weeklyKeys,
            dailyFocusMs = summary.dailyFocusMs,
            selectedDateKey = selectedDateKey,
            onSelectDate = onSelectDate,
        )
    }
}

@Composable
private fun HeatmapGrid(
    heatmap: List<List<Float>>,
    modifier: Modifier = Modifier,
) {
    val dayLabels = listOf("M", "", "W", "", "F", "", "")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        heatmap.forEachIndexed { rowIndex, week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = dayLabels.getOrElse(rowIndex) { "" },
                    fontSize = 7.sp,
                    color = LockInPalette.TextTertiary,
                    modifier = Modifier.width(8.dp),
                )
                week.forEach { intensity ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(heatColor(intensity)),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarRow(
    weeklyKeys: List<String>,
    dailyFocusMs: Map<String, Long>,
    selectedDateKey: String?,
    onSelectDate: (String) -> Unit,
) {
    val values = weeklyKeys.map { dailyFocusMs[it] ?: 0L }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val highlightedKey = selectedDateKey ?: weeklyKeys.getOrNull(values.indexOf(maxValue))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeklyKeys.forEachIndexed { index, dateKey ->
            val valueMs = values[index]
            val isHighlighted = dateKey == highlightedKey
            val fraction = (valueMs.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            val barHeight = (8f + fraction * 54f).dp

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectDate(dateKey) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (valueMs > 0L) AnalyticsFormatters.formatHoursDecimal(valueMs) else "–",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isHighlighted) LockInPalette.Cyan else LockInPalette.TextTertiary,
                    maxLines = 1,
                )

                Spacer(modifier = Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(5.dp))
                        .background(GlassTokens.barGradient(LockInPalette.Cyan, isHighlighted)),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AnalyticsFormatters.formatSingleLetterWeekday(dateKey),
                    fontSize = 8.5.sp,
                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isHighlighted) LockInPalette.TextPrimary else LockInPalette.TextTertiary,
                )
            }
        }
    }
}

// ============================================================================
//  Course breakdown
// ============================================================================

@Composable
private fun CourseBreakdownCard(
    moduleBreakdown: Map<CourseModule, Long>,
    onAddModule: () -> Unit,
) {
    val topModules = moduleBreakdown.entries
        .sortedByDescending { it.value }
        .take(3)
    val maxMs = topModules.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Card,
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Course Breakdown",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBadge(text = "THIS WEEK")
                Spacer(modifier = Modifier.width(6.dp))
                GlassIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add course module",
                    onClick = onAddModule,
                    size = 24.dp,
                    tint = LockInPalette.TextTertiary,
                )
            }
        }

        if (topModules.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Add a course module to see how your hours split up.",
                fontSize = 10.5.sp,
                color = LockInPalette.TextTertiary,
            )
        }

        topModules.forEach { (module, timeMs) ->
            val fallback = LockInPalette.Cyan
            val moduleColor = remember(module.colorHex, fallback) {
                runCatching { Color(android.graphics.Color.parseColor(module.colorHex)) }
                    .getOrDefault(fallback)
            }
            val fraction = (timeMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(color = moduleColor, size = 4.dp, halo = false)
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "${module.code} ${module.name}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LockInPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = AnalyticsFormatters.formatHoursDecimal(timeMs),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockInPalette.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(GlassRadius.Pill))
                    .background(LockInPalette.SurfaceTint.copy(alpha = 0.05f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(GlassRadius.Pill))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    moduleColor.copy(alpha = 0.55f),
                                    moduleColor,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

// ============================================================================
//  Recent sessions
// ============================================================================

@Composable
private fun FilterChipRow(
    selectedFilter: SessionFilter,
    onSelectFilter: (SessionFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
    ) {
        items(SessionFilter.entries) { filter ->
            val isSelected = filter == selectedFilter
            GlassPill(
                selected = isSelected,
                accent = LockInPalette.Cyan,
                onClick = { onSelectFilter(filter) },
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    text = filter.label(),
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) LockInPalette.TextPrimary else LockInPalette.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: StudySession,
    moduleCode: String?,
) {
    val completed = session.outcome == SessionOutcome.COMPLETED
    val accent = if (completed) LockInPalette.Mint else LockInPalette.Coral

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Item,
        contentPadding = 13.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                StatusDot(color = accent, size = 5.dp, halo = false)
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moduleCode ?: "General Focus",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LockInPalette.TextPrimary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = AnalyticsFormatters.formatSessionTimestamp(session.startTime),
                    fontSize = 9.5.sp,
                    color = LockInPalette.TextTertiary,
                    maxLines = 1,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AnalyticsFormatters.formatDuration(session.actualDurationMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockInPalette.TextPrimary,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = session.triggerType.label(),
                    fontSize = 9.sp,
                    color = LockInPalette.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsCard(
    hasAnyData: Boolean,
    onSeedSampleData: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Item,
        contentPadding = 20.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (hasAnyData) "No sessions match this filter" else "No sessions logged yet",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (hasAnyData) {
                    "Try a different filter to see your history."
                } else {
                    "Finish a focus session and your rhythm starts building here."
                },
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = LockInPalette.TextTertiary,
                textAlign = TextAlign.Center,
            )

            if (!hasAnyData) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Load sample week",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockInPalette.Cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(GlassRadius.Pill))
                        .background(LockInPalette.Cyan.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            brush = GlassTokens.rimLight(LockInPalette.Cyan),
                            shape = RoundedCornerShape(GlassRadius.Pill),
                        )
                        .clickable(onClick = onSeedSampleData)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ============================================================================
//  Helpers
// ============================================================================

private fun SessionFilter.label(): String = when (this) {
    SessionFilter.ALL -> "All"
    SessionFilter.COMPLETED -> "Completed"
    SessionFilter.MANUAL -> "Manual"
    SessionFilter.BARCODE -> "Barcode"
    SessionFilter.NFC -> "NFC"
}

private fun TriggerType.label(): String = when (this) {
    TriggerType.BARCODE -> "Barcode"
    TriggerType.NFC -> "Card tap"
    TriggerType.WIFI -> "Wi-Fi"
    TriggerType.MANUAL -> "Manual"
}

@Composable
private fun heatColor(intensity: Float): Color {
    val accent = LockInPalette.Cyan
    val empty = LockInPalette.SurfaceTint.copy(alpha = 0.045f)
    return when {
        intensity <= 0f -> empty
        intensity < 0.25f -> accent.copy(alpha = 0.18f)
        intensity < 0.5f -> accent.copy(alpha = 0.34f)
        intensity < 0.75f -> accent.copy(alpha = 0.58f)
        else -> accent.copy(alpha = 0.90f)
    }
}

/** 7 rows (Mon..Sun) x 12 columns (oldest week → current week), normalised 0..1. */
private fun buildHeatmap(sessions: List<StudySession>): List<List<Float>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val startOfWeek = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val firstWeekStart = startOfWeek.minusWeeks(11)

    val perDayMs = HashMap<LocalDate, Long>()
    sessions.filter { it.outcome == SessionOutcome.COMPLETED }.forEach { session ->
        val date = session.startTime.atZone(zone).toLocalDate()
        perDayMs[date] = (perDayMs[date] ?: 0L) + session.actualDurationMs
    }
    val maxMs = perDayMs.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    return (0 until 7).map { dayIndex ->
        (0 until 12).map { weekIndex ->
            val date = firstWeekStart.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
            when {
                date.isAfter(today) -> 0f
                else -> ((perDayMs[date] ?: 0L).toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
}

/** Completed focus milliseconds for the last 7 days vs the 7 days before that. */
private fun weekOverWeek(sessions: List<StudySession>): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    var thisWeek = 0L
    var lastWeek = 0L

    sessions.filter { it.outcome == SessionOutcome.COMPLETED }.forEach { session ->
        val date = session.startTime.atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(date, today)
        when {
            daysAgo in 0..6 -> thisWeek += session.actualDurationMs
            daysAgo in 7..13 -> lastWeek += session.actualDurationMs
        }
    }
    return thisWeek to lastWeek
}
