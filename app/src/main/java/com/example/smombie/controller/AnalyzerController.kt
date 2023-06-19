package com.example.smombie.controller

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.example.smombie.State
import com.example.smombie.analysis.LifecycleAnalyzer
import com.example.smombie.analysis.camera.CameraLifecycleAnalyzer
import com.example.smombie.analysis.gyro.GyroLifecycleAnalyzer
import com.example.smombie.ui.AlertPreviewView
import com.example.smombie.ui.AlertTextView

class AnalyzerController(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private val state: MutableLiveData<State> = MutableLiveData(State.SAFE)

    private var analyzer: LifecycleAnalyzer? = null
    private val cameraAnalyzer: CameraLifecycleAnalyzer by lazy { CameraLifecycleAnalyzer(context, state) }
    private val gyroAnalyzer: GyroLifecycleAnalyzer by lazy { GyroLifecycleAnalyzer(context, state) }

    private val alertPreviewView = AlertPreviewView(context, lifecycleOwner)
    private val alertTextView = AlertTextView(context, lifecycleOwner)
    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        state.observe(lifecycleOwner) {
            if (it != this.state.value) {
                updateUI(it)
                changeAnalyzer(it)
            }
        }
    }

    fun start() {
        if (analyzer == null) analyzer = cameraAnalyzer
        analyzer!!.start()
    }

    fun stop() {
        analyzer?.stop()
    }

    private fun changeAnalyzer(state: State) {
        analyzer?.stop()

        analyzer = when (state) {
            State.SAFE -> cameraAnalyzer
            State.WARNING -> gyroAnalyzer
            State.HAZARD -> cameraAnalyzer
        }

        analyzer!!.start()
    }

    private fun updateUI(state: State) {
        uiHandler.post {
            when (state) {
                State.SAFE -> {
                    alertPreviewView.hide()
                    alertTextView.setColorAndText(Color.GREEN, "SAFE")
                }
                State.WARNING -> {
                    alertPreviewView.hide()
                    alertTextView.setColorAndText(Color.CYAN, "WARNING")
                }
                State.HAZARD -> {
                    alertTextView.hide()
                    alertPreviewView.show()
                }
            }
        }
    }
}