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

    @Inject
    lateinit var distractionGuard: DistractionGuard

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var hapticHelper: HapticFeedbackHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserverJob: Job? = null
    private var distractionObserverJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "com.lockin.app.ACTION_START_TIMER"
        const val ACTION_PAUSE = "com.lockin.app.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME = "com.lockin.app.ACTION_RESUME_TIMER"
        const val ACTION_STOP = "com.lockin.app.ACTION_STOP_TIMER"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_TRIGGER_VALUE = "extra_trigger_value"
        const val EXTRA_MODULE_CODE = "extra_module_code"

        fun startService(
            context: Context,
            sessionId: Long,
            durationMs: Long,
            triggerType: TriggerType,
            moduleCode: String? = null,
            triggerValue: String? = null,
        ) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
                putExtra(EXTRA_TRIGGER_VALUE, triggerValue)
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
                val triggerValue = intent.getStringExtra(EXTRA_TRIGGER_VALUE)
                val moduleCode = intent.getStringExtra(EXTRA_MODULE_CODE)

                handleStart(sessionId, durationMs, triggerType, moduleCode, triggerValue)
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
        triggerValue: String? = null,
    ) {
        wakeLock?.acquire(durationMs + 60_000L)

        // Post initial notification immediately for foreground service contract
        val initialNotification = if (triggerType == TriggerType.FLIP || triggerType == TriggerType.BARCODE || triggerType == TriggerType.NFC) {
            notificationHelper.buildArmingNotification(triggerType, moduleCode)
        } else {
            notificationHelper.buildTimerNotification(
                remainingMs = durationMs,
                totalMs = durationMs,
                isPaused = false,
                moduleCode = moduleCode,
            )
        }

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

        timerEngine.startTimer(sessionId, durationMs, triggerType, moduleCode, triggerValue)
        observeTimerState(moduleCode)
    }

    private fun observeTimerState(moduleCode: String?) {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            var lastNotificationUpdateSec = 0L
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            timerEngine.timerState.collect { state ->
                when (state) {
                    is TimerState.ArmingTrigger -> {
                        val notification = notificationHelper.buildArmingNotification(
                            triggerType = state.triggerType,
                            moduleCode = moduleCode,
                        )
                        notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
                    }
                    is TimerState.ActiveLocked -> {
                        if (distractionObserverJob == null || distractionObserverJob?.isActive == false) {
                            startDistractionMonitoring(moduleCode)
                        }

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
                            notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
                        }
                    }
                    is TimerState.GracePeriodWarning -> {
                        val graceSec = (state.graceRemainingMs / 1000).coerceAtLeast(0L)
                        val notification = notificationHelper.buildGraceNotification(
                            graceRemainingSec = graceSec,
                            moduleCode = moduleCode,
                        )
                        notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
                    }
                    is TimerState.Completed, is TimerState.Failed -> {
                        cleanUpAndStop()
                    }
                    TimerState.Idle -> {
                        cleanUpAndStop()
                    }
                }
            }
        }
    }

    private fun startDistractionMonitoring(moduleCode: String?) {
        distractionObserverJob?.cancel()
        distractionObserverJob = serviceScope.launch {
            var lastAlertEpochMs = 0L

            preferencesRepository.blacklistedPackages.collect { blacklist ->
                distractionGuard.observeForegroundApp(1500L).collect { fgPackage ->
                    if (fgPackage != null && fgPackage != packageName && blacklist.contains(fgPackage)) {
                        val now = System.currentTimeMillis()
                        // Alert at most once every 6 seconds to avoid notification flapping
                        if (now - lastAlertEpochMs > 6000L) {
                            lastAlertEpochMs = now
                            timerEngine.incrementBreakCount()
                            hapticHelper.startGracePeriodWarningHaptic()

                            val friendlyName = when {
                                fgPackage.contains("instagram") -> "Instagram"
                                fgPackage.contains("tiktok") || fgPackage.contains("musically") -> "TikTok"
                                fgPackage.contains("twitter") || fgPackage.contains("x") -> "X (Twitter)"
                                fgPackage.contains("reddit") -> "Reddit"
                                fgPackage.contains("youtube") -> "YouTube"
                                fgPackage.contains("snapchat") -> "Snapchat"
                                fgPackage.contains("facebook") -> "Facebook"
                                else -> "Social Media"
                            }
                            notificationHelper.showDistractionWarningNotification(friendlyName, moduleCode)
                        }
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
        distractionObserverJob?.cancel()
        distractionObserverJob = null
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
