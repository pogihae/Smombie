package com.example.smombie.analysis

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.example.smombie.ui.MainActivity

//todo fix 처음 시작 시 분석 안함
class AnalysisService : LifecycleService() {
    private val binder = LocalBinder()

    private lateinit var cameraAnalyzer: CameraAnalyzer

    var isRunning = false

    //onBind when??
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        //why context can only init in here?
        cameraAnalyzer = CameraAnalyzer(this, lifecycle)
        cameraAnalyzer.startAnalyze()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, intent?.action.toString())
        if (intent?.action == MainActivity.ACTION_STOP) {
            isRunning = false
            cameraAnalyzer.stopAnalyze()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    inner class LocalBinder : Binder() {
        fun getService(): AnalysisService = this@AnalysisService
    }

    companion object {
        private const val TAG = "AnalysisService"
    }
}