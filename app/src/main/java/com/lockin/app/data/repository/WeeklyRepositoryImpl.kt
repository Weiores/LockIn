package com.lockin.app.data.repository

import com.lockin.app.data.local.db.dao.WeeklyDao
import com.lockin.app.data.local.db.entity.WeeklyGoalEntity
import com.lockin.app.data.local.db.entity.WeeklyNoteEntity
import com.lockin.app.domain.model.WeeklyGoal
import com.lockin.app.domain.model.WeeklyNote
import com.lockin.app.domain.repository.WeeklyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyRepositoryImpl @Inject constructor(
    private val weeklyDao: WeeklyDao,
) : WeeklyRepository {

    override fun observeNotes(weekStart: LocalDate): Flow<List<WeeklyNote>> =
        weeklyDao.observeNotes(weekStart.toEpochDay()).map { rows ->
            rows.map { row ->
                WeeklyNote(
                    id = row.id,
                    weekStart = LocalDate.ofEpochDay(row.weekStart),
                    body = row.body,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                )
            }
        }

    override fun observeGoals(weekStart: LocalDate): Flow<List<WeeklyGoal>> =
        weeklyDao.observeGoals(weekStart.toEpochDay()).map { rows ->
            rows.map { row ->
                WeeklyGoal(
                    id = row.id,
                    weekStart = LocalDate.ofEpochDay(row.weekStart),
                    text = row.text,
                    isDone = row.isDone,
                    createdAt = row.createdAt,
                )
            }
        }

    override fun observeWeeksWithContent(): Flow<List<LocalDate>> =
        weeklyDao.observeWeeksWithContent().map { days -> days.map { LocalDate.ofEpochDay(it) } }

    override suspend fun addNote(weekStart: LocalDate, body: String): Long =
        weeklyDao.insertNote(
            WeeklyNoteEntity(weekStart = weekStart.toEpochDay(), body = body),
        )

    override suspend fun updateNote(weekStart: LocalDate, noteId: Long, body: String) {
        val existing = weeklyDao.noteById(noteId) ?: return
        weeklyDao.updateNote(
            existing.copy(
                weekStart = weekStart.toEpochDay(),
                body = body,
                updatedAt = java.time.Instant.now(),
            ),
        )
    }

    override suspend fun deleteNote(noteId: Long) {
        weeklyDao.deleteNote(noteId)
    }

    override suspend fun addGoal(weekStart: LocalDate, text: String): Long =
        weeklyDao.insertGoal(
            WeeklyGoalEntity(weekStart = weekStart.toEpochDay(), text = text),
        )

    override suspend fun setGoalDone(goalId: Long, isDone: Boolean) {
        weeklyDao.setGoalDone(goalId, isDone)
    }

    override suspend fun deleteGoal(goalId: Long) {
        weeklyDao.deleteGoal(goalId)
    }
}
