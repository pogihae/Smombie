package com.example.smombie.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import com.example.smombie.R


class Alerter(private val mContext: Context) : FrameLayout(mContext) {
    private val imageView: ImageView = ImageView(context)
    private val textView: TextView = TextView(context)

    private val enterAnimation = AnimationUtils.loadAnimation(
        context,
        R.anim.alerter_slide_in_from_top
    )
    private val exitAnimation = AnimationUtils.loadAnimation(
        context,
        R.anim.alerter_slide_out_to_top
    )

    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)

    private var isShowing: Boolean = false
    private var startTime: Long = 0L

    init {
        addOverlayView(
            this, ALERT_WIDTH, ALERT_HEIGHT, Gravity.TOP
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        textView.apply {
            textSize = 30.0f
            setTextColor(Color.RED)
            text = ""
            gravity = Gravity.CENTER
        }

        background = AppCompatResources.getDrawable(context, R.drawable.view_border)
        visibility = View.INVISIBLE
        this.addView(imageView)
        this.addView(textView)

        startTime = 0
    }

    fun show(image: Bitmap?, label: String) {
        if (image == null) {
            return
        }
        visibility = View.VISIBLE

        imageView.setImageBitmap(image)
        textView.text = label

        startTime += 1L
        if (!isShowing) {
            imageView.startAnimation(enterAnimation)
            isShowing = true
        }
        Log.d(TAG, "New Alert: $startTime")
        Log.d(TAG, startTime.toString())
    }

    fun hide() {
        if (isShowing.not()) return
        isShowing = false
        Log.d(TAG, "View expired duration: ${System.currentTimeMillis() - startTime}")
        enterAnimation.setAnimationListener(null)
        imageView.startAnimation(exitAnimation)
        visibility = View.INVISIBLE
    }

    private fun addOverlayView(
        view: View,
        width: Int,
        height: Int,
        gravity: Int,
        xPos: Int = 0,
        yPos: Int = 0,
        format: Int = PixelFormat.TRANSLUCENT
    ) {
        val layoutParams = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            format
        ).apply {
            this.gravity =
                GravityCompat.getAbsoluteGravity(gravity, ViewCompat.LAYOUT_DIRECTION_LTR)
            this.x = xPos
            this.y = yPos
        }

        windowManager.addView(view, layoutParams)
    }

    companion object {
        private const val TAG = "Alerter"
        private const val ALERT_WIDTH = WindowManager.LayoutParams.MATCH_PARENT
        private const val ALERT_HEIGHT = 600
        private const val DURATION = 1000L
    }
}