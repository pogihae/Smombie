package com.example.smombie.ui

import android.content.Context
import android.util.Log
import androidx.camera.core.Preview


class Alerter(context: Context) {
    private val textView = AlertTextView(context)
    private val previewView = AlertPreviewView(context)
    private var currentView: OverlayView = previewView

    private var isSafe = true

    fun show() {
        currentView.show()
    }

    fun hide() {
        currentView.hide()
    }

    fun updateState(isSafe: Boolean) {
        if (this.isSafe == isSafe) return

        Log.d(TAG, "$isSafe")

        if (isSafe) {
            previewView.hide()
            textView.show()
            currentView = textView
            previewView.stopBlink()
        } else {
            textView.hide()
            previewView.show()
            previewView.startBlink()
            currentView = previewView
        }

        this.isSafe = isSafe
    }

    companion object {
        private const val TAG = "Alerter"
    }
}