package com.example.smombie

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smombie.analysis.AnalysisService
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    private var onBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AnalysisService.LocalBinder
            val analysisService = binder.getService()
            onBound = true
            analysisService.isRunning.observe(this@MainActivity) {
                if (it) {
                    startButton.text = getString(R.string.stop)
                    startButton.setBackgroundColor(Color.RED)
                    startButton.setOnClickListener { stopAnalysis() }
                } else {
                    startButton.text = getString(R.string.start)
                    startButton.setBackgroundColor(Color.GREEN)
                    startButton.setOnClickListener { startAnalysis() }
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            onBound = false
        }
    }

    private lateinit var startButton: Button

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this).not()) {
            finishWithReason("Overlay permission rejected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        startButton = findViewById(R.id.start_button)
        startButton.setOnClickListener { startAnalysis() }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AnalysisService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startAnalysis() {
        Intent(this@MainActivity, AnalysisService::class.java).also {
            startService(it)
        }
    }

    private fun stopAnalysis() {
        Intent(this@MainActivity, AnalysisService::class.java).apply {
            action = AnalysisService.ACTION_STOP
        }.also {
            startService(it)
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Runtime permissions
        // If all other permissions are granted, check overlay permission
        // and request it if can not draw overlay
        PermissionX.init(this).permissions(perms).explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList, "Core fundamental are based on these permissions",
                    "OK", "Cancel"
                )
            }.request { allGranted, _, _ ->
                if (allGranted.not()) {
                    finishWithReason("Core permissions are denied")
                }

                // Overlay permission
                if (Settings.canDrawOverlays(this).not()) {
                    AlertDialog.Builder(this).setTitle("Request Overlay permission")
                        .setMessage("To show your state and camera preview, we need draw overlays permission")
                        .setNegativeButton("CANCEL") { _, _ ->
                            finishWithReason("Overlay permission rejected")
                        }.setPositiveButton("OK") { _, _ ->
                            requestOverlayPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }.create().show()
                }
            }
    }

    private fun finishWithReason(reason: String) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onStop() {
        super.onStop()
        if (onBound) {
            unbindService(serviceConnection)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnalysis()
    }

    companion object {
        private const val TAG = "MainActivity"

    }
}