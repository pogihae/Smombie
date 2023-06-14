package com.example.smombie.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.LifecycleOwner
import com.example.smombie.R

@SuppressLint("ViewConstructor")
class AlertTextView(context: Context, lifecycle: LifecycleOwner) : OverlayView(context, lifecycle) {
    private val textView: TextView
    private val mBackground: Drawable

    init {
        textView = TextView(context)
        mBackground = AppCompatResources.getDrawable(context, R.drawable.rounded_corner)!!
        addView(textView)
        setColorAndText(Color.YELLOW, "SAFE")
    }

    fun setColorAndText(@ColorInt colorInt: Int, newText: String) {
        val wrapped = DrawableCompat.wrap(mBackground)
        DrawableCompat.setTint(wrapped, colorInt)
        background = wrapped
        textView.text = newText
    }
}