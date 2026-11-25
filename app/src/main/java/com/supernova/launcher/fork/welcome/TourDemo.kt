package com.supernova.launcher.fork.welcome

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

// The small moving picture of a tour page: tiles with the phone's own app icons that grow, move or slide in. It is
// drawn, not made of the launcher's views, and takes no touches. With the system's animations off it stands still.
@SuppressLint("ViewConstructor") // Built in code, with what it draws.
@Suppress("MagicNumber") // The bare numbers are places and sizes in cells of the picture.
internal class TourDemo(
    context: Context,
    private val scene: Scene,
    private val icons: List<Drawable>,
    ink: Int,
    // The names on the Feed's cards, top to bottom.
    private val labels: List<String> = emptyList()
) : View(context) {

    enum class Scene { MOSAIC, ARRANGE, FEED }

    private val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (ink and COLOR_MASK) or PLATE_ALPHA }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = TEXT_SP * resources.displayMetrics.scaledDensity
    }
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = PERIOD_MS
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    // The side of one cell of the four by four the picture is drawn on.
    private var cell = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        cell = minOf(width, height) / CELLS
        canvas.translate((width - cell * CELLS) / 2, (height - cell * CELLS) / 2)
        when (scene) {
            Scene.MOSAIC -> mosaic(canvas)
            Scene.ARRANGE -> arrange(canvas)
            Scene.FEED -> feed(canvas)
        }
    }

    // One tile grows from two cells wide to four and back; the one under it stays.
    private fun mosaic(canvas: Canvas) {
        val wide = 2f + 2f * phase
        tile(canvas, 0f, 0f, wide, 2f)
        icon(canvas, 0, 0f, 0f, 1f)
        icon(canvas, 1, 1f, 0f, 1f)
        icon(canvas, 2, 0f, 1f, 1f)
        icon(canvas, 3, 1f, 1f, 1f)
        if (wide > FULL) {
            icon(canvas, 4, 2f, 0f, 1f)
            icon(canvas, 5, 3f, 0f, 1f)
        }
        tile(canvas, 0f, 2f, 2f, 2f)
        icon(canvas, 6, 0.5f, 2.5f, 1f)
    }

    // A block of four small tiles moves to the other side as one.
    private fun arrange(canvas: Canvas) {
        tile(canvas, 0f, 0f, 4f, 2f)
        for (i in 0 until 4) icon(canvas, i, i.toFloat(), 0.5f, 1f)
        val left = 2f * phase
        for (i in 0 until 4) {
            val x = left + i % 2
            val y = 2f + i / 2
            tile(canvas, x, y, 1f, 1f)
            icon(canvas, 4 + i, x, y, 1f)
        }
    }

    // The Feed's cards slide in from the left, one after another.
    private fun feed(canvas: Canvas) {
        labels.forEachIndexed { index, label ->
            val shown = (phase * (labels.size + 1) - index).coerceIn(0f, 1f)
            val left = -CELLS * (1f - shown)
            tile(canvas, left, index.toFloat(), CELLS, 1f)
            // Names alone: an app's icon beside "Search" or "Calendar" would say it is that app.
            canvas.drawText(label, left * cell + cell * TEXT_INSET, (index + 0.5f) * cell + text.textSize / 3, text)
        }
    }

    private fun tile(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val gap = cell * GAP / 2
        val box = RectF(x * cell + gap, y * cell + gap, (x + w) * cell - gap, (y + h) * cell - gap)
        canvas.drawRoundRect(box, cell * CORNER, cell * CORNER, plate)
    }

    private fun icon(canvas: Canvas, index: Int, x: Float, y: Float, side: Float) {
        val drawable = icons.getOrNull(index % maxOf(icons.size, 1)) ?: return
        val inset = cell * side * ICON_INSET
        drawable.setBounds(
            (x * cell + inset).toInt(), (y * cell + inset).toInt(),
            ((x + side) * cell - inset).toInt(), ((y + side) * cell - inset).toInt()
        )
        drawable.draw(canvas)
    }

    private companion object {
        const val CELLS = 4f
        const val FULL = 3.95f
        const val GAP = 0.12f
        const val TEXT_INSET = 0.35f
        const val CORNER = 0.22f
        const val ICON_INSET = 0.18f
        const val TEXT_SP = 15f
        const val PERIOD_MS = 1800L
        const val COLOR_MASK = 0xFFFFFF
        const val PLATE_ALPHA = 0x26000000
    }
}
