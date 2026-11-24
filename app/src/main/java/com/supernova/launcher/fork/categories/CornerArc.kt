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
    // A halo of the opposite shade round it, so the arc shows on a light tile and on a dark one alike.
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke + HALO_BOTH_SIDES * HALO_DP * density
        strokeCap = Paint.Cap.ROUND
        this.color = contrastOf(color)
    }
    private val oval = RectF()

    // The bounds are the box the arc lies in: its centre is the box's top left corner, its radius the box's width, and
    // it is the quarter that bulges to the bottom right.
    override fun draw(canvas: Canvas) {
        val r = bounds
        val radius = r.width() - stroke / 2
        oval.set(r.left - radius, r.top - radius, r.left + radius, r.top + radius)
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, false, halo)
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, false, paint)
    }

    // Black over a light colour, white over a dark one, at about two thirds of full strength.
    private fun contrastOf(color: Int): Int {
        val luminance = (RED * ((color shr RED_SHIFT) and CHANNEL) + GREEN * ((color shr GREEN_SHIFT) and CHANNEL) +
            BLUE * (color and CHANNEL)) / CHANNEL
        val base = if (luminance > LIGHT) BLACK else WHITE
        return (HALO_ALPHA shl ALPHA_SHIFT) or base
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        halo.alpha = alpha * HALO_ALPHA / CHANNEL
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val STROKE_DP = 4f
        const val HALO_DP = 1.5f
        const val HALO_BOTH_SIDES = 2
        const val HALO_ALPHA = 0xAA
        const val ALPHA_SHIFT = 24
        const val CHANNEL = 0xFF
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val RED = 0.299f
        const val GREEN = 0.587f
        const val BLUE = 0.114f
        const val LIGHT = 140
        const val BLACK = 0x000000
        const val WHITE = 0xFFFFFF
        const val START_ANGLE = 0f
        const val SWEEP_ANGLE = 90f
    }
}
