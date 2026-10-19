package com.lockin.app.domain.usecase

import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import java.time.Instant
import javax.inject.Inject

class EndSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        outcome: SessionOutcome,
        actualDurationMs: Long,
        breakCount: Int = 0,
        notes: String? = null,
    ) {
        val existing = sessionRepository.getById(sessionId)
        if (existing != null) {
            val updated = existing.copy(
                actualDurationMs = actualDurationMs,
                endTime = Instant.now(),
                outcome = outcome,
                breakCount = breakCount,
                notes = notes ?: existing.notes,
            )
            sessionRepository.update(updated)
        }
        preferencesRepository.clearTimerSnapshot()
    }
}
