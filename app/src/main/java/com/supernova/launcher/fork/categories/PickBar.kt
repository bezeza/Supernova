package com.supernova.launcher.fork.categories

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.supernova.launcher.R

// The bar under a category's header while its large icons are being chosen, in the accent colour so the mode cannot be
// missed: what to tap next ("Choose large icon 1 of 3") and a button that gives the choosing up.
@SuppressLint("ViewConstructor") // Built in code, with the callback it needs.
internal class PickBar(context: Context, private val onCancel: () -> Unit) : LinearLayout(context) {

    private val density = context.resources.displayMetrics.density
    private val text = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    private val cancel = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        gravity = Gravity.CENTER
        setText(R.string.cancel)
    }
    private val shape = GradientDrawable().apply { cornerRadius = dp(CORNER_DP).toFloat() }
    private val button = GradientDrawable().apply { cornerRadius = dp(BUTTON_CORNER_DP).toFloat() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(PADDING_DP)
        setPadding(pad, pad / 2, pad / 2, pad / 2)
        background = shape
        addView(text, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        cancel.background = button
        cancel.setPadding(pad, pad / 2, pad, pad / 2)
        addView(cancel, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginStart = pad })
        cancel.setOnClickListener { onCancel() }
        // The grid under it is not to take a touch that lands on the bar.
        isClickable = true
    }

    fun setLook(accent: Int) {
        // Text and button read on the accent whichever way it leans.
        val onAccent = if (ColorUtils.calculateLuminance(accent) > LIGHT_ACCENT) DARK else LIGHT
        shape.setColor(accent)
        button.setColor((onAccent and COLOR_MASK) or (BUTTON_ALPHA shl ALPHA_SHIFT))
        text.setTextColor(onAccent)
        cancel.setTextColor(onAccent)
    }

    fun setMessage(message: String) {
        text.text = message
    }

    private fun dp(value: Float): Int = (value * density).toInt()

    private companion object {
        const val TEXT_SP = 15f
        const val PADDING_DP = 14f
        const val CORNER_DP = 18f
        const val BUTTON_CORNER_DP = 14f
        const val LIGHT_ACCENT = 0.5
        const val DARK = 0xFF101216.toInt()
        const val LIGHT = 0xFFFFFFFF.toInt()
        const val COLOR_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
        const val BUTTON_ALPHA = 0x33
    }
}
