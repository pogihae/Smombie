package com.example.smombie.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.example.smombie.R

class AlertTextView(context: Context, attrs: AttributeSet? = null) : OverlayView(context, attrs) {
    private val textView: TextView
    private val mBackground: Drawable

    init {
        textView = TextView(context, attrs)
        mBackground = AppCompatResources.getDrawable(context, R.drawable.rounded_corner)!!
        addView(textView)
        setColorAndText(Color.GREEN, "SAFE")
    }

    fun setColorAndText(@ColorInt colorInt: Int, newText: String) {
        val wrapped = DrawableCompat.wrap(mBackground)
        DrawableCompat.setTint(wrapped, colorInt)
        background = wrapped
        textView.text = newText
    }
}