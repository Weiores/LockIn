package com.lockin.app.domain.model

import java.time.Instant
import java.time.LocalDate

/** A freeform journal entry attached to a week. */
data class WeeklyNote(
    val id: Long = 0,
    val weekStart: LocalDate,
    val body: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/** A tick-box the user wants to finish during a week. */
data class WeeklyGoal(
    val id: Long = 0,
    val weekStart: LocalDate,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Instant = Instant.now(),
)
