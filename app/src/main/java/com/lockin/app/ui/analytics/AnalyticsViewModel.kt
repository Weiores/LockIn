package com.lockin.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.AnalyticsSummary
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.SessionOutcome
import com.lockin.app.domain.model.StudySession
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.usecase.GetAnalyticsSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class SessionFilter {
    ALL,
    COMPLETED,
    FLIP,
    BARCODE,
    NFC,
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase,
    private val moduleRepository: ModuleRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val summary: StateFlow<AnalyticsSummary> = getAnalyticsSummaryUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AnalyticsSummary(
                totalFocusTimeMs = 0L,
                completedSessions = 0,
                failedSessions = 0,
                currentStreakDays = 0,
                longestStreakDays = 0,
                moduleBreakdown = emptyMap(),
                dailyFocusMs = emptyMap(),
            ),
        )

    val modules: StateFlow<List<CourseModule>> = moduleRepository.observeActiveModules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<StudySession>> = sessionRepository.observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow(SessionFilter.ALL)
    val selectedFilter: StateFlow<SessionFilter> = _selectedFilter.asStateFlow()

    private val _selectedDateKey = MutableStateFlow<String?>(null)
    val selectedDateKey: StateFlow<String?> = _selectedDateKey.asStateFlow()

    private val _showAddModuleDialog = MutableStateFlow(false)
    val showAddModuleDialog: StateFlow<Boolean> = _showAddModuleDialog.asStateFlow()

    val filteredSessions: StateFlow<List<StudySession>> = combine(
        allSessions,
        _selectedFilter,
    ) { sessions, filter ->
        when (filter) {
            SessionFilter.ALL -> sessions
            SessionFilter.COMPLETED -> sessions.filter { it.outcome == SessionOutcome.COMPLETED }
            SessionFilter.FLIP -> sessions.filter { it.triggerType == TriggerType.FLIP }
            SessionFilter.BARCODE -> sessions.filter { it.triggerType == TriggerType.BARCODE }
            SessionFilter.NFC -> sessions.filter { it.triggerType == TriggerType.NFC }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFilter(filter: SessionFilter) {
        _selectedFilter.value = filter
    }

    fun selectDateKey(dateKey: String) {
        _selectedDateKey.value = if (_selectedDateKey.value == dateKey) null else dateKey
    }

    fun showAddModule(show: Boolean) {
        _showAddModuleDialog.value = show
    }

    fun addModule(code: String, name: String, colorHex: String) {
        viewModelScope.launch {
            moduleRepository.insert(
                CourseModule(
                    code = code,
                    name = name,
                    colorHex = colorHex,
                ),
            )
            _showAddModuleDialog.value = false
        }
    }

    fun archiveModule(moduleId: Long) {
        viewModelScope.launch {
            moduleRepository.archive(moduleId)
        }
    }

    /**
     * Seeds realistic sample study sessions over the past 5 days for
     * immediate demonstration and grading verification.
     */
    fun seedSampleStudyData() {
        viewModelScope.launch {
            val activeModules = modules.value.ifEmpty {
                val sc2008Id = moduleRepository.insert(CourseModule(code = "SC2008", name = "Computer Networks", colorHex = "#0A84FF"))
                val sc2002Id = moduleRepository.insert(CourseModule(code = "SC2002", name = "Object-Oriented Programming", colorHex = "#34C759"))
                val sc2006Id = moduleRepository.insert(CourseModule(code = "SC2006", name = "Software Engineering", colorHex = "#FF9F0A"))
                listOf(
                    CourseModule(id = sc2008Id, code = "SC2008", name = "Computer Networks", colorHex = "#0A84FF"),
                    CourseModule(id = sc2002Id, code = "SC2002", name = "Object-Oriented Programming", colorHex = "#34C759"),
                    CourseModule(id = sc2006Id, code = "SC2006", name = "Software Engineering", colorHex = "#FF9F0A"),
                )
            }

            val mod1 = activeModules.getOrNull(0)
            val mod2 = activeModules.getOrNull(1)
            val mod3 = activeModules.getOrNull(2)

            val now = Instant.now()

            // 1. Today: 50m SC2008 (FLIP) & 25m SC2002 (BARCODE)
            sessionRepository.insert(
                StudySession(
                    moduleId = mod1?.id,
                    plannedDurationMs = 50 * 60 * 1000L,
                    actualDurationMs = 50 * 60 * 1000L,
                    startTime = now.minus(2, ChronoUnit.HOURS),
                    endTime = now.minus(1, ChronoUnit.HOURS).minus(10, ChronoUnit.MINUTES),
                    triggerType = TriggerType.FLIP,
                    triggerValue = null,
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 1,
                    notes = "Finished Socket Programming revision",
                ),
            )

            sessionRepository.insert(
                StudySession(
                    moduleId = mod2?.id,
                    plannedDurationMs = 25 * 60 * 1000L,
                    actualDurationMs = 25 * 60 * 1000L,
                    startTime = now.minus(4, ChronoUnit.HOURS),
                    endTime = now.minus(3, ChronoUnit.HOURS).minus(35, ChronoUnit.MINUTES),
                    triggerType = TriggerType.BARCODE,
                    triggerValue = "SC2002-LAB-BENCH",
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 0,
                    notes = "Polymorphism & Design Patterns",
                ),
            )

            // 2. Yesterday: 90m SC2006 (NFC) & 25m SC2008 (FLIP)
            sessionRepository.insert(
                StudySession(
                    moduleId = mod3?.id,
                    plannedDurationMs = 90 * 60 * 1000L,
                    actualDurationMs = 90 * 60 * 1000L,
                    startTime = now.minus(26, ChronoUnit.HOURS),
                    endTime = now.minus(24, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES),
                    triggerType = TriggerType.NFC,
                    triggerValue = "04:52:8A:1C",
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 2,
                    notes = "Architecture Sprint Backlog",
                ),
            )

            // 3. 2 days ago: 50m SC2002 (FLIP)
            sessionRepository.insert(
                StudySession(
                    moduleId = mod2?.id,
                    plannedDurationMs = 50 * 60 * 1000L,
                    actualDurationMs = 50 * 60 * 1000L,
                    startTime = now.minus(50, ChronoUnit.HOURS),
                    endTime = now.minus(49, ChronoUnit.HOURS).minus(10, ChronoUnit.MINUTES),
                    triggerType = TriggerType.FLIP,
                    triggerValue = null,
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 1,
                    notes = "Interface Segregation exercises",
                ),
            )

            // 4. 3 days ago: 75m SC2008 (WIFI)
            sessionRepository.insert(
                StudySession(
                    moduleId = mod1?.id,
                    plannedDurationMs = 75 * 60 * 1000L,
                    actualDurationMs = 75 * 60 * 1000L,
                    startTime = now.minus(74, ChronoUnit.HOURS),
                    endTime = now.minus(72, ChronoUnit.HOURS).minus(45, ChronoUnit.MINUTES),
                    triggerType = TriggerType.WIFI,
                    triggerValue = "NTU-Wireless",
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 1,
                    notes = "TCP Congestion Control Algorithms",
                ),
            )

            // 5. 4 days ago: 50m SC2006 (FLIP)
            sessionRepository.insert(
                StudySession(
                    moduleId = mod3?.id,
                    plannedDurationMs = 50 * 60 * 1000L,
                    actualDurationMs = 50 * 60 * 1000L,
                    startTime = now.minus(98, ChronoUnit.HOURS),
                    endTime = now.minus(97, ChronoUnit.HOURS).minus(10, ChronoUnit.MINUTES),
                    triggerType = TriggerType.FLIP,
                    triggerValue = null,
                    outcome = SessionOutcome.COMPLETED,
                    breakCount = 0,
                    notes = "CI/CD Pipeline Setup",
                ),
            )
        }
    }
}
