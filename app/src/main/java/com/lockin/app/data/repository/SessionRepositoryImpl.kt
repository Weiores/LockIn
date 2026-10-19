package com.lockin.app.data.repository

import com.lockin.app.data.local.db.dao.SessionDao
import com.lockin.app.data.local.db.entity.SessionEntity
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
) : SessionRepository {

    override fun observeAllSessions(): Flow<List<StudySession>> =
        sessionDao.observeAllSessions().map { list -> list.map { it.toDomain() } }

    override fun observeSessionsByModule(moduleId: Long): Flow<List<StudySession>> =
        sessionDao.observeSessionsByModule(moduleId).map { list -> list.map { it.toDomain() } }

    override fun observeSessionsSince(since: Instant): Flow<List<StudySession>> =
        sessionDao.observeSessionsSince(since).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): StudySession? =
        sessionDao.getById(id)?.toDomain()

    override suspend fun getLastCompletedSession(): StudySession? =
        sessionDao.getLastCompletedSession()?.toDomain()

    override suspend fun countCompletedSessionsSince(since: Instant): Int =
        sessionDao.countCompletedSessionsSince(since)

    override suspend fun totalFocusTimeSince(since: Instant): Long =
        sessionDao.totalFocusTimeSince(since)

    override suspend fun insert(session: StudySession): Long =
        sessionDao.insert(session.toEntity())

    override suspend fun update(session: StudySession) =
        sessionDao.update(session.toEntity())

    private fun SessionEntity.toDomain() = StudySession(
        id = id,
        moduleId = moduleId,
        plannedDurationMs = plannedDurationMs,
        actualDurationMs = actualDurationMs,
        startTime = startTime,
        endTime = endTime,
        triggerType = TriggerType.valueOf(triggerType),
        triggerValue = triggerValue,
        outcome = SessionOutcome.valueOf(outcome),
        breakCount = breakCount,
        notes = notes,
    )

    private fun StudySession.toEntity() = SessionEntity(
        id = id,
        moduleId = moduleId,
        plannedDurationMs = plannedDurationMs,
        actualDurationMs = actualDurationMs,
        startTime = startTime,
        endTime = endTime,
        triggerType = triggerType.name,
        triggerValue = triggerValue,
        outcome = outcome.name,
        breakCount = breakCount,
        notes = notes,
    )
}

