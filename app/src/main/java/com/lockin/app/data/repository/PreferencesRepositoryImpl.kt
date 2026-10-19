package com.lockin.app.data.repository

import com.lockin.app.data.local.datastore.UserPreferencesDataStore
import com.lockin.app.data.local.datastore.UserPreferencesDataStore.ActiveTimerSnapshot
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
    override val flipTriggerEnabled: Flow<Boolean> = dataStore.flipTriggerEnabled
    override val wifiAutoPromptEnabled: Flow<Boolean> = dataStore.wifiAutoPromptEnabled
    override val vibrationEnabled: Flow<Boolean> = dataStore.vibrationEnabled
    override val soundEnabled: Flow<Boolean> = dataStore.soundEnabled
    override val graceCountdownSeconds: Flow<Int> = dataStore.graceCountdownSeconds
    override val blacklistedPackages: Flow<Set<String>> = dataStore.blacklistedPackages
    override val activeTimerSnapshot: Flow<ActiveTimerSnapshot?> = dataStore.activeTimerSnapshot

    override suspend fun setDefaultDuration(minutes: Int) = dataStore.setDefaultDuration(minutes)
    override suspend fun setDefaultBreak(minutes: Int) = dataStore.setDefaultBreak(minutes)
    override suspend fun setFlipTriggerEnabled(enabled: Boolean) = dataStore.setFlipTriggerEnabled(enabled)
    override suspend fun setWifiAutoPromptEnabled(enabled: Boolean) = dataStore.setWifiAutoPromptEnabled(enabled)
    override suspend fun setVibrationEnabled(enabled: Boolean) = dataStore.setVibrationEnabled(enabled)
    override suspend fun setSoundEnabled(enabled: Boolean) = dataStore.setSoundEnabled(enabled)
    override suspend fun setGraceCountdownSeconds(seconds: Int) = dataStore.setGraceCountdownSeconds(seconds)
    override suspend fun setBlacklistedPackages(packages: Set<String>) = dataStore.setBlacklistedPackages(packages)
    override suspend fun saveTimerSnapshot(snapshot: ActiveTimerSnapshot) = dataStore.saveTimerSnapshot(snapshot)
    override suspend fun clearTimerSnapshot() = dataStore.clearTimerSnapshot()
}

