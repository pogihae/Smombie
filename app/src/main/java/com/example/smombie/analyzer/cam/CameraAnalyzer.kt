package com.example.smombie.analyzer.cam

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.smombie.R
import com.example.smombie.analyzer.Analyzer
import com.example.smombie.util.IMAGE_SIZE_X
import com.example.smombie.util.IMAGE_SIZE_Y
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraAnalyzer(
    private val context: Context,
    private val onStateChanged: (Analyzer, Analyzer.State) -> Unit
) : Analyzer, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetResolution(Size(IMAGE_SIZE_X, IMAGE_SIZE_Y))
        .build()

    init {
        // Start camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            setAnalyzer()
        }, ContextCompat.getMainExecutor(context))

        // Register lifecycle
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun startAnalyze() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun stopAnalyze() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun setAnalyzer() {
        CoroutineScope(Dispatchers.Main).launch {
            imageAnalysis.clearAnalyzer()
            imageAnalysis.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                ORTAnalyzer(createOrtSession(), ::onAnalyzeFinish)
            )
        }
    }

    private suspend fun createOrtSession(): OrtSession? {
        return withContext(Dispatchers.IO) {
            try {
                val model = context.resources.openRawResource(R.raw.efficient_net_final).readBytes()
                OrtEnvironment.getEnvironment().createSession(model)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun onAnalyzeFinish(detected: String, score: Float) {
        if (score < SCORE_THRESHOLD) return

        if (detected != SIDEWALK && detected != SIDEWALK_ENTRY) {
            onStateChanged(this, Analyzer.State.HAZARD)
        } else if (detected == SIDEWALK_ENTRY) {
            onStateChanged(this, Analyzer.State.WARNING)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    companion object {
        private const val TAG = "CameraAnalyzer"

        private const val SIDEWALK = "SIDEWALK"
        private const val SIDEWALK_ENTRY = "ENTRY"

        private const val SCORE_THRESHOLD = 0.95f
    }
}