package com.lockin.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import com.lockin.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    /** Shared so the distraction guard can refresh its countdown notification in place. */
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

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
        if (intent == null) {
            // The system restarted us (sticky service). Re-attach to whatever is
            // still running instead of tearing it down.
            handleRestart()
            return START_STICKY
        }

        when (intent.action) {
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

        return if (timerEngine.timerState.value is TimerState.ActiveLocked ||
            timerEngine.timerState.value is TimerState.ArmingTrigger
        ) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    /**
     * Sticky-restart recovery: the engine is a @Singleton, so its state survived
     * the process restart and we only need to rebuild the observers and get the
     * foreground notification back up.
     */
    private fun handleRestart() {
        val state = timerEngine.timerState.value
        val moduleCode = timerEngine.activeModuleCode

        when (state) {
            is TimerState.ActiveLocked -> {
                if (wakeLock?.isHeld != true) {
                    wakeLock?.acquire(state.remainingMs + 60_000L)
                }
                val notification = notificationHelper.buildTimerNotification(
                    remainingMs = state.remainingMs,
                    totalMs = state.totalMs,
                    isPaused = state.isPaused,
                    moduleCode = moduleCode,
                )
                ServiceCompat.startForeground(
                    this,
                    Constants.TIMER_NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
                observeTimerState(moduleCode)
            }
            is TimerState.ArmingTrigger -> {
                val notification = notificationHelper.buildArmingNotification(
                    triggerType = state.triggerType,
                    moduleCode = moduleCode,
                )
                ServiceCompat.startForeground(
                    this,
                    Constants.TIMER_NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
                observeTimerState(moduleCode)
            }
            else -> cleanUpAndStop()
        }
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
        val initialNotification = if (triggerType == TriggerType.BARCODE || triggerType == TriggerType.NFC) {
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
                distractionGuard.observeForegroundApp(Constants.DISTRACTION_POLL_INTERVAL_MS)
                    .collect { fgPackage ->
                        if (fgPackage == null ||
                            fgPackage == packageName ||
                            !blacklist.contains(fgPackage)
                        ) {
                            return@collect
                        }

                        val now = System.currentTimeMillis()
                        // Alert at most once every 6 seconds to avoid notification flapping
                        if (now - lastAlertEpochMs <= 6000L) return@collect
                        lastAlertEpochMs = now

                        timerEngine.incrementBreakCount()
                        hapticHelper.performDistractionWarningHaptic()

                        val friendlyName = DistractionFriendlyNames.nameFor(fgPackage)
                        val graceDeadline = now + Constants.DISTRACTION_GRACE_SECONDS * 1000L

                        var stillThere = true
                        while (stillThere && System.currentTimeMillis() < graceDeadline) {
                            val secondsLeft = ((graceDeadline - System.currentTimeMillis()) / 1000L) + 1L
                            postDistractionCountdown(friendlyName, moduleCode, secondsLeft)

                            delay(1000L)

                            val currentPackage = distractionGuard.readForegroundPackageName()
                            val stillDistracted = currentPackage != null &&
                                currentPackage != packageName &&
                                blacklist.contains(currentPackage)

                            if (!stillDistracted) {
                                // The user actually left the offending app: stand down.
                                notificationHelper.cancelDistractionWarningNotification()
                                stillThere = false
                            }
                        }

                        if (!stillThere) return@collect

                        // Grace window ran out — the session dies here.
                        notificationHelper.cancelDistractionWarningNotification()
                        hapticHelper.performDistractionWarningHaptic()
                        notificationHelper.showSessionEndedByDistraction(friendlyName, moduleCode)
                        timerEngine.stopTimer(FailReason.DISTRACTION)
                        return@collect
                    }
            }
        }
    }

    private fun postDistractionCountdown(
        appName: String,
        moduleCode: String?,
        secondsLeft: Long,
    ) {
        notificationManager.notify(
            Constants.DISTRACTION_NOTIFICATION_ID,
            notificationHelper.buildDistractionWarningNotification(
                appName = appName,
                moduleCode = moduleCode,
                secondsLeft = secondsLeft,
            ),
        )
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // The user swiped the app away. The session is still theirs to finish, so
        // the foreground notification keeps counting down and a heads-up reminder
        // tells them the timer is alive out here.
        val state = timerEngine.timerState.value
        if (state is TimerState.ActiveLocked) {
            notificationHelper.showAppClosedNotification(
                remainingMs = state.remainingMs,
                moduleCode = timerEngine.activeModuleCode,
            )
            val notification = notificationHelper.buildTimerNotification(
                remainingMs = state.remainingMs,
                totalMs = state.totalMs,
                isPaused = state.isPaused,
                moduleCode = timerEngine.activeModuleCode,
            )
            notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
        } else if (state is TimerState.ArmingTrigger) {
            notificationHelper.showAppClosedNotification(
                remainingMs = state.plannedDurationMs,
                moduleCode = timerEngine.activeModuleCode,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpAndStop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
