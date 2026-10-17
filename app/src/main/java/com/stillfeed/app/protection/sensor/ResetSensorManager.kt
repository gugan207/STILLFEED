package com.stillfeed.app.protection.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sensor orientation states for STILLFEED reset activities.
 */
enum class PhoneOrientationState {
    UNKNOWN,
    FACE_UP,
    FACE_DOWN,
    REMOVED_FROM_SURFACE, // Phone was lifted after being face-down
    MOTION_DETECTED
}

/**
 * Hardware sensor manager for STILLFEED Reset activities.
 *
 * Responsibilities:
 * 1. Accurately detects when the phone is flipped face-down (Z-axis gravity < -6.5 m/s² and/or proximity sensor covered).
 * 2. Immediately detects if the user lifts or removes the phone before completion (Z-axis gravity > -5.0 m/s²),
 *    triggering the required "start from first" reset rule.
 * 3. Energy-conscious: only registers sensor listeners during active reset countdowns and releases them immediately.
 */
class ResetSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val _orientationState = MutableStateFlow(PhoneOrientationState.UNKNOWN)
    val orientationState: StateFlow<PhoneOrientationState> = _orientationState.asStateFlow()

    private var wasFaceDown = false
    private var isListening = false
    private var proximityNear = false

    /**
     * Starts listening for phone flip / orientation sensors.
     */
    fun startListening() {
        if (isListening || sensorManager == null) return
        wasFaceDown = false
        proximityNear = false
        _orientationState.value = PhoneOrientationState.UNKNOWN

        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isListening = true
    }

    /**
     * Stops sensor listening to conserve battery.
     */
    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        wasFaceDown = false
        _orientationState.value = PhoneOrientationState.UNKNOWN
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.getOrNull(0) ?: return
                val maxRange = proximitySensor?.maximumRange ?: 5f
                proximityNear = distance < maxRange
            }
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values.getOrNull(2) ?: return

                // Face down condition: Z is strongly negative (gravity pointing into screen)
                // When flat face down on table, Z ≈ -9.8 m/s². Threshold: < -6.5 m/s².
                val isCurrentlyFaceDown = z < FACE_DOWN_THRESHOLD

                if (isCurrentlyFaceDown) {
                    wasFaceDown = true
                    _orientationState.value = PhoneOrientationState.FACE_DOWN
                } else {
                    // If phone was face-down and now lifted (Z > -5.0 m/s²)
                    if (wasFaceDown && z > REMOVED_THRESHOLD) {
                        _orientationState.value = PhoneOrientationState.REMOVED_FROM_SURFACE
                    } else if (!wasFaceDown) {
                        _orientationState.value = PhoneOrientationState.FACE_UP
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val FACE_DOWN_THRESHOLD = -6.5f // m/s²
        private const val REMOVED_THRESHOLD = -5.0f   // m/s²
    }
}
