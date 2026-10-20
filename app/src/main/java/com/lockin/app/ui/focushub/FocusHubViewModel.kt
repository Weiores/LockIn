package com.lockin.app.ui.focushub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.repository.TriggerRepository
import com.lockin.app.domain.usecase.StartSessionUseCase
import com.lockin.app.service.TimerEngine
import com.lockin.app.service.TimerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class FocusHubViewModel @Inject constructor(
    private val moduleRepository: ModuleRepository,
    private val sessionRepository: SessionRepository,
    private val triggerRepository: TriggerRepository,
    private val preferencesRepository: PreferencesRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val timerEngine: TimerEngine,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerEngine.timerState

    val isDarkMode: StateFlow<Boolean?> = preferencesRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val modules: StateFlow<List<CourseModule>> = moduleRepository.observeActiveModules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTriggers: StateFlow<List<TriggerProfile>> = triggerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedModule = MutableStateFlow<CourseModule?>(null)
    val selectedModule: StateFlow<CourseModule?> = _selectedModule.asStateFlow()

    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _selectedTriggerType = MutableStateFlow(TriggerType.MANUAL)
    val selectedTriggerType: StateFlow<TriggerType> = _selectedTriggerType.asStateFlow()

    private val _selectedTriggerProfile = MutableStateFlow<TriggerProfile?>(null)
    val selectedTriggerProfile: StateFlow<TriggerProfile?> = _selectedTriggerProfile.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    init {
        seedInitialModulesIfEmpty()
        calculateStreak()
    }

    private fun seedInitialModulesIfEmpty() {
        viewModelScope.launch {
            val existing = moduleRepository.getById(1)
            if (existing == null) {
                moduleRepository.insert(
                    CourseModule(code = "SC2008", name = "Computer Networks", colorHex = "#0A84FF"),
                )
                moduleRepository.insert(
                    CourseModule(code = "SC2002", name = "Object-Oriented Programming", colorHex = "#34C759"),
                )
                moduleRepository.insert(
                    CourseModule(code = "SC2006", name = "Software Engineering", colorHex = "#FF9F0A"),
                )
            }
        }
    }

    private fun calculateStreak() {
        viewModelScope.launch {
            val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)
            val count = sessionRepository.countCompletedSessionsSince(oneDayAgo)
            _streakDays.value = if (count > 0) 1 else 0
        }
    }

    fun toggleDarkMode(currentDark: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(!currentDark)
        }
    }

    fun selectModule(module: CourseModule?) {
        _selectedModule.value = if (_selectedModule.value?.id == module?.id) null else module
    }

    fun selectDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
    }

    fun selectTriggerType(triggerType: TriggerType) {
        _selectedTriggerType.value = triggerType
        _selectedTriggerProfile.value = null
    }

    fun selectSavedTrigger(trigger: TriggerProfile) {
        _selectedTriggerProfile.value = if (_selectedTriggerProfile.value?.id == trigger.id) null else trigger
        if (_selectedTriggerProfile.value != null) {
            _selectedTriggerType.value = trigger.type
        } else {
            _selectedTriggerType.value = TriggerType.MANUAL
        }
    }

    fun startSession(
        context: Context,
        onSessionStarted: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val durationMs = _selectedDurationMinutes.value * 60 * 1000L
            val moduleId = _selectedModule.value?.id
            val moduleCode = _selectedModule.value?.code
            val trigger = _selectedTriggerType.value
            val triggerValue = _selectedTriggerProfile.value?.value

            val sessionId = startSessionUseCase(
                moduleId = moduleId,
                durationMs = durationMs,
                triggerType = trigger,
                triggerValue = triggerValue,
            )

            TimerForegroundService.startService(
                context = context,
                sessionId = sessionId,
                durationMs = durationMs,
                triggerType = trigger,
                moduleCode = moduleCode,
                triggerValue = triggerValue,
            )

            onSessionStarted(sessionId)
        }
    }

    fun stopActiveSession(context: Context) {
        TimerForegroundService.stopService(context)
        timerEngine.stopTimer(FailReason.USER_CANCELLED)
        timerEngine.resetToIdle()
    }
}
