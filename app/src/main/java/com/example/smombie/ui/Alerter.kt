package com.example.smombie.ui

import android.content.Context
import android.util.Log
import androidx.camera.core.Preview
import com.example.smombie.analysis.AnalysisResult
import com.example.smombie.analysis.ORTAnalyzer


class Alerter(context: Context, preview: Preview) {
    private val textView = AlertTextView(context)
    private val previewView = AlertPreviewView(context)

    private var currentView: AlertView = previewView

    init {
        textView.setOnClickListener {
            hide()
            currentView = previewView
            previewView.show()
        }
        previewView.setOnClickListener {
            hide()
            currentView = textView
            textView.show()
        }
        previewView.preview = preview
    }

    fun show() {
        currentView.show()
    }

    fun hide() {
        currentView.hide()
    }

    //todo change view
    fun update(result: AnalysisResult) {
        Log.d(TAG, "DETECTED: ${result.detectedLabel} with ${result.detectedScore}")

        if (result.detectedScore < THRESH_HOLD) {
            return
        }

        if (result.detectedLabel in ORTAnalyzer.SAFE_LABELS) {
            currentView.safe()
        } else {
            currentView.hazard()
        }
    }

    companion object {
        private const val TAG = "Alerter"
        private const val THRESH_HOLD = 0.55
    }
}