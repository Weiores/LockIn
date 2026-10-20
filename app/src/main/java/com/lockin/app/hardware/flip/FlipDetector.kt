package com.lockin.app.hardware.flip

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.lockin.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlipDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService<SensorManager>()

    sealed interface FlipState {
        data object FaceDown : FlipState
        data object FaceUp : FlipState
        data object Unknown : FlipState
    }

    val isSupported: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    /**
     * Emits the physical orientation of the device using Accelerometer Z-axis and Proximity sensor.
     *
     * Z-axis gravity:
     * - Flat face-up on table: Z ≈ +9.8 m/s²
     * - Flat face-down on table: Z ≈ -9.8 m/s²
     *
     * Combined with proximity sensor (0 cm when screen is against desk).
     * Applies hysteresis thresholds to prevent flutter.
     */
    fun observeFlipState(): Flow<FlipState> = callbackFlow {
        val sm = sensorManager ?: run {
            trySend(FlipState.Unknown)
            close()
            return@callbackFlow
        }

        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (accelerometer == null) {
            trySend(FlipState.Unknown)
            close()
            return@callbackFlow
        }

        var lastZ = 0f
        var lastProximity = Float.MAX_VALUE
        var hasProximity = proximity != null
        var lastEmitted: FlipState = FlipState.Unknown

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastZ = event.values[2]
                    }
                    Sensor.TYPE_PROXIMITY -> {
                        lastProximity = event.values[0]
                    }
                }

                val isNear = !hasProximity || lastProximity < 1.0f
                val isFar = hasProximity && lastProximity >= 1.0f

                val isFaceDown = lastZ < Constants.FLIP_Z_FACE_DOWN_THRESHOLD && isNear
                val isFaceUp = lastZ > Constants.FLIP_Z_FACE_UP_THRESHOLD || isFar

                val newState = when {
                    isFaceDown -> FlipState.FaceDown
                    isFaceUp -> FlipState.FaceUp
                    else -> lastEmitted
                }

                if (newState != lastEmitted && newState != FlipState.Unknown) {
                    lastEmitted = newState
                    trySend(newState)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        proximity?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        awaitClose {
            sm.unregisterListener(listener)
        }
    }.distinctUntilChanged()
}

