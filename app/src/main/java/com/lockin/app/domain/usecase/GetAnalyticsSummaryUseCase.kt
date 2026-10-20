package com.lockin.app.domain.usecase

import com.lockin.app.domain.model.AnalyticsSummary
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import java.util.TreeSet
import javax.inject.Inject

/**
 * Aggregates study session records and active academic course modules into
 * an [AnalyticsSummary] metrics object containing total focus hours, daily
 * streaks, course time distribution, and past 7-day focus progression.
 */
class GetAnalyticsSummaryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val moduleRepository: ModuleRepository,
) {

    operator fun invoke(): Flow<AnalyticsSummary> = combine(
        sessionRepository.observeAllSessions(),
        moduleRepository.observeAllModules(),
    ) { sessions, modules ->
        buildAnalyticsSummary(sessions, modules)
    }

    fun buildAnalyticsSummary(
        sessions: List<StudySession>,
        modules: List<CourseModule>,
    ): AnalyticsSummary {
        val completedSessions = sessions.filter { it.outcome == SessionOutcome.COMPLETED }
        val failedSessions = sessions.filter {
            it.outcome == SessionOutcome.FAILED || it.outcome == SessionOutcome.CANCELLED
        }

        val totalFocusTimeMs = completedSessions.sumOf { it.actualDurationMs }

        // --- Daily Streaks ---
        val zone = ZoneId.systemDefault()
        val studyDates = TreeSet<LocalDate>()
        for (session in completedSessions) {
            studyDates.add(session.startTime.atZone(zone).toLocalDate())
        }

        val today = LocalDate.now(zone)
        val currentStreak = calculateCurrentStreak(studyDates, today)
        val longestStreak = calculateLongestStreak(studyDates)

        // --- Module Breakdown ---
        val moduleMap = modules.associateBy { it.id }
        val breakdown = mutableMapOf<CourseModule, Long>()
        var unassignedMs = 0L

        for (session in completedSessions) {
            val modId = session.moduleId
            if (modId != null && moduleMap.containsKey(modId)) {
                val mod = moduleMap.getValue(modId)
                breakdown[mod] = (breakdown[mod] ?: 0L) + session.actualDurationMs
            } else {
                unassignedMs += session.actualDurationMs
            }
        }

        // Include active modules with zero logged time
        for (mod in modules.filter { !it.isArchived }) {
            if (!breakdown.containsKey(mod)) {
                breakdown[mod] = 0L
            }
        }

        // Include unassigned time if present
        if (unassignedMs > 0L) {
            val generalModule = CourseModule(
                id = -1L,
                code = "GEN",
                name = "General Focus",
                colorHex = "#8E8E93",
            )
            breakdown[generalModule] = unassignedMs
        }

        val sortedBreakdown = breakdown.toList()
            .sortedByDescending { it.second }
            .toMap(LinkedHashMap())

        // --- 7-Day Focus Progression (Past 6 days + today in order) ---
        val dailyMap = LinkedHashMap<String, Long>()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            dailyMap[date.toString()] = 0L
        }

        for (session in completedSessions) {
            val date = session.startTime.atZone(zone).toLocalDate().toString()
            if (dailyMap.containsKey(date)) {
                dailyMap[date] = (dailyMap[date] ?: 0L) + session.actualDurationMs
            }
        }

        return AnalyticsSummary(
            totalFocusTimeMs = totalFocusTimeMs,
            completedSessions = completedSessions.size,
            failedSessions = failedSessions.size,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            moduleBreakdown = sortedBreakdown,
            dailyFocusMs = dailyMap,
        )
    }

    private fun calculateCurrentStreak(studyDates: Set<LocalDate>, today: LocalDate): Int {
        if (studyDates.isEmpty()) return 0
        var streak = 0
        var checkDate = when {
            studyDates.contains(today) -> today
            studyDates.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        while (studyDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(studyDates: Set<LocalDate>): Int {
        if (studyDates.isEmpty()) return 0
        var longest = 0
        var current = 0
        var prevDate: LocalDate? = null

        for (date in studyDates) {
            if (prevDate == null) {
                current = 1
            } else if (date == prevDate.plusDays(1)) {
                current++
            } else {
                current = 1
            }
            if (current > longest) {
                longest = current
            }
            prevDate = date
        }
        return longest
    }
}
