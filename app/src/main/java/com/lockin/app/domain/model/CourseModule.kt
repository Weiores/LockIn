package com.lockin.app.domain.model

import java.time.Instant

data class CourseModule(
    val id: Long = 0,
    val code: String,
    val name: String,
    val colorHex: String,
    /** Weekly focus target in minutes. 0 means "no numeric target". */
    val weeklyGoalMinutes: Int = 0,
    /** Free-text intention, e.g. "Finish the socket programming problem set". */
    val goalNote: String = "",
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false,
) {
    val hasGoal: Boolean
        get() = weeklyGoalMinutes > 0 || goalNote.isNotBlank()
}
