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
import com.example.smombie.analysis.mic.MicLifecycleAnalyzer
import com.example.smombie.ui.AlertPreviewView
import com.example.smombie.ui.AlertTextView

class AnalyzerController(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private val state: MutableLiveData<State> = MutableLiveData(State.SAFE)

    private var analyzer: MutableList<LifecycleAnalyzer> =
        emptyList<LifecycleAnalyzer>().toMutableList()
    private val cameraAnalyzer: CameraLifecycleAnalyzer by lazy {
        CameraLifecycleAnalyzer(
            context,
            state
        )
    }
    private val gyroAnalyzer: GyroLifecycleAnalyzer by lazy {
        GyroLifecycleAnalyzer(
            context,
            state
        )
    }
    private val micAnalyzer: MicLifecycleAnalyzer by lazy { MicLifecycleAnalyzer(context, state) }

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
        if (analyzer.isEmpty()) analyzer.add(cameraAnalyzer)
        analyzer.forEach { _ -> start() }
    }

    fun stop() {
        analyzer.forEach { _ -> stop() }
    }

    private fun changeAnalyzer(state: State) {
        analyzer.forEach { _ -> stop() }
        analyzer.clear()

        when (state) {
            State.SAFE -> analyzer.add(cameraAnalyzer)
            State.WARNING -> {
                analyzer.add(gyroAnalyzer)
                analyzer.add(micAnalyzer)
            }
            State.HAZARD -> cameraAnalyzer
        }

        analyzer.forEach { _ -> start() }
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