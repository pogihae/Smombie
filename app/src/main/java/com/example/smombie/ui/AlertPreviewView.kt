package com.example.smombie.ui

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.R

class AlertPreviewView(context: Context, attrs: AttributeSet? = null) : OverlayView(context, attrs) {
    private val preview: Preview

    private val previewView: PreviewView
    private val blinkView: View

    private val blinkHandler = android.os.Handler(Looper.getMainLooper())
    private val blinkRunnable: Runnable

    init {
        inflate(context, R.layout.alert_preview, this)

        preview = Preview.Builder().build()
        previewView = findViewById(R.id.preview)
        blinkView = findViewById(R.id.color_overlay)

        previewView.scaleType = PreviewView.ScaleType.FILL_START

        blinkRunnable = object : Runnable {
            override fun run() {
                blinkView.visibility = if (blinkView.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                blinkHandler.postDelayed(this, 500)
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    override fun show() {
        preview.setSurfaceProvider(previewView.surfaceProvider)
        super.show()
    }

    override fun hide() {
        preview.setSurfaceProvider(null)
        super.hide()
    }

    fun startBlink() {
        if (isShown.not()) {
            Log.d("AlertPreview", "NOT SHOWN STATE")
            return
        }
        stopBlink()
        blinkHandler.post(blinkRunnable)
    }

    fun stopBlink() {
        blinkView.visibility = View.GONE
        blinkHandler.removeCallbacksAndMessages(null)
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview).apply {
            cameraControl.enableTorch(false)
            cameraControl.cancelFocusAndMetering()
        }
    }
}