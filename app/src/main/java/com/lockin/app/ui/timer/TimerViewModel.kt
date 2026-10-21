package com.lockin.app.ui.timer

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.GoalProgress
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.usecase.RecoverSessionUseCase
import com.lockin.app.hardware.nfc.NfcCardReader
import com.lockin.app.service.TimerEngine
import com.lockin.app.service.TimerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerEngine: TimerEngine,
    private val sessionRepository: SessionRepository,
    private val moduleRepository: ModuleRepository,
    private val recoverSessionUseCase: RecoverSessionUseCase,
    private val nfcCardReader: NfcCardReader,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerEngine.timerState
    val activeTriggerValue: String? get() = timerEngine.activeTriggerValue

    private val _activeModule = MutableStateFlow<CourseModule?>(null)
    val activeModule: StateFlow<CourseModule?> = _activeModule.asStateFlow()

    /** Start of the current week — the window every module goal is measured over. */
    private val weekStart: Instant = run {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val daysSinceMonday = ((today.dayOfWeek.value + 6) % 7).toLong()
        today.minusDays(daysSinceMonday).atStartOfDay(zone).toInstant()
    }

    /**
     * Weekly goal progress for the module being studied, recomputed whenever a
     * session lands so the Active Locked card is always current.
     */
    val activeModuleGoal: StateFlow<GoalProgress?> = combine(
        _activeModule,
        sessionRepository.observeSessionsSince(weekStart),
    ) { module, sessions ->
        if (module == null) {
            null
        } else {
            val focusedMs = sessions
                .filter { it.moduleId == module.id && it.outcome == SessionOutcome.COMPLETED }
                .sumOf { it.actualDurationMs }
            GoalProgress(
                moduleCode = module.code,
                note = module.goalNote,
                targetMinutes = module.weeklyGoalMinutes,
                focusedMinutes = (focusedMs / 60_000L).toInt(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var nfcJob: Job? = null

    /**
     * Why the last start/stop key was rejected. The screens surface this instead
     * of failing silently.
     */
    private val _keyError = MutableStateFlow<String?>(null)
    val keyError: StateFlow<String?> = _keyError.asStateFlow()

    fun clearKeyError() {
        _keyError.value = null
    }

    /** Raw card-UID stream, used by the unlock sheet to match the starting key. */
    fun observeCardUid(activity: Activity): kotlinx.coroutines.flow.Flow<String> =
        nfcCardReader.observeCardUid(activity)

    init {
        checkSessionRecovery()
    }

    private fun checkSessionRecovery() {
        viewModelScope.launch {
            val result = recoverSessionUseCase()
            if (result is RecoverSessionUseCase.RecoveryResult.Restored) {
                val session = sessionRepository.getById(result.sessionId)
                if (session?.moduleId != null) {
                    _activeModule.value = moduleRepository.getById(session.moduleId)
                }
            }
        }
    }

    fun loadModuleForSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getById(sessionId)
            if (session?.moduleId != null) {
                _activeModule.value = moduleRepository.getById(session.moduleId)
            } else {
                _activeModule.value = null
            }
        }
    }

    fun startNfcArmingListener(activity: Activity) {
        nfcJob?.cancel()
        _keyError.value = null
        nfcJob = viewModelScope.launch {
            nfcCardReader.observeCardUid(activity).collect { uid ->
                val expectedValue = activeTriggerValue
                if (expectedValue == null || expectedValue.equals(uid, ignoreCase = true)) {
                    unlockArmingTrigger()
                } else {
                    _keyError.value = "That card doesn't match the one you armed."
                }
            }
        }
    }

    fun stopNfcArmingListener() {
        nfcJob?.cancel()
        nfcJob = null
    }

    fun checkScannedBarcode(scannedValue: String): Boolean {
        val expectedValue = activeTriggerValue
        if (expectedValue == null || expectedValue.equals(scannedValue, ignoreCase = true)) {
            _keyError.value = null
            unlockArmingTrigger()
            return true
        }
        _keyError.value = "That barcode doesn't match the one you armed."
        return false
    }

    fun unlockArmingTrigger() {
        stopNfcArmingListener()
        timerEngine.confirmTriggerUnlocked()
    }

    fun pause() {
        timerEngine.pauseTimer()
    }

    fun resume() {
        timerEngine.resumeTimer()
    }

    /**
     * Ends the session but deliberately leaves the engine in [TimerState.Failed]
     * so the overlay can show the summary. Resetting to idle here would blow the
     * summary away and pop the screen twice.
     */
    fun stopSession(context: Context) {
        stopNfcArmingListener()
        TimerForegroundService.stopService(context)
        timerEngine.stopTimer(FailReason.USER_CANCELLED)
    }

    fun dismiss(context: Context? = null) {
        stopNfcArmingListener()
        if (context != null) {
            TimerForegroundService.stopService(context)
        }
        timerEngine.resetToIdle()
        _activeModule.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopNfcArmingListener()
    }
}
