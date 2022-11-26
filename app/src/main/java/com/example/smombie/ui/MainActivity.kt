package com.example.smombie.ui

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smombie.analysis.AnalysisService
import com.example.smombie.R

class MainActivity : AppCompatActivity() {
    private var analysisService: AnalysisService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AnalysisService.LocalBinder
            analysisService = binder.getService()
        }
        override fun onServiceDisconnected(className: ComponentName) {
            analysisService = null
        }
    }

    private lateinit var serviceNotification: Notification
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        // 권한
        if (!Settings.canDrawOverlays(this)) {
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Settings.canDrawOverlays(this).not()) {
                    finish()
                }
            }.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, RC_PERMISSIONS
            )
        }

        startButton = findViewById(R.id.start_button)
        startButton.setOnClickListener { bindAnalysisService() }
    }

    override fun onStart() {
        super.onStart()
        analysisService?.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    override fun onStop() {
        super.onStop()
        if (this::serviceNotification.isInitialized.not()) {
            serviceNotification = createForegroundNotification()
        }
        analysisService?.startForeground(1, serviceNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //bindAnalysisService()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun bindAnalysisService() {
        Intent(this, AnalysisService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        val stopIntent =
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_STOP
            }

        // TODO 종료 Intent 수정
        val stopPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopAction: Notification.Action = Notification.Action
            .Builder(null, "stop", stopPendingIntent).build()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("${getString(R.string.app_name)} is working")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(stopAction)
            .setOngoing(false)
            .build()

        return notification
    }

    companion object {
        const val ACTION_STOP = "Analyzing.stop"
        const val CHANNEL_ID = "SMOMBIE_NOTIFICATION_CHANNEL"

        private const val RC_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT >= 33) {
                    add(android.Manifest.permission.POST_NOTIFICATIONS) }
            }.toTypedArray()
    }
}