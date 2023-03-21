package com.example.smombie.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.R

@SuppressLint("ViewConstructor")
class AlertPreviewView(context: Context, lifecycle: Lifecycle) : OverlayView(context, lifecycle) {
    private val preview: Preview

    private val previewView: PreviewView
    private val blinkView: View

    private val blinkHandler = android.os.Handler(Looper.getMainLooper())
    private val blinkRunnable: Runnable

    init {
        inflate(context, R.layout.alert_preview, this)

        previewView = findViewById(R.id.preview)
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_START

        blinkView = findViewById(R.id.color_overlay)

        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

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
        super.show()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        startBlink()
    }

    override fun hide() {
        super.hide()
        preview.setSurfaceProvider(null)
        stopBlink()
    }

    private fun startBlink() {
        stopBlink()
        blinkHandler.post(blinkRunnable)
    }

    private fun stopBlink() {
        blinkView.visibility = View.GONE
        blinkHandler.removeCallbacksAndMessages(null)
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }
}