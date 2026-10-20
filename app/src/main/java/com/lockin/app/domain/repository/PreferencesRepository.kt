package com.lockin.app.domain.repository

import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val defaultDurationMinutes: Flow<Int>
    val defaultBreakMinutes: Flow<Int>
    val flipTriggerEnabled: Flow<Boolean>
    val wifiAutoPromptEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>
    val soundEnabled: Flow<Boolean>
    val graceCountdownSeconds: Flow<Int>
    val blacklistedPackages: Flow<Set<String>>
    val darkMode: Flow<Boolean?>
    val activeTimerSnapshot: Flow<ActiveTimerSnapshot?>

    suspend fun setDefaultDuration(minutes: Int)
    suspend fun setDefaultBreak(minutes: Int)
    suspend fun setFlipTriggerEnabled(enabled: Boolean)
    suspend fun setWifiAutoPromptEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setGraceCountdownSeconds(seconds: Int)
    suspend fun setBlacklistedPackages(packages: Set<String>)
    suspend fun setDarkMode(isDark: Boolean?)
    suspend fun saveTimerSnapshot(snapshot: ActiveTimerSnapshot)
    suspend fun clearTimerSnapshot()
}

