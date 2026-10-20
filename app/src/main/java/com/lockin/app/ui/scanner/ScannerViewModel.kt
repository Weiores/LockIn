package com.lockin.app.ui.scanner

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backing view model for the two full-screen scanner pages. Scoped to each
 * scanner destination's nav entry, so the vault's own state stays untouched
 * while the user is scanning.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val triggerRepository: TriggerRepository,
    private val nfcCardReader: NfcCardReader,
    private val hapticFeedbackHelper: HapticFeedbackHelper,
) : ViewModel() {

    /** Most recent payload read by whichever sensor is active. */
    private val _scannedValue = MutableStateFlow<String?>(null)
    val scannedValue: StateFlow<String?> = _scannedValue.asStateFlow()

    val isNfcSupported: Boolean = nfcCardReader.isSupported
    val isNfcEnabled: Boolean = nfcCardReader.isEnabled

    private var nfcJob: Job? = null

    fun startNfcListening(activity: Activity) {
        _scannedValue.value = null
        nfcJob?.cancel()
        nfcJob = viewModelScope.launch {
            nfcCardReader.observeCardUid(activity).collect { uid ->
                _scannedValue.value = uid
                hapticFeedbackHelper.performLockSuccessHaptic()
            }
        }
    }

    fun stopNfcListening() {
        nfcJob?.cancel()
        nfcJob = null
    }

    fun onBarcodeScanned(value: String) {
        _scannedValue.value = value
        hapticFeedbackHelper.performLockSuccessHaptic()
    }

    /** Entered by hand when the camera is broken or the code is unreadable. */
    fun onManualEntry(value: String) {
        if (value.isNotBlank()) _scannedValue.value = value.trim()
    }

    fun resetScan() {
        nfcJob?.cancel()
        _scannedValue.value = null
    }

    fun saveTrigger(
        type: TriggerType,
        label: String,
        value: String,
        isStrict: Boolean,
    ): Boolean {
        if (value.isBlank()) return false
        viewModelScope.launch {
            triggerRepository.insert(
                TriggerProfile(
                    type = type,
                    label = label.ifBlank { defaultLabel(type) },
                    value = value,
                    isStrict = isStrict,
                ),
            )
        }
        return true
    }

    private fun defaultLabel(type: TriggerType): String = when (type) {
        TriggerType.BARCODE -> "Desk Barcode"
        TriggerType.NFC -> "NFC Card"
        else -> "Physical Key"
    }

    override fun onCleared() {
        nfcJob?.cancel()
        super.onCleared()
    }
}
