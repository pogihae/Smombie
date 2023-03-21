package com.example.smombie.analysis

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.smombie.R
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

class CameraAnalyzer(private val context: Context, private val lifecycle: Lifecycle) :
    LifecycleOwner, DefaultLifecycleObserver {

    private val lifecycleRegistry: LifecycleRegistry

    private val alertPreviewView: AlertPreviewView
    private val alertTextView: AlertTextView
    private var currentView: OverlayView

    private var currentState: Boolean = false

    private val imageAnalysis =
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(IMAGE_SIZE_X, IMAGE_SIZE_Y)).build()

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        lifecycle.addObserver(this)
        lifecycleRegistry = LifecycleRegistry(this)

        alertPreviewView = AlertPreviewView(context, lifecycle)
        alertTextView = AlertTextView(context, lifecycle)
        currentView = alertPreviewView

        startCamera()

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun startAnalyze() {
        currentView.show()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stopAnalyze() {
        currentView.hide()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun startCamera() {
        LifecycleCameraController(context).apply {
            enableTorch(false)
            cameraControl?.cancelFocusAndMetering()
        }.bindToLifecycle(this)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener( {
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

    private fun updateUI(result: AnalysisResult) {
        uiHandler.post {
            if (result.isSafe == currentState) return@post

            if (result.isSafe) {
                currentView.hide()
                alertTextView.setColorAndText(Color.GREEN, result.detectedLabel)
                currentView = alertTextView
                currentView.show()
            } else {
                currentView.hide()
                currentView = alertPreviewView
                currentView.show()
            }

            currentState = result.isSafe
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}