package com.example.smombie

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smombie.analysis.AnalysisService
import com.permissionx.guolindev.PermissionX

// permission activity
class MainActivity : AppCompatActivity() {
    private var analysisService: AnalysisService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AnalysisService.LocalBinder
            analysisService = binder.getService()
            analysisService?.isRunning?.observe(this@MainActivity) {
                startButton.isEnabled = it.not()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            analysisService = null
        }
    }

    private lateinit var startButton: Button
    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this).not()) {
            finishWithReason("Overlay permission required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        startButton = findViewById(R.id.start_button)
        startButton.setOnClickListener { bindAnalysisService() }
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisService?.stopService()
        unbindService(serviceConnection)
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Runtime permissions
        PermissionX.init(this)
            .permissions(perms)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Core fundamental are based on these permissions",
                    "OK", "Cancel")
            }
            .request { allGranted, _, _ ->
                Log.d(TAG, "Finish permission request")
                if (allGranted.not()) {
                    finishWithReason("Core permissions are denied")
                }

                // Overlay permission
                if (Settings.canDrawOverlays(this).not()) {
                    Log.d(TAG, "Request overlay permission")
                    AlertDialog.Builder(this)
                        .setTitle("Request Overlay permission")
                        .setMessage("Require overlays permission")
                        .setNegativeButton("CANCEL") { _, _ ->
                            finishWithReason("Overlay permission required")
                        }
                        .setPositiveButton("OK") { _, _ ->
                            requestOverlayPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                        .create()
                        .show()
                }
            }
    }

    private fun finishWithReason(reason: String) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG)
            .show()
        finish()
    }

    private fun bindAnalysisService() {
        Intent(this, AnalysisService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    companion object {
        private const val TAG = "MainActivity"

    }
}