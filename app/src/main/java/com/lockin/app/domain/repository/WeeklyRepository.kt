package com.lockin.app.domain.repository

import com.lockin.app.domain.model.WeeklyGoal
import com.lockin.app.domain.model.WeeklyNote
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Storage for the weekly journal. Every method is scoped to a week so the UI
 * can flip between weeks with a single [observeWeek] subscription.
 */
interface WeeklyRepository {

    fun observeNotes(weekStart: LocalDate): Flow<List<WeeklyNote>>

    fun observeGoals(weekStart: LocalDate): Flow<List<WeeklyGoal>>

    /** Weeks (as week-start dates) that contain at least one note or goal. */
    fun observeWeeksWithContent(): Flow<List<LocalDate>>

    suspend fun addNote(weekStart: LocalDate, body: String): Long

    suspend fun updateNote(weekStart: LocalDate, noteId: Long, body: String)

    suspend fun deleteNote(noteId: Long)

    suspend fun addGoal(weekStart: LocalDate, text: String): Long

    suspend fun setGoalDone(goalId: Long, isDone: Boolean)

    suspend fun deleteGoal(goalId: Long)
}
