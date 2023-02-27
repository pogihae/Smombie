package com.example.smombie.ui

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.R

class AlertPreviewView(context: Context, attrs: AttributeSet? = null) : AlertView(context, attrs) {
    var preview: Preview? = null

    private val previewView: PreviewView
    private val overlayView: View

    private val mHandler = android.os.Handler(Looper.getMainLooper())
    private val blinkRunnable: Runnable

    init {
        inflate(context, R.layout.alert_preview, this)

        previewView = findViewById(R.id.preview)
        overlayView = findViewById(R.id.color_overlay)

        previewView.scaleType = PreviewView.ScaleType.FILL_START

        blinkRunnable = object : Runnable {
            override fun run() {
                overlayView.visibility = if (overlayView.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                mHandler.postDelayed(this, 500)
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    override fun show() {
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        super.show()
    }

    override fun hide() {
        preview?.setSurfaceProvider(null)
        mHandler.removeCallbacks(blinkRunnable)
        super.hide()
    }

    override fun hazard() {
        mHandler.postDelayed(blinkRunnable, 500)
    }

    override fun safe() {
        overlayView.visibility = View.GONE
        mHandler.removeCallbacks(blinkRunnable)
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview).apply {
            cameraControl.enableTorch(false)
            cameraControl.cancelFocusAndMetering()
        }
    }
}