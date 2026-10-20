package com.lockin.app.domain.repository

import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val defaultDurationMinutes: Flow<Int>
    val defaultBreakMinutes: Flow<Int>
    val wifiAutoPromptEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>
    val soundEnabled: Flow<Boolean>
    val blacklistedPackages: Flow<Set<String>>
    val themeMode: Flow<ThemeMode>
    val accentScheme: Flow<AccentScheme>
    val activeTimerSnapshot: Flow<ActiveTimerSnapshot?>

    suspend fun setDefaultDuration(minutes: Int)
    suspend fun setDefaultBreak(minutes: Int)
    suspend fun setWifiAutoPromptEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setBlacklistedPackages(packages: Set<String>)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentScheme(scheme: AccentScheme)
    suspend fun saveTimerSnapshot(snapshot: ActiveTimerSnapshot)
    suspend fun clearTimerSnapshot()
}
