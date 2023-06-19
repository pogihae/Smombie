package com.example.smombie.analysis.gyro

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.analysis.LifecycleAnalyzer
import com.example.smombie.ui.AlertTextView
import kotlin.math.abs

class GyroLifecycleAnalyzer(private val context: Context) :
    LifecycleAnalyzer(context as LifecycleOwner), SensorEventListener {

    private val alertTextView = AlertTextView(context, this as LifecycleOwner)

    private lateinit var sensorManager: SensorManager
    private var rotationVector: Sensor? = null

    private var prevAzimuth = 0f

    init {
        initGyroSensor()
        alertTextView.setColorAndText(Color.CYAN, "EX_WARNING")
    }

    override fun start() {
        super.start()
        alertTextView.show()
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun stop() {
        super.stop()
        alertTextView.hide()
        sensorManager.unregisterListener(this)
    }

    private fun initGyroSensor() {
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == rotationVector) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event!!.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

            detectStepDirection(azimuth)
        }
    }

    private fun detectStepDirection(azimuth: Float) {
        val azimuthChange = abs(azimuth - prevAzimuth)

        if (azimuthChange > 5f) {
            Log.d("GYRO", "STEP CHANGED")
        }

        prevAzimuth = azimuth
    }
}