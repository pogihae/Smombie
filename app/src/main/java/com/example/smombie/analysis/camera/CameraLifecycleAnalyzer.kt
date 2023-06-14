package com.example.smombie.analysis.camera

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.R
import com.example.smombie.analysis.LifecycleAnalyzer
import com.example.smombie.ui.AlertPreviewView
import com.example.smombie.ui.AlertTextView
import com.example.smombie.ui.OverlayView
import com.example.smombie.util.IMAGE_SIZE_X
import com.example.smombie.util.IMAGE_SIZE_Y
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraLifecycleAnalyzer(
    private val context: Context
) : LifecycleAnalyzer(context as LifecycleOwner) {

    private val alertPreviewView = AlertPreviewView(context, this as LifecycleOwner)
    private val alertTextView = AlertTextView(context, this as LifecycleOwner)

    private var currentView: OverlayView = alertPreviewView
    private var currentState: Boolean = false

    private val imageAnalysis =
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(IMAGE_SIZE_X, IMAGE_SIZE_Y)).build()

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        startCamera()
    }

    override fun start() {
        super.start()
        currentView.show()
    }

    override fun stop() {
        super.stop()
        currentView.hide()
    }

    private fun startCamera() {
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
    }

    private fun setAnalyzer() {
        CoroutineScope(Dispatchers.Main).launch {
            imageAnalysis.clearAnalyzer()
            imageAnalysis.setAnalyzer(
                Executors.newSingleThreadExecutor(), ORTAnalyzer(createOrtSession(), ::updateUI)
            )
        }
    }

    private suspend fun createOrtSession(): OrtSession? {
        return withContext(Dispatchers.IO) {
            try {
                val model = context.resources.openRawResource(R.raw.test_door).readBytes()
                OrtEnvironment.getEnvironment().createSession(model)
            } catch (e: Exception) {
                null
            }
        }
    }

    // 추론 결과 횟수 저장
    private val countMap: MutableMap<Boolean, Int> = mutableMapOf(
        true to 0,
        false to 0
    )

    private val REQUIRED_COUNT = 10

    private fun updateUI(result: AnalysisResult) {
        uiHandler.post {
            //Log.d(TAG, result.toString())
            if (result.isSafe == currentState) return@post

            countMap[result.isSafe] = (countMap[result.isSafe] ?: 0) + 1
            if (countMap[result.isSafe]!! < REQUIRED_COUNT) return@post

            if (result.isSafe) {
                currentView.hide()
                alertTextView.setColorAndText(Color.YELLOW, result.detectedLabel)
                currentView = alertTextView
                currentView.show()
            } else {
                currentView.hide()
                currentView = alertPreviewView
                currentView.show()
            }

            countMap[result.isSafe] = 0
            currentState = result.isSafe
        }
    }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}