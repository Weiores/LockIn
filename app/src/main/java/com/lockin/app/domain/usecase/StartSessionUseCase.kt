package com.lockin.app.domain.usecase

import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import java.time.Instant
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(
        moduleId: Long?,
        durationMs: Long,
        triggerType: TriggerType,
        triggerValue: String? = null,
    ): Long {
        val now = Instant.now()
        val session = StudySession(
            id = 0,
            moduleId = moduleId,
            plannedDurationMs = durationMs,
            actualDurationMs = 0L,
            startTime = now,
            endTime = null,
            triggerType = triggerType,
            triggerValue = triggerValue,
            outcome = SessionOutcome.FAILED, // Default until marked COMPLETED
            breakCount = 0,
            notes = null,
        )
        val sessionId = sessionRepository.insert(session)

        val targetEpochMs = System.currentTimeMillis() + durationMs
        val snapshot = ActiveTimerSnapshot(
            sessionId = sessionId,
            state = "ActiveLocked",
            remainingMs = durationMs,
            totalMs = durationMs,
            triggerType = triggerType.name,
            targetEpochMs = targetEpochMs,
            lastTickEpochMs = System.currentTimeMillis(),
        )
        preferencesRepository.saveTimerSnapshot(snapshot)

        return sessionId
    }
}

