package com.lockin.app.domain.model

/**
 * A module's weekly intention: what the student wants to get done and how many
 * focused minutes it should take. Shown inside the Active Locked overlay so the
 * target stays in front of them for the whole session.
 */
data class GoalProgress(
    val moduleCode: String,
    val note: String,
    val targetMinutes: Int,
    val focusedMinutes: Int,
) {
    /** A goal exists as soon as there is a target or a written intention. */
    val isSet: Boolean
        get() = targetMinutes > 0 || note.isNotBlank()

    /** 0f..1f — how much of the weekly target is already covered. */
    val fraction: Float
        get() = if (targetMinutes <= 0) 0f else (focusedMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)

    val remainingMinutes: Int
        get() = (targetMinutes - focusedMinutes).coerceAtLeast(0)
}
