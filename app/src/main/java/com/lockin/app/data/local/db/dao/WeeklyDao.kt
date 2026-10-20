package com.lockin.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lockin.app.data.local.db.entity.WeeklyGoalEntity
import com.lockin.app.data.local.db.entity.WeeklyNoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Storage for the weekly journal: notes and tick-box goals. Both are keyed by
 * the ISO epoch day of the week's Monday.
 */
@Dao
interface WeeklyDao {

    @Query("SELECT * FROM weekly_notes WHERE week_start = :weekStart ORDER BY updated_at DESC")
    fun observeNotes(weekStart: Long): Flow<List<WeeklyNoteEntity>>

    @Query("SELECT * FROM weekly_goals WHERE week_start = :weekStart ORDER BY is_done ASC, created_at ASC")
    fun observeGoals(weekStart: Long): Flow<List<WeeklyGoalEntity>>

    @Query("SELECT * FROM weekly_notes WHERE id = :id")
    suspend fun noteById(id: Long): WeeklyNoteEntity?

    @Insert
    suspend fun insertNote(note: WeeklyNoteEntity): Long

    @Update
    suspend fun updateNote(note: WeeklyNoteEntity)

    @Query("DELETE FROM weekly_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Insert
    suspend fun insertGoal(goal: WeeklyGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: WeeklyGoalEntity)

    @Query("UPDATE weekly_goals SET is_done = :isDone WHERE id = :id")
    suspend fun setGoalDone(id: Long, isDone: Boolean)

    @Query("DELETE FROM weekly_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT DISTINCT week_start FROM weekly_notes UNION SELECT DISTINCT week_start FROM weekly_goals ORDER BY week_start DESC")
    fun observeWeeksWithContent(): Flow<List<Long>>
}
