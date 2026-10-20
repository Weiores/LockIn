package com.lockin.app.ui.focushub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.ThemeMode
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.repository.TriggerRepository
import com.lockin.app.domain.usecase.StartSessionUseCase
import com.lockin.app.hardware.wifi.WifiZoneDetector
import com.lockin.app.hardware.wifi.WifiZoneInfo
import com.lockin.app.service.TimerEngine
import com.lockin.app.service.TimerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class FocusHubViewModel @Inject constructor(    private val moduleRepository: ModuleRepository,
    private val sessionRepository: SessionRepository,
    private val triggerRepository: TriggerRepository,
    private val preferencesRepository: PreferencesRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val timerEngine: TimerEngine,
    private val wifiZoneDetector: WifiZoneDetector,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerEngine.timerState

    /** Start of the current week — the window every module goal is measured over. */
    private val zone: ZoneId = ZoneId.systemDefault()

    private val weekStart: Instant = run {
        val today = LocalDate.now(zone)
        val daysSinceMonday = ((today.dayOfWeek.value + 6) % 7).toLong()
        today.minusDays(daysSinceMonday).atStartOfDay(zone).toInstant()
    }

    /** Start of the seven-day window shown in the hub's timeline strip. */
    private val weekStripStart: Instant =
        LocalDate.now(zone).minusDays(6).atStartOfDay(zone).toInstant()

    val modules: StateFlow<List<CourseModule>> = moduleRepository.observeActiveModules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Completed focus minutes per module for the current week. */
    val weeklyFocusByModule: StateFlow<Map<Long, Int>> = sessionRepository
        .observeSessionsSince(weekStart)
        .map { sessions ->
            sessions
                .filter { it.outcome == SessionOutcome.COMPLETED && it.moduleId != null }
                .groupBy { it.moduleId!! }
                .mapValues { (_, list) -> (list.sumOf { it.actualDurationMs } / 60_000L).toInt() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** One column per day for the hub's seven-day strip, oldest first. */
    val lastSevenDays: StateFlow<List<DayFocus>> = sessionRepository
        .observeSessionsSince(weekStripStart)
        .map { sessions ->
            val minutesByDay = sessions
                .filter { it.outcome == SessionOutcome.COMPLETED }
                .groupBy { it.startTime.atZone(zone).toLocalDate() }
                .mapValues { (_, list) -> (list.sumOf { it.actualDurationMs } / 60_000L).toInt() }

            val today = LocalDate.now(zone)
            (6 downTo 0).map { daysBack ->
                val date = today.minusDays(daysBack.toLong())
                DayFocus(
                    letter = date.dayOfWeek.name.take(1),
                    dayOfMonth = date.dayOfMonth,
                    minutes = minutesByDay[date] ?: 0,
                    isToday = daysBack == 0,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTriggers: StateFlow<List<TriggerProfile>> = triggerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentWifi: StateFlow<WifiZoneInfo> = wifiZoneDetector.observeCurrentWifi()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WifiZoneInfo(isConnected = false))

    val activeStudyZone: StateFlow<TriggerProfile?> = combine(
        currentWifi,
        savedTriggers,
    ) { wifi, triggers ->
        val wifiZones = triggers.filter { it.type == TriggerType.WIFI }
        wifiZoneDetector.findMatchingZone(wifi, wifiZones)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        preselectFirstModule()
    }

    /** Start on the first module so the goal strip is visible without a tap. */
    private fun preselectFirstModule() {
        viewModelScope.launch {
            val first = modules.first { it.isNotEmpty() }.firstOrNull() ?: return@launch
            if (_selectedModule.value == null) {
                _selectedModule.value = first
            }
        }
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
            preferencesRepository.setThemeMode(if (currentDark) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }

    /** Persist the weekly intention for a module. */
    fun setModuleGoal(module: CourseModule, goalMinutes: Int, goalNote: String) {
        viewModelScope.launch {
            moduleRepository.update(
                module.copy(
                    weeklyGoalMinutes = goalMinutes.coerceAtLeast(0),
                    goalNote = goalNote.trim(),
                ),
            )
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

/** One column of the hub's seven-day strip. */
data class DayFocus(
    val letter: String,
    val dayOfMonth: Int,
    val minutes: Int,
    val isToday: Boolean,
)
