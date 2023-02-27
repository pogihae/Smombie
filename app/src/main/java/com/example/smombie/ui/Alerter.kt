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
        previewView.preview = preview

        textView.setOnClickListener { changeView(previewView) }
        previewView.setOnClickListener { changeView(textView) }
    }

    fun show() {
        currentView.show()
    }

    fun hide() {
        currentView.hide()
    }

    fun updateState(isSafe: Boolean) {
        currentView.setState(isSafe)
    }

    private fun changeView(targetView: AlertView) {
        val prevState = currentView.isSafe

        currentView.hide()
        currentView = targetView
        currentView.setState(prevState)
        currentView.show()
    }

    companion object {
        private const val TAG = "Alerter"
    }
}