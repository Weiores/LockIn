package com.lockin.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.usecase.RecoverSessionUseCase
import com.lockin.app.service.TimerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerEngine.timerState

    private val _activeModule = MutableStateFlow<CourseModule?>(null)
    val activeModule: StateFlow<CourseModule?> = _activeModule.asStateFlow()

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

    fun pause() {
        timerEngine.pauseTimer()
    }

    fun resume() {
        timerEngine.resumeTimer()
    }

    fun giveUp() {
        timerEngine.stopTimer(FailReason.USER_CANCELLED)
    }

    fun dismiss() {
        timerEngine.resetToIdle()
        _activeModule.value = null
    }
}
