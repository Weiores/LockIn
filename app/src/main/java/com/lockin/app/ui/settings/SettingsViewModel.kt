package com.lockin.app.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.TriggerRepository
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import com.lockin.app.hardware.wifi.WifiZoneDetector
import com.lockin.app.hardware.wifi.WifiZoneInfo
import com.lockin.app.service.DistractionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val triggerRepository: TriggerRepository,
    private val distractionGuard: DistractionGuard,
    private val wifiZoneDetector: WifiZoneDetector,
    private val hapticHelper: HapticFeedbackHelper,
) : ViewModel() {

    val defaultDurationMinutes: StateFlow<Int> = preferencesRepository.defaultDurationMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val defaultBreakMinutes: StateFlow<Int> = preferencesRepository.defaultBreakMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val vibrationEnabled: StateFlow<Boolean> = preferencesRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val accentScheme: StateFlow<AccentScheme> = preferencesRepository.accentScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentScheme.BLUE)

    val blacklistedPackages: StateFlow<Set<String>> = preferencesRepository.blacklistedPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val currentWifi: StateFlow<WifiZoneInfo> = wifiZoneDetector.observeCurrentWifi()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WifiZoneInfo(isConnected = false))

    val savedWifiZones: StateFlow<List<TriggerProfile>> = triggerRepository.observeByType(TriggerType.WIFI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasUsageStatsPermission: Boolean
        get() = distractionGuard.hasUsageStatsPermission()

    fun getUsageAccessIntent(): Intent = distractionGuard.getUsageAccessSettingsIntent()

    fun toggleBlacklistPackage(packageName: String) {
        viewModelScope.launch {
            val current = blacklistedPackages.value.toMutableSet()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferencesRepository.setBlacklistedPackages(current)
            hapticHelper.performLockSuccessHaptic()
        }
    }

    fun addCustomBlacklistPackage(packageName: String) {
        if (packageName.isBlank()) return
        viewModelScope.launch {
            val current = blacklistedPackages.value.toMutableSet()
            current.add(packageName.trim().lowercase())
            preferencesRepository.setBlacklistedPackages(current)
            hapticHelper.performLockSuccessHaptic()
        }
    }

    fun saveCurrentWifiAsStudyZone(label: String) {
        val wifi = currentWifi.value
        val value = wifi.bssid ?: wifi.ssid ?: return
        viewModelScope.launch {
            triggerRepository.insert(
                TriggerProfile(
                    type = TriggerType.WIFI,
                    label = label.ifBlank { wifi.ssid ?: "Campus Wi-Fi" },
                    value = value,
                    autoPrompt = true,
                ),
            )
            hapticHelper.performLockSuccessHaptic()
        }
    }

    fun deleteWifiZone(zone: TriggerProfile) {
        viewModelScope.launch {
            triggerRepository.delete(zone)
        }
    }

    fun toggleWifiAutoPrompt(zone: TriggerProfile, enabled: Boolean) {
        viewModelScope.launch {
            triggerRepository.update(zone.copy(autoPrompt = enabled))
        }
    }

    fun setDefaultDuration(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultDuration(minutes)
        }
    }

    fun setDefaultBreak(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultBreak(minutes)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVibrationEnabled(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            hapticHelper.performLockSuccessHaptic()
        }
    }

    fun setAccentScheme(scheme: AccentScheme) {
        viewModelScope.launch {
            preferencesRepository.setAccentScheme(scheme)
            hapticHelper.performLockSuccessHaptic()
        }
    }
}

