package com.lockin.app.domain.repository

import com.lockin.app.domain.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SessionRepository {
    fun observeAllSessions(): Flow<List<StudySession>>
    fun observeSessionsByModule(moduleId: Long): Flow<List<StudySession>>
    fun observeSessionsSince(since: Instant): Flow<List<StudySession>>
    suspend fun getById(id: Long): StudySession?
    suspend fun getLastCompletedSession(): StudySession?
    suspend fun countCompletedSessionsSince(since: Instant): Int
    suspend fun totalFocusTimeSince(since: Instant): Long
    suspend fun insert(session: StudySession): Long
    suspend fun update(session: StudySession)
}
