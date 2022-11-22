package com.example.smombie.analysis

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.smombie.R
import com.example.smombie.ui.Alerter
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AnalysisService : LifecycleService() {
    private val binder = LocalBinder()
    private val alerter = Alerter()
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture

    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val analysisExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val captureExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        startCamera()
    }

    private fun updateUI(result: AnalysisResult) {
        if (result.detectedScore < THRESH_HOLD) {
            return
        }
        Log.d(TAG, "${result.detectedLabel}, ${result.detectedScore}")
        alerter.show()
    }

    private suspend fun createOrtSession(): OrtSession? {
        return withContext(Dispatchers.IO) {
            val model = resources.openRawResource(R.raw.test).readBytes()
            ortEnv.createSession(model)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            setAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setAnalyzer() {
        scope.launch {
            imageAnalysis.clearAnalyzer()
            imageAnalysis.setAnalyzer(
                analysisExecutor,
                ORTAnalyzer(createOrtSession(), ::updateUI)
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): AnalysisService = this@AnalysisService
    }

    companion object {
        private const val TAG = "AnalysisService"
        private const val THRESH_HOLD = 0.8
    }
}