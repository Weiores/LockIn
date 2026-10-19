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
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var currentSessionId: Long = -1L
    private var targetEpochMs: Long = 0L
    private var totalDurationMs: Long = 0L
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
        tickerJob?.cancel()
        cancelAlarm()

        currentSessionId = sessionId
        totalDurationMs = durationMs
        targetEpochMs = System.currentTimeMillis() + durationMs
        currentTriggerType = triggerType
        currentModuleCode = moduleCode
        breakCount = 0

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

    private fun startTicker() {
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
        tickerJob?.cancel()
        cancelAlarm()
        val currentState = _timerState.value
        val actualDurationMs = if (currentState is TimerState.ActiveLocked) {
            totalDurationMs - currentState.remainingMs
        } else {
            0L
        }

        val sessionId = currentSessionId
        _timerState.value = TimerState.Failed(
            sessionId = sessionId,
            actualDurationMs = actualDurationMs,
            reason = reason,
        )

        scope.launch {
            endSessionUseCase(
                sessionId = sessionId,
                outcome = SessionOutcome.CANCELLED,
                actualDurationMs = actualDurationMs,
                breakCount = breakCount,
            )
        }
    }

    fun onTimerFinished() {
        tickerJob?.cancel()
        cancelAlarm()
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
        tickerJob?.cancel()
        cancelAlarm()
        _timerState.value = TimerState.Idle
        currentSessionId = -1L
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
            // In case SCHEDULE_EXACT_ALARM is not granted, fallback to standard alarm
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
