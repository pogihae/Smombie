package com.example.smombie.ui

import android.content.Context
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat

open class AlertView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val mWindowManager: WindowManager
    private val mParams: WindowManager.LayoutParams

    private var isSafe = true

    init {
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = GravityCompat.getAbsoluteGravity(Gravity.TOP, ViewCompat.LAYOUT_DIRECTION_LTR)
            windowAnimations = android.R.style.Animation_Translucent
        }
    }

    open fun show() {
        mWindowManager.addView(this, mParams)
    }

    open fun hide() {
        mWindowManager.removeView(this)
    }

    open fun safe() {
        if (isSafe) return
        isSafe = true
    }

    open fun hazard() {
        if (isSafe.not()) return
        isSafe = false
    }
}