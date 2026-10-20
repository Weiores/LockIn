package com.lockin.app.ui.vault

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.TriggerRepository
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import com.lockin.app.hardware.nfc.NfcCardReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TriggerVaultViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository,
    private val nfcCardReader: NfcCardReader,
    private val hapticFeedbackHelper: HapticFeedbackHelper,
) : ViewModel() {

    val triggers: StateFlow<List<TriggerProfile>> = triggerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isNfcSupported: Boolean = nfcCardReader.isSupported
    val isNfcEnabled: Boolean = nfcCardReader.isEnabled

    private var nfcJob: Job? = null
    private val _scannedCardUid = MutableStateFlow<String?>(null)
    val scannedCardUid: StateFlow<String?> = _scannedCardUid.asStateFlow()

    fun startNfcListening(activity: Activity) {
        _scannedCardUid.value = null
        nfcJob?.cancel()
        nfcJob = viewModelScope.launch {
            nfcCardReader.observeCardUid(activity).collect { uid ->
                _scannedCardUid.value = uid
                hapticFeedbackHelper.performLockSuccessHaptic()
            }
        }
    }

    fun stopNfcListening() {
        nfcJob?.cancel()
        nfcJob = null
        _scannedCardUid.value = null
    }

    /** Confirmation pulse after a successful barcode read. */
    fun playSuccessHaptic() {
        hapticFeedbackHelper.performLockSuccessHaptic()
    }

    fun saveTrigger(
        type: TriggerType,
        label: String,
        value: String,
        isStrict: Boolean = false,
        autoPrompt: Boolean = false,
    ) {
        viewModelScope.launch {
            triggerRepository.insert(
                TriggerProfile(
                    type = type,
                    label = label,
                    value = value,
                    isStrict = isStrict,
                    autoPrompt = autoPrompt,
                ),
            )
        }
    }

    fun deleteTrigger(trigger: TriggerProfile) {
        viewModelScope.launch {
            triggerRepository.delete(trigger)
        }
    }
}
