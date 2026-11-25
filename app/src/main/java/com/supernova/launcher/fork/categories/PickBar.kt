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
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import com.supernova.launcher.R

// A plate in the accent colour, so what it says cannot be missed: a line of text and, at its end, one button. While a
// category's large icons are being chosen it says what to tap next and gives the choosing up; it is also what the
// hints are told on.
@SuppressLint("ViewConstructor") // Built in code, with the callback it needs.
internal class PickBar(
    context: Context,
    @StringRes buttonText: Int = R.string.cancel,
    private val onButton: () -> Unit
) : LinearLayout(context) {

    private val density = context.resources.displayMetrics.density
    private val text = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        maxLines = MAX_LINES
        ellipsize = TextUtils.TruncateAt.END
    }
    private val button = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        gravity = Gravity.CENTER
        setText(buttonText)
    }
    private val shape = GradientDrawable().apply { cornerRadius = dp(CORNER_DP).toFloat() }
    private val buttonShape = GradientDrawable().apply { cornerRadius = dp(BUTTON_CORNER_DP).toFloat() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(PADDING_DP)
        setPadding(pad, pad / 2, pad / 2, pad / 2)
        background = shape
        addView(text, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        button.background = buttonShape
        button.setPadding(pad, pad / 2, pad, pad / 2)
        addView(button, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginStart = pad })
        button.setOnClickListener { onButton() }
        // The grid under it is not to take a touch that lands on the bar.
        isClickable = true
    }

    fun setLook(accent: Int) {
        // Text and button read on the accent whichever way it leans.
        val onAccent = if (ColorUtils.calculateLuminance(accent) > LIGHT_ACCENT) DARK else LIGHT
        shape.setColor(accent)
        buttonShape.setColor((onAccent and COLOR_MASK) or (BUTTON_ALPHA shl ALPHA_SHIFT))
        text.setTextColor(onAccent)
        button.setTextColor(onAccent)
    }

    fun setButtonShown(shown: Boolean) {
        button.visibility = if (shown) VISIBLE else GONE
    }

    fun setMessage(message: String) {
        text.text = message
    }

    private fun dp(value: Float): Int = (value * density).toInt()

    private companion object {
        const val TEXT_SP = 15f
        const val MAX_LINES = 4
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
