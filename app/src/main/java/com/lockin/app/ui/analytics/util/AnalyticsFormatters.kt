package com.lockin.app.ui.analytics.util

import androidx.compose.ui.graphics.Color
import com.lockin.app.ui.theme.AppleBlue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object AnalyticsFormatters {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    /**
     * Formats milliseconds into concise human-readable duration (e.g. "2h 15m", "45m", "< 1m").
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "0m"
        val totalMinutes = ms / 60_000L
        if (totalMinutes < 1L) return "< 1m"

        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return if (hours > 0L) {
            if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Formats milliseconds into expanded duration (e.g. "2 hours 15 mins", "45 mins").
     */
    fun formatDurationExpanded(ms: Long): String {
        if (ms <= 0L) return "0 mins"
        val totalMinutes = ms / 60_000L
        if (totalMinutes < 1L) return "Less than a min"

        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return when {
            hours > 0L && minutes > 0L -> {
                val hourLabel = if (hours == 1L) "hr" else "hrs"
                "$hours $hourLabel $minutes mins"
            }
            hours > 0L -> {
                val hourLabel = if (hours == 1L) "hr" else "hrs"
                "$hours $hourLabel"
            }
            else -> "$minutes mins"
        }
    }

    /**
     * Formats milliseconds into decimal hours (e.g. "2.4h").
     */
    fun formatHoursDecimal(ms: Long): String {
        val hours = ms / 3_600_000.0
        return String.format(Locale.US, "%.1fh", hours)
    }

    /**
     * Formats an [Instant] into user-friendly relative timestamp
     * (e.g. "Today, 4:30 PM", "Yesterday, 9:15 AM", "Oct 18, 2:00 PM").
     */
    fun formatSessionTimestamp(instant: Instant): String {
        val zone = ZoneId.systemDefault()
        val sessionDateTime = instant.atZone(zone)
        val sessionDate = sessionDateTime.toLocalDate()
        val today = LocalDate.now(zone)

        val timeString = sessionDateTime.format(timeFormatter)

        return when {
            sessionDate == today -> "Today, $timeString"
            sessionDate == today.minusDays(1) -> "Yesterday, $timeString"
            sessionDate.isAfter(today.minusDays(7)) -> "${sessionDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, $timeString"
            else -> "${sessionDate.format(monthDayFormatter)}, $timeString"
        }
    }

    /**
     * Formats ISO date "yyyy-MM-dd" into short weekday name (e.g. "Mon", "Tue").
     */
    fun formatShortWeekday(dateKey: String): String {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return "Day"
        return date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * Formats ISO date "yyyy-MM-dd" into single letter weekday (e.g. "M", "T", "W").
     */
    fun formatSingleLetterWeekday(dateKey: String): String {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return "-"
        return date.dayOfWeek.name.take(1)
    }

    /**
     * Formats ISO date "yyyy-MM-dd" into full date display (e.g. "Tuesday, Oct 20").
     */
    fun formatFullDate(dateKey: String): String {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
        val today = LocalDate.now(ZoneId.systemDefault())
        return when (date) {
            today -> "Today (${date.format(monthDayFormatter)})"
            today.minusDays(1) -> "Yesterday (${date.format(monthDayFormatter)})"
            else -> date.format(fullDateFormatter)
        }
    }

    /**
     * Safely parses hex color strings (e.g. "#0A84FF") into Compose [Color].
     */
    fun parseModuleColor(colorHex: String, fallback: Color = AppleBlue): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            fallback
        }
    }
}
