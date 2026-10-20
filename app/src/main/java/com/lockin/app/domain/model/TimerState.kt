package com.lockin.app.domain.model

sealed interface TimerState {

    data object Idle : TimerState

    data class ArmingTrigger(
        val triggerType: TriggerType,
        val moduleId: Long?,
        val plannedDurationMs: Long,
    ) : TimerState

    data class ActiveLocked(
        val sessionId: Long,
        val remainingMs: Long,
        val totalMs: Long,
        val triggerType: TriggerType,
        val isPaused: Boolean = false,
    ) : TimerState

    data class Completed(
        val sessionId: Long,
        val actualDurationMs: Long,
        val breakCount: Int,
    ) : TimerState

    data class Failed(
        val sessionId: Long,
        val actualDurationMs: Long,
        val reason: FailReason,
    ) : TimerState
}
