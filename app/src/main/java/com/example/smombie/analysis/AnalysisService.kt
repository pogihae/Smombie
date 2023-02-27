package com.example.smombie.analysis

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.smombie.R
import com.example.smombie.ui.Alerter
import com.example.smombie.ui.MainActivity
import com.example.smombie.util.IMAGE_SIZE_X
import com.example.smombie.util.IMAGE_SIZE_Y
import kotlinx.coroutines.*
import java.util.concurrent.Executors

//todo fix 처음 시작 시 분석 안함
class AnalysisService : LifecycleService() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var cameraController: LifecycleCameraController
    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetResolution(Size(IMAGE_SIZE_X, IMAGE_SIZE_Y))
        .build()
    private val preview = Preview.Builder().build()

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())

    private lateinit var alerter: Alerter

    var isRunning = false

    //onBind when??
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        //why context can only init in here?
        cameraController = LifecycleCameraController(this)
        startCamera()
        alerter = Alerter(this, preview)
        alerter.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, intent?.action.toString())
        if (intent?.action == MainActivity.ACTION_STOP) {
            isRunning = false
            alerter.hide()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startCamera() {
        cameraController.bindToLifecycle(this)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, preview
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
                analysisExecutor, ORTAnalyzer(createOrtSession(), ::updateUI)
            )
        }
    }

    private suspend fun createOrtSession(): OrtSession? {
        return withContext(Dispatchers.IO) {
            try {
                val model = resources.openRawResource(R.raw.test_door).readBytes()
                ortEnv.createSession(model)
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnalysisService,
                    "ERROR ON READ MODEL", Toast.LENGTH_LONG
                ).show()
                stopSelf()
                null
            }
        }
    }

    private fun updateUI(result: AnalysisResult) {
        if (result.detectedScore < THRESH_HOLD) return

        val isSafe = result.detectedLabel in ORTAnalyzer.SAFE_LABELS

        uiHandler.post {
            alerter.updateState(isSafe)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        alerter.hide()
    }

    inner class LocalBinder : Binder() {
        fun getService(): AnalysisService = this@AnalysisService
    }

    companion object {
        private const val TAG = "AnalysisService"
        private const val THRESH_HOLD = 0.55

    }
}