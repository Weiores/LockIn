package com.lockin.app.hardware.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedbackHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Crisp single tap when locking into deep work or restoring lock posture.
     */
    fun performLockSuccessHaptic() {
        if (vibrator?.hasVibrator() == true) {
            val effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Urgent pulsating waveform fired when a blacklisted app is opened during a
     * locked session. Repeats until cancelled.
     */
    fun performDistractionWarningHaptic() {
        if (vibrator?.hasVibrator() == true) {
            // Pattern: 0ms delay, 180ms pulse, 120ms pause, 180ms pulse, 500ms pause
            val timings = longArrayOf(0, 180, 120, 180, 500)
            val amplitudes = intArrayOf(0, 220, 0, 220, 0)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // repeat at index 0
            vibrator.vibrate(effect)
        }
    }

    /**
     * Stops any ongoing warning vibration immediately.
     */
    fun cancelHaptics() {
        vibrator?.cancel()
    }
}

