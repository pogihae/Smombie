package com.example.smombie.analyzer.mic

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.smombie.analyzer.Analyzer
import java.io.IOException
import kotlin.math.log10

class MicAnalyzer(
    context: Context,
    private val onStateChanged: (Analyzer, Analyzer.State) -> Unit
) : Analyzer {

    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private var mEMA = 0.0
    private val fileName: String = "${context.externalCacheDir!!.absolutePath}/test.3gp"

    private val handler = Handler(Looper.getMainLooper())
    private val updateDecibelRunnable = object : Runnable {
        override fun run() {
            if (isRecording && recorder != null) {
                val decibel = soundDb(getAmplitude())

                if (decibel > DECIBEL_THRESHOLD) {
                    onStateChanged(this@MicAnalyzer, Analyzer.State.HAZARD)
                }

                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
    }

    override fun startAnalyze() {
        startRecording()
        isRecording = true
        handler.postDelayed(updateDecibelRunnable, UPDATE_INTERVAL)
    }

    override fun stopAnalyze() {
        stopRecording()
        isRecording = false
        handler.removeCallbacks(updateDecibelRunnable)
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.let {
            it.stop()
            it.release()
        }
    }

    private fun soundDb(ampl: Double): Double {
        return 20 * log10(getAmplitudeEMA() / ampl)
    }

    private fun getAmplitude(): Double = recorder?.maxAmplitude?.toDouble() ?: 0.0

    private fun getAmplitudeEMA(): Double {
        val amp = getAmplitude()
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA
    }

    companion object {
        private const val TAG = "MicAnalyzer"

        private const val DECIBEL_THRESHOLD = 80.0f
        private const val UPDATE_INTERVAL = 1000L // 데시벨 업데이트 간격 (밀리초)
        private const val EMA_FILTER = 0.6
    }
}