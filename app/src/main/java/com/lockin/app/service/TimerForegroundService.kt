package com.lockin.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject
    lateinit var timerEngine: TimerEngine

    @Inject
    lateinit var notificationHelper: TimerNotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserverJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "com.lockin.app.ACTION_START_TIMER"
        const val ACTION_PAUSE = "com.lockin.app.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME = "com.lockin.app.ACTION_RESUME_TIMER"
        const val ACTION_STOP = "com.lockin.app.ACTION_STOP_TIMER"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_MODULE_CODE = "extra_module_code"

        fun startService(
            context: Context,
            sessionId: Long,
            durationMs: Long,
            triggerType: TriggerType,
            moduleCode: String? = null,
        ) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
                putExtra(EXTRA_MODULE_CODE, moduleCode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LockIn:TimerWakeLock",
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 25 * 60 * 1000L)
                val triggerTypeName = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: TriggerType.MANUAL.name
                val triggerType = TriggerType.valueOf(triggerTypeName)
                val moduleCode = intent.getStringExtra(EXTRA_MODULE_CODE)

                handleStart(sessionId, durationMs, triggerType, moduleCode)
            }
            ACTION_PAUSE -> {
                timerEngine.pauseTimer()
            }
            ACTION_RESUME -> {
                timerEngine.resumeTimer()
            }
            ACTION_STOP -> {
                handleStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStart(
        sessionId: Long,
        durationMs: Long,
        triggerType: TriggerType,
        moduleCode: String?,
    ) {
        wakeLock?.acquire(durationMs + 60_000L)

        // Post initial notification immediately for foreground service contract
        val initialNotification = notificationHelper.buildTimerNotification(
            remainingMs = durationMs,
            totalMs = durationMs,
            isPaused = false,
            moduleCode = moduleCode,
        )

        ServiceCompat.startForeground(
            this,
            Constants.TIMER_NOTIFICATION_ID,
            initialNotification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        timerEngine.startTimer(sessionId, durationMs, triggerType, moduleCode)
        observeTimerState(moduleCode)
    }

    private fun observeTimerState(moduleCode: String?) {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            var lastNotificationUpdateSec = 0L

            timerEngine.timerState.collect { state ->
                when (state) {
                    is TimerState.ActiveLocked -> {
                        val currentRemainingSec = state.remainingMs / 1000
                        // Throttle notification updates to once per 5 seconds or when paused to save system resources
                        if (state.isPaused || Math.abs(currentRemainingSec - lastNotificationUpdateSec) >= 5) {
                            lastNotificationUpdateSec = currentRemainingSec
                            val notification = notificationHelper.buildTimerNotification(
                                remainingMs = state.remainingMs,
                                totalMs = state.totalMs,
                                isPaused = state.isPaused,
                                moduleCode = moduleCode,
                            )
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
                        }
                    }
                    is TimerState.Completed, is TimerState.Failed -> {
                        cleanUpAndStop()
                    }
                    TimerState.Idle -> {
                        cleanUpAndStop()
                    }
                    is TimerState.GracePeriodWarning, is TimerState.ArmingTrigger -> {
                        // Will be expanded in Phase 3
                    }
                }
            }
        }
    }

    private fun handleStop() {
        timerEngine.stopTimer()
        cleanUpAndStop()
    }

    private fun cleanUpAndStop() {
        stateObserverJob?.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpAndStop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
