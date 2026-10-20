package com.lockin.app.data.repository

import com.lockin.app.data.local.datastore.UserPreferencesDataStore
import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode
import com.lockin.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
) : PreferencesRepository {

    override val defaultDurationMinutes: Flow<Int> = dataStore.defaultDurationMinutes
    override val defaultBreakMinutes: Flow<Int> = dataStore.defaultBreakMinutes
    override val wifiAutoPromptEnabled: Flow<Boolean> = dataStore.wifiAutoPromptEnabled
    override val vibrationEnabled: Flow<Boolean> = dataStore.vibrationEnabled
    override val soundEnabled: Flow<Boolean> = dataStore.soundEnabled
    override val blacklistedPackages: Flow<Set<String>> = dataStore.blacklistedPackages
    override val themeMode: Flow<ThemeMode> = dataStore.themeMode
    override val accentScheme: Flow<AccentScheme> = dataStore.accentScheme
    override val activeTimerSnapshot: Flow<ActiveTimerSnapshot?> = dataStore.activeTimerSnapshot

    override suspend fun setDefaultDuration(minutes: Int) = dataStore.setDefaultDuration(minutes)
    override suspend fun setDefaultBreak(minutes: Int) = dataStore.setDefaultBreak(minutes)
    override suspend fun setWifiAutoPromptEnabled(enabled: Boolean) = dataStore.setWifiAutoPromptEnabled(enabled)
    override suspend fun setVibrationEnabled(enabled: Boolean) = dataStore.setVibrationEnabled(enabled)
    override suspend fun setSoundEnabled(enabled: Boolean) = dataStore.setSoundEnabled(enabled)
    override suspend fun setBlacklistedPackages(packages: Set<String>) = dataStore.setBlacklistedPackages(packages)
    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)
    override suspend fun setAccentScheme(scheme: AccentScheme) = dataStore.setAccentScheme(scheme)
    override suspend fun saveTimerSnapshot(snapshot: ActiveTimerSnapshot) = dataStore.saveTimerSnapshot(snapshot)
    override suspend fun clearTimerSnapshot() = dataStore.clearTimerSnapshot()
}
