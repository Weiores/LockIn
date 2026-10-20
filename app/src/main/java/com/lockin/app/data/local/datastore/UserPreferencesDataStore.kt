package com.lockin.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lockin_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DEFAULT_DURATION_MINUTES = intPreferencesKey("default_duration_minutes")
        val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val ENABLE_WIFI_AUTO_PROMPT = booleanPreferencesKey("enable_wifi_auto_prompt")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val BLACKLISTED_PACKAGES = stringSetPreferencesKey("blacklisted_packages")

        // Appearance
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_SCHEME = stringPreferencesKey("accent_scheme")

        // Active timer snapshot for crash recovery
        val ACTIVE_SESSION_ID = longPreferencesKey("active_session_id")
        val ACTIVE_STATE = stringPreferencesKey("active_state")
        val ACTIVE_REMAINING_MS = longPreferencesKey("active_remaining_ms")
        val ACTIVE_TOTAL_MS = longPreferencesKey("active_total_ms")
        val ACTIVE_TRIGGER_TYPE = stringPreferencesKey("active_trigger_type")
        val ACTIVE_TARGET_EPOCH_MS = longPreferencesKey("active_target_epoch_ms")
        val ACTIVE_LAST_TICK_EPOCH_MS = longPreferencesKey("active_last_tick_epoch_ms")
    }

    private val defaultBlacklist = setOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.reddit.frontpage",
        "com.snapchat.android",
    )

    val defaultDurationMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_DURATION_MINUTES] ?: 25
    }

    val defaultBreakMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5
    }

    val wifiAutoPromptEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_WIFI_AUTO_PROMPT] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIBRATION_ENABLED] ?: true
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SOUND_ENABLED] ?: true
    }

    val blacklistedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.BLACKLISTED_PACKAGES] ?: defaultBlacklist
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val accentScheme: Flow<AccentScheme> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.ACCENT_SCHEME]) {
            AccentScheme.GREEN.name -> AccentScheme.GREEN
            AccentScheme.RED.name -> AccentScheme.RED
            else -> AccentScheme.BLUE
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentScheme(scheme: AccentScheme) {
        context.dataStore.edit { it[Keys.ACCENT_SCHEME] = scheme.name }
    }

    suspend fun setDefaultDuration(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_DURATION_MINUTES] = minutes }
    }

    suspend fun setDefaultBreak(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_BREAK_MINUTES] = minutes }
    }

    suspend fun setWifiAutoPromptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_WIFI_AUTO_PROMPT] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setBlacklistedPackages(packages: Set<String>) {
        context.dataStore.edit { it[Keys.BLACKLISTED_PACKAGES] = packages }
    }

    // --- Active Timer Snapshot (crash recovery) ---

    data class ActiveTimerSnapshot(
        val sessionId: Long,
        val state: String,
        val remainingMs: Long,
        val totalMs: Long,
        val triggerType: String,
        val targetEpochMs: Long,
        val lastTickEpochMs: Long,
    )

    val activeTimerSnapshot: Flow<ActiveTimerSnapshot?> = context.dataStore.data.map { prefs ->
        val sessionId = prefs[Keys.ACTIVE_SESSION_ID] ?: return@map null
        ActiveTimerSnapshot(
            sessionId = sessionId,
            state = prefs[Keys.ACTIVE_STATE] ?: return@map null,
            remainingMs = prefs[Keys.ACTIVE_REMAINING_MS] ?: return@map null,
            totalMs = prefs[Keys.ACTIVE_TOTAL_MS] ?: return@map null,
            triggerType = prefs[Keys.ACTIVE_TRIGGER_TYPE] ?: return@map null,
            targetEpochMs = prefs[Keys.ACTIVE_TARGET_EPOCH_MS] ?: return@map null,
            lastTickEpochMs = prefs[Keys.ACTIVE_LAST_TICK_EPOCH_MS] ?: return@map null,
        )
    }

    suspend fun saveTimerSnapshot(snapshot: ActiveTimerSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SESSION_ID] = snapshot.sessionId
            prefs[Keys.ACTIVE_STATE] = snapshot.state
            prefs[Keys.ACTIVE_REMAINING_MS] = snapshot.remainingMs
            prefs[Keys.ACTIVE_TOTAL_MS] = snapshot.totalMs
            prefs[Keys.ACTIVE_TRIGGER_TYPE] = snapshot.triggerType
            prefs[Keys.ACTIVE_TARGET_EPOCH_MS] = snapshot.targetEpochMs
            prefs[Keys.ACTIVE_LAST_TICK_EPOCH_MS] = snapshot.lastTickEpochMs
        }
    }

    suspend fun clearTimerSnapshot() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_SESSION_ID)
            prefs.remove(Keys.ACTIVE_STATE)
            prefs.remove(Keys.ACTIVE_REMAINING_MS)
            prefs.remove(Keys.ACTIVE_TOTAL_MS)
            prefs.remove(Keys.ACTIVE_TRIGGER_TYPE)
            prefs.remove(Keys.ACTIVE_TARGET_EPOCH_MS)
            prefs.remove(Keys.ACTIVE_LAST_TICK_EPOCH_MS)
        }
    }
}
