package com.example.smombie.analyzer.gyro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.smombie.analyzer.Analyzer

class GyroAnalyzer(
    context: Context,
    private val onStateChanged: (Analyzer, Analyzer.State) -> Unit
) : Analyzer, SensorEventListener {

    private val sensorManager: SensorManager
    private val rotationVector: Sensor

    private var prevAzimuth = 0f

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun startAnalyze() {
        sensorManager.registerListener(
            this,
            rotationVector,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    override fun stopAnalyze() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == rotationVector) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

            detectStepDirection(azimuth)
        }
    }

    private fun detectStepDirection(azimuth: Float) {
        val azimuthChange = kotlin.math.abs(azimuth - prevAzimuth)

        // 걸음 방향 변화 감지 시
        if (azimuthChange > 5f) {
            onStateChanged(this, Analyzer.State.HAZARD)
        }

        prevAzimuth = azimuth
    }
}