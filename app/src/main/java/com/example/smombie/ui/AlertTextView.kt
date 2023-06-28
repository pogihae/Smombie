package com.example.smombie.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.example.smombie.R

@SuppressLint("ViewConstructor")
class AlertTextView(context: Context) : OverlayView(context) {
    private val textView: TextView
    private val mBackground: Drawable

    init {
        textView = TextView(context)
        mBackground = AppCompatResources.getDrawable(context, R.drawable.rounded_corner)!!
        addView(textView)
        setColor(Color.YELLOW)
    }

    fun setColor(@ColorInt colorInt: Int) {
        val wrapped = DrawableCompat.wrap(mBackground)
        DrawableCompat.setTint(wrapped, colorInt)
        background = wrapped

    }

    fun setText(text: String) {
        textView.text = text
    }

    override fun startAlert() {
        setColor(Color.RED)
        setText("HAZARD")
    }

    override fun stopAlert() {
        setColor(Color.GREEN)
        setText("SAFE")
    }
}