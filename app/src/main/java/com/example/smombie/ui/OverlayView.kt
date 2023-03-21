package com.example.smombie.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

@SuppressLint("ViewConstructor")
open class OverlayView(
    val mContext: Context, val mLifecycle: Lifecycle
) : FrameLayout(mContext), LifecycleOwner, DefaultLifecycleObserver {

    private val mWindowManager: WindowManager
    private val mParams: WindowManager.LayoutParams
    private val lifecycleRegistry: LifecycleRegistry

    init {
        mLifecycle.addObserver(this)
        lifecycleRegistry = LifecycleRegistry(this)

        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = GravityCompat.getAbsoluteGravity(Gravity.TOP, ViewCompat.LAYOUT_DIRECTION_LTR)
            windowAnimations = android.R.style.Animation_Translucent
        }

        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun show() {
        if (isShown) return
        mWindowManager.addView(this, mParams)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    open fun hide() {
        if (isShown.not()) return
        mWindowManager.removeView(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mWindowManager.removeView(this)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}