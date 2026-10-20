package com.lockin.app.ui.timer

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private var nfcJob: Job? = null

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
        nfcJob = viewModelScope.launch {
            nfcCardReader.observeCardUid(activity).collect { uid ->
                val expectedValue = activeTriggerValue
                if (expectedValue == null || expectedValue.equals(uid, ignoreCase = true)) {
                    unlockArmingTrigger()
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
        if (expectedValue == null || expectedValue == scannedValue) {
            unlockArmingTrigger()
            return true
        }
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

    fun stopSession(context: Context) {
        stopNfcArmingListener()
        TimerForegroundService.stopService(context)
        timerEngine.stopTimer(FailReason.USER_CANCELLED)
        timerEngine.resetToIdle()
        _activeModule.value = null
    }

    fun giveUp() {
        stopNfcArmingListener()
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
