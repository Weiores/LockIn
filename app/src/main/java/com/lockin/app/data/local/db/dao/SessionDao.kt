package com.lockin.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockin.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY start_time DESC")
    fun observeAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE module_id = :moduleId ORDER BY start_time DESC")
    fun observeSessionsByModule(moduleId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE start_time >= :since ORDER BY start_time DESC")
    fun observeSessionsSince(since: Instant): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE outcome = 'COMPLETED' ORDER BY start_time DESC LIMIT 1")
    suspend fun getLastCompletedSession(): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions WHERE outcome = 'COMPLETED' AND start_time >= :since")
    suspend fun countCompletedSessionsSince(since: Instant): Int

    @Query("SELECT COALESCE(SUM(actual_duration_ms), 0) FROM sessions WHERE outcome = 'COMPLETED' AND start_time >= :since")
    suspend fun totalFocusTimeSince(since: Instant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)
}
