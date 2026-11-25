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

// The bar that is on screen while the tiles are arranged: how to, and the button that ends it. It sits over the
// bottom of the tiles' list, not in the list, so it is seen without scrolling however long the list is.
@SuppressLint("ViewConstructor") // Built in code, with the callback it needs.
internal class PinnedDoneBar(context: Context, private val onDone: () -> Unit) : LinearLayout(context) {

    private val density = context.resources.displayMetrics.density
    private val hint = label(HINT_SP, lines = HINT_LINES)
    private val done = label(DONE_SP, lines = 1).apply { gravity = Gravity.CENTER }
    private val shape = GradientDrawable().apply { cornerRadius = dp(CORNER_DP).toFloat() }
    private val button = GradientDrawable().apply { cornerRadius = dp(BUTTON_CORNER_DP).toFloat() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(PADDING_DP)
        setPadding(pad, pad / 2, pad, pad / 2)
        background = shape
        elevation = dp(ELEVATION_DP).toFloat()
        hint.setText(R.string.fork_edit_hint)
        addView(hint, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        done.setText(R.string.fork_edit_done)
        done.background = button
        done.setPadding(pad, pad / 2, pad, pad / 2)
        addView(done, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginStart = pad })
        done.setOnClickListener { onDone() }
        // The tiles' list under it is not to take a touch that lands on the bar.
        isClickable = true
    }

    // The bar is solid, the tiles scroll under it: the tiles' own tint is see-through.
    fun setLook(textColor: Int, tile: Int) {
        hint.setTextColor(textColor)
        done.setTextColor(textColor)
        val light = ColorUtils.calculateLuminance(textColor) > LIGHT_TEXT
        shape.setColor(if (light) SOLID_DARK else SOLID_LIGHT)
        button.setColor(tile)
    }

    private fun label(sizeSp: Float, lines: Int): TextView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        maxLines = lines
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun dp(value: Float): Int = (value * density).toInt()

    private companion object {
        const val HINT_SP = 15f
        const val HINT_LINES = 3
        const val DONE_SP = 14f
        const val PADDING_DP = 12f
        const val CORNER_DP = 18f
        const val BUTTON_CORNER_DP = 14f
        const val ELEVATION_DP = 4f
        const val LIGHT_TEXT = 0.5
        const val SOLID_DARK = 0xF2202329.toInt()
        const val SOLID_LIGHT = 0xF2F1F2F4.toInt()
    }
}
