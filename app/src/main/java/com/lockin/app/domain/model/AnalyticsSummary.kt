package com.lockin.app.domain.model

data class AnalyticsSummary(
    val totalFocusTimeMs: Long,
    val completedSessions: Int,
    val failedSessions: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val moduleBreakdown: Map<CourseModule, Long>,
    val dailyFocusMs: Map<String, Long>,
)
