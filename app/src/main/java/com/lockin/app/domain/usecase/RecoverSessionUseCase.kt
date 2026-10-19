package com.lockin.app.domain.usecase

import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class RecoverSessionUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val endSessionUseCase: EndSessionUseCase,
) {
    sealed interface RecoveryResult {
        data object None : RecoveryResult
        data class Restored(
            val sessionId: Long,
            val remainingMs: Long,
            val totalMs: Long,
            val triggerType: String,
        ) : RecoveryResult
        data class CompletedWhileKilled(val sessionId: Long) : RecoveryResult
    }

    suspend operator fun invoke(): RecoveryResult {
        val snapshot: ActiveTimerSnapshot = preferencesRepository.activeTimerSnapshot.firstOrNull()
            ?: return RecoveryResult.None

        val now = System.currentTimeMillis()
        val remainingMs = snapshot.targetEpochMs - now

        return if (remainingMs > 0) {
            RecoveryResult.Restored(
                sessionId = snapshot.sessionId,
                remainingMs = remainingMs,
                totalMs = snapshot.totalMs,
                triggerType = snapshot.triggerType,
            )
        } else {
            // Timer expired while app process was dead
            endSessionUseCase(
                sessionId = snapshot.sessionId,
                outcome = SessionOutcome.COMPLETED,
                actualDurationMs = snapshot.totalMs,
            )
            RecoveryResult.CompletedWhileKilled(snapshot.sessionId)
        }
    }
}
