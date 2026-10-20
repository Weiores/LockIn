package com.lockin.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.WeeklyGoal
import com.lockin.app.domain.model.WeeklyNote
import com.lockin.app.domain.repository.WeeklyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val weeklyRepository: WeeklyRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val currentWeekStart: LocalDate = startOfWeek(today)

    private val _weekStart = MutableStateFlow(currentWeekStart)

    /** Week currently on screen. */
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()

    /** True when the screen is showing the week containing today. */
    val isCurrentWeek: StateFlow<Boolean> = _weekStart
        .map { it == currentWeekStart }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val goals: StateFlow<List<WeeklyGoal>> = _weekStart
        .flatMapLatest { week -> weeklyRepository.observeGoals(week) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<WeeklyNote>> = _weekStart
        .flatMapLatest { week -> weeklyRepository.observeNotes(week) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many unfinished goals are sitting in the previous week. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousWeekOpenGoals: StateFlow<Int> = _weekStart
        .map { week -> week.minusWeeks(1) }
        .flatMapLatest { week -> weeklyRepository.observeGoals(week) }
        .map { rows -> rows.count { !it.isDone } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun goToPreviousWeek() {
        _weekStart.value = startOfWeek(_weekStart.value.minusWeeks(1))
    }

    fun goToNextWeek() {
        val next = startOfWeek(_weekStart.value.plusWeeks(1))
        if (next <= currentWeekStart) _weekStart.value = next
    }

    fun addGoal(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            weeklyRepository.addGoal(_weekStart.value, trimmed)
        }
    }

    fun toggleGoal(goal: WeeklyGoal) {
        viewModelScope.launch {
            weeklyRepository.setGoalDone(goal.id, !goal.isDone)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch { weeklyRepository.deleteGoal(goalId) }
    }

    fun addNote(body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            weeklyRepository.addNote(_weekStart.value, trimmed)
        }
    }

    fun updateNote(noteId: Long, body: String) {
        viewModelScope.launch {
            weeklyRepository.updateNote(_weekStart.value, noteId, body.trim())
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { weeklyRepository.deleteNote(noteId) }
    }

    /** Copy every unfinished goal from [from] into the currently visible week. */
    fun carryOverOpenGoals(from: LocalDate = _weekStart.value.minusWeeks(1)) {
        viewModelScope.launch {
            weeklyRepository.observeGoals(from).first().forEach { goal ->
                if (!goal.isDone) weeklyRepository.addGoal(_weekStart.value, goal.text)
            }
        }
    }

    private fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
