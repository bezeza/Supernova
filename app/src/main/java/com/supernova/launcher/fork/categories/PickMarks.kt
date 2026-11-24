package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView

// The marks on the apps chosen as large icons while they are being chosen: a frame in the accent colour round the app
// and a badge with its number, 1, 2 or 3, so a tap is seen to have taken and in which order. Drawn over the grid, so
// the drawer's own adapter and its cells are not touched.
internal class PickMarks(private val density: Float) : RecyclerView.ItemDecoration() {

    // Which large icon the app at an adapter position is, null if it is not chosen (or nothing is being chosen).
    var orderAt: (Int) -> Int? = { null }
    var accent = 0

    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = FRAME_DP * density
    }
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val digit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = DIGIT_SP * density
        isFakeBoldText = true
    }
    private val box = RectF()

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val onAccent = if (ColorUtils.calculateLuminance(accent) > LIGHT_ACCENT) DARK else LIGHT
        frame.color = accent
        disc.color = accent
        digit.color = onAccent
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val order = orderAt(parent.getChildAdapterPosition(child)) ?: continue
            mark(canvas, child, order)
        }
    }

    private fun mark(canvas: Canvas, child: View, order: Int) {
        val inset = FRAME_INSET_DP * density
        box.set(child.left + inset, child.top + inset, child.right - inset, child.bottom - inset)
        canvas.drawRoundRect(box, CORNER_DP * density, CORNER_DP * density, frame)
        val radius = BADGE_DP * density
        val cx = box.right - radius * BADGE_SHIFT
        val cy = box.top + radius * BADGE_SHIFT
        canvas.drawCircle(cx, cy, radius, disc)
        canvas.drawText(order.toString(), cx, cy - (digit.ascent() + digit.descent()) / 2f, digit)
    }

    private companion object {
        const val FRAME_DP = 3f
        const val FRAME_INSET_DP = 2f
        const val CORNER_DP = 16f
        const val BADGE_DP = 12f
        const val BADGE_SHIFT = 0.8f
        const val DIGIT_SP = 14f
        const val LIGHT_ACCENT = 0.5
        const val DARK = 0xFF101216.toInt()
        const val LIGHT = 0xFFFFFFFF.toInt()
    }
}
