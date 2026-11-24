package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

// The arc a tile wears at its bottom right corner while its menu is open: a finger on it and a move resizes the
// tile. It hugs the corner of the card from outside, as the stock launcher's does.
internal class CornerArc(density: Float, color: Int) : Drawable() {

    private val stroke = STROKE_DP * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        this.color = color
    }
    private val oval = RectF()

    // The bounds are the box the arc lies in: its centre is the box's top left corner, its radius the box's width, and
    // it is the quarter that bulges to the bottom right.
    override fun draw(canvas: Canvas) {
        val r = bounds
        val radius = r.width() - stroke / 2
        oval.set(r.left - radius, r.top - radius, r.left + radius, r.top + radius)
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, false, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val STROKE_DP = 4f
        const val START_ANGLE = 0f
        const val SWEEP_ANGLE = 90f
    }
}
