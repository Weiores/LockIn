package com.lockin.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.usecase.EndSessionUseCase
import com.lockin.app.hardware.flip.FlipDetector
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val endSessionUseCase: EndSessionUseCase,
    private val notificationHelper: TimerNotificationHelper,
    private val flipDetector: FlipDetector,
    private val hapticHelper: HapticFeedbackHelper,
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private var flipJob: Job? = null
    private var graceJob: Job? = null

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var currentSessionId: Long = -1L
    private var targetEpochMs: Long = 0L
    private var totalDurationMs: Long = 0L
    private var sessionRemainingBeforeGraceMs: Long = 0L
    private var currentTriggerType: TriggerType = TriggerType.MANUAL
    private var currentModuleCode: String? = null
    private var breakCount: Int = 0

    companion object {
        const val ACTION_ALARM_COMPLETED = "com.lockin.app.ACTION_ALARM_COMPLETED"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }

    fun startTimer(
        sessionId: Long,
        durationMs: Long,
        triggerType: TriggerType,
        moduleCode: String? = null,
    ) {
        cancelAllJobs()
        cancelAlarm()
        hapticHelper.cancelHaptics()

        currentSessionId = sessionId
        totalDurationMs = durationMs
        currentTriggerType = triggerType
        currentModuleCode = moduleCode
        breakCount = 0

        if (triggerType == TriggerType.FLIP) {
            _timerState.value = TimerState.ArmingTrigger(
                triggerType = TriggerType.FLIP,
                moduleId = null,
                plannedDurationMs = durationMs,
            )
            startFlipObserver()
        } else {
            targetEpochMs = System.currentTimeMillis() + durationMs
            _timerState.value = TimerState.ActiveLocked(
                sessionId = sessionId,
                remainingMs = durationMs,
                totalMs = durationMs,
                triggerType = triggerType,
                isPaused = false,
            )
            scheduleAlarm(targetEpochMs)
            startTicker()
        }
    }

    private fun startFlipObserver() {
        flipJob?.cancel()
        flipJob = scope.launch {
            flipDetector.observeFlipState().collect { flipState ->
                val currentState = _timerState.value
                when (flipState) {
                    is FlipDetector.FlipState.FaceDown -> {
                        when (currentState) {
                            is TimerState.ArmingTrigger -> {
                                // Locked in! Phone placed face-down
                                hapticHelper.performLockSuccessHaptic()
                                targetEpochMs = System.currentTimeMillis() + totalDurationMs
                                _timerState.value = TimerState.ActiveLocked(
                                    sessionId = currentSessionId,
                                    remainingMs = totalDurationMs,
                                    totalMs = totalDurationMs,
                                    triggerType = currentTriggerType,
                                    isPaused = false,
                                )
                                scheduleAlarm(targetEpochMs)
                                startTicker()
                            }
                            is TimerState.GracePeriodWarning -> {
                                // Streak restored! Phone placed back face-down in time
                                graceJob?.cancel()
                                graceJob = null
                                hapticHelper.cancelHaptics()
                                hapticHelper.performLockSuccessHaptic()
                                breakCount++

                                targetEpochMs = System.currentTimeMillis() + sessionRemainingBeforeGraceMs
                                _timerState.value = TimerState.ActiveLocked(
                                    sessionId = currentSessionId,
                                    remainingMs = sessionRemainingBeforeGraceMs,
                                    totalMs = totalDurationMs,
                                    triggerType = currentTriggerType,
                                    isPaused = false,
                                )
                                scheduleAlarm(targetEpochMs)
                                startTicker()
                            }
                            else -> Unit
                        }
                    }
                    is FlipDetector.FlipState.FaceUp -> {
                        if (currentState is TimerState.ActiveLocked && !currentState.isPaused) {
                            // Phone lifted during active flip session!
                            tickerJob?.cancel()
                            cancelAlarm()

                            val now = System.currentTimeMillis()
                            val remaining = (targetEpochMs - now).coerceAtLeast(0L)
                            sessionRemainingBeforeGraceMs = remaining

                            val graceTotalMs = 10_000L
                            _timerState.value = TimerState.GracePeriodWarning(
                                sessionId = currentSessionId,
                                graceRemainingMs = graceTotalMs,
                                sessionRemainingMs = remaining,
                                totalMs = totalDurationMs,
                            )

                            hapticHelper.startGracePeriodWarningHaptic()
                            startGraceCountdown(graceTotalMs)
                        }
                    }
                    FlipDetector.FlipState.Unknown -> Unit
                }
            }
        }
    }

    private fun startGraceCountdown(totalGraceMs: Long) {
        graceJob?.cancel()
        graceJob = scope.launch {
            val graceTarget = System.currentTimeMillis() + totalGraceMs
            while (isActive) {
                delay(200)
                val now = System.currentTimeMillis()
                val remainingGrace = graceTarget - now

                if (remainingGrace <= 0) {
                    // Grace period expired — break the session
                    hapticHelper.cancelHaptics()
                    cancelAllJobs()

                    val actualDurationMs = (totalDurationMs - sessionRemainingBeforeGraceMs).coerceAtLeast(0L)
                    val sessionId = currentSessionId

                    _timerState.value = TimerState.Failed(
                        sessionId = sessionId,
                        actualDurationMs = actualDurationMs,
                        reason = FailReason.GRACE_EXPIRED,
                    )

                    endSessionUseCase(
                        sessionId = sessionId,
                        outcome = SessionOutcome.FAILED,
                        actualDurationMs = actualDurationMs,
                        breakCount = breakCount,
                    )
                    break
                } else {
                    _timerState.value = TimerState.GracePeriodWarning(
                        sessionId = currentSessionId,
                        graceRemainingMs = remainingGrace,
                        sessionRemainingMs = sessionRemainingBeforeGraceMs,
                        totalMs = totalDurationMs,
                    )
                }
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(500)
                val now = System.currentTimeMillis()
                val remaining = targetEpochMs - now

                if (remaining <= 0) {
                    onTimerFinished()
                    break
                } else {
                    _timerState.value = TimerState.ActiveLocked(
                        sessionId = currentSessionId,
                        remainingMs = remaining,
                        totalMs = totalDurationMs,
                        triggerType = currentTriggerType,
                        isPaused = false,
                    )
                }
            }
        }
    }

    fun pauseTimer() {
        val currentState = _timerState.value
        if (currentState is TimerState.ActiveLocked && !currentState.isPaused) {
            tickerJob?.cancel()
            cancelAlarm()
            val now = System.currentTimeMillis()
            val remaining = (targetEpochMs - now).coerceAtLeast(0L)
            _timerState.value = currentState.copy(
                remainingMs = remaining,
                isPaused = true,
            )
        }
    }

    fun resumeTimer() {
        val currentState = _timerState.value
        if (currentState is TimerState.ActiveLocked && currentState.isPaused) {
            targetEpochMs = System.currentTimeMillis() + currentState.remainingMs
            _timerState.value = currentState.copy(isPaused = false)
            scheduleAlarm(targetEpochMs)
            startTicker()
        }
    }

    fun stopTimer(reason: FailReason = FailReason.USER_CANCELLED) {
        cancelAllJobs()
        cancelAlarm()
        hapticHelper.cancelHaptics()

        val currentState = _timerState.value
        val actualDurationMs = when (currentState) {
            is TimerState.ActiveLocked -> (totalDurationMs - currentState.remainingMs).coerceAtLeast(0L)
            is TimerState.GracePeriodWarning -> (totalDurationMs - currentState.sessionRemainingMs).coerceAtLeast(0L)
            else -> 0L
        }

        val sessionId = currentSessionId
        val outcome = if (reason == FailReason.GRACE_EXPIRED) SessionOutcome.FAILED else SessionOutcome.CANCELLED

        _timerState.value = TimerState.Failed(
            sessionId = sessionId,
            actualDurationMs = actualDurationMs,
            reason = reason,
        )

        scope.launch {
            endSessionUseCase(
                sessionId = sessionId,
                outcome = outcome,
                actualDurationMs = actualDurationMs,
                breakCount = breakCount,
            )
        }
    }

    fun onTimerFinished() {
        cancelAllJobs()
        cancelAlarm()
        hapticHelper.cancelHaptics()
        hapticHelper.performLockSuccessHaptic()

        val sessionId = currentSessionId
        val totalMs = totalDurationMs
        val module = currentModuleCode

        _timerState.value = TimerState.Completed(
            sessionId = sessionId,
            actualDurationMs = totalMs,
            breakCount = breakCount,
        )

        scope.launch {
            endSessionUseCase(
                sessionId = sessionId,
                outcome = SessionOutcome.COMPLETED,
                actualDurationMs = totalMs,
                breakCount = breakCount,
            )
            notificationHelper.showCompletionNotification(module)
        }
    }

    fun incrementBreakCount() {
        breakCount++
    }

    fun resetToIdle() {
        cancelAllJobs()
        cancelAlarm()
        hapticHelper.cancelHaptics()
        _timerState.value = TimerState.Idle
        currentSessionId = -1L
    }

    private fun cancelAllJobs() {
        tickerJob?.cancel()
        tickerJob = null
        graceJob?.cancel()
        graceJob = null
        flipJob?.cancel()
        flipJob = null
    }

    private fun scheduleAlarm(triggerAtMillis: Long) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_COMPLETED
            putExtra(EXTRA_SESSION_ID, currentSessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun cancelAlarm() {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_COMPLETED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
