package com.example.smombie.analysis.mic

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.example.smombie.State
import com.example.smombie.analysis.LifecycleAnalyzer

class MicLifecycleAnalyzer(
    private val context: Context, private val state: MutableLiveData<State>
) : LifecycleAnalyzer(context as LifecycleOwner) {

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private val maxAmplitudeUpdateInterval = 1000L // 데시벨 업데이트 간격 (밀리초)
    private val handler = Handler(Looper.getMainLooper())

    private var mEMA = 0.0

    override fun onStart() {
        startRecording()
    }

    override fun onStop() {
        stopRecording()
    }

    private fun startRecording() {
        mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setOutputFile("${context.externalCacheDir!!.absolutePath}/test.3gp")
        mediaRecorder.prepare()
        mediaRecorder.start()

        isRecording = true
        handler.postDelayed(updateDecibelRunnable, maxAmplitudeUpdateInterval)
    }

    private fun stopRecording() {
        isRecording = false
        mediaRecorder.stop()
        mediaRecorder.release()
    }

    private val updateDecibelRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val maxAmplitude = mediaRecorder.maxAmplitude
                val decibel = convertdDb(maxAmplitude.toDouble())

                if (decibel > DECIBEL_THRESHOLD) {
                    state.value = State.HAZARD
                }

                handler.postDelayed(this, maxAmplitudeUpdateInterval)
            }
        }
    }

    fun convertdDb(amplitude: Double): Double {
        // Cellphones can catch up to 90 db + -
        // getMaxAmplitude returns a value between 0-32767 (in most phones). that means that if the maximum db is 90, the pressure
        // at the microphone is 0.6325 Pascal.
        // it does a comparison with the previous value of getMaxAmplitude.
        // we need to divide maxAmplitude with (32767/0.6325)
        //51805.5336 or if 100db so 46676.6381
        val EMA_FILTER = 0.6
        val sp = context.getSharedPreferences("device-base", AppCompatActivity.MODE_PRIVATE)
        sp.getFloat("amplitude", 0f).toDouble()
        val mEMAValue: Double = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA
        //Assuming that the minimum reference pressure is 0.000085 Pascal (on most phones) is equal to 0 db
        // samsung S9 0.000028251
        return (20 * Math.log10(mEMAValue / 51805.5336 / 0.000028251).toFloat()).toDouble()
    }

    companion object {
        private const val DECIBEL_THRESHOLD = 80.0f
    }
}