package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.supernova.launcher.R

// What a tile's card wears while the tiles are being arranged: a gear in its
// top right corner, in a circle (the card's settings), and a grip in its bottom
// right one (a drag of it resizes). Also where those are, for a touch to ask.
internal class EditMarks(private val card: ViewGroup) {

    private val density = card.resources.displayMetrics.density
    private val grip = ContextCompat.getDrawable(card.context, R.drawable.ic_fork_resize_handle)?.mutate()
    private val gear = ContextCompat.getDrawable(card.context, R.drawable.ic_fork_cat_settings)?.mutate()
    private val backing = GradientDrawable().apply { shape = GradientDrawable.OVAL }

    // A small tile has its gear in the bottom right corner, out of the way of its name along the top, and
    // its grip in the bottom left one.
    var small = false
        set(value) {
            if (field == value) return
            field = value
            card.invalidate()
        }

    fun setColor(color: Int) {
        grip?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        gear?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        backing.setColor((color and COLOR_MASK) or (BACKING_ALPHA shl ALPHA_SHIFT))
        card.invalidate()
    }

    // The grip's target: the card's bottom right corner, the bottom left one on a small tile.
    fun onGrip(x: Float, y: Float): Boolean {
        val size = target()
        val across = if (small) x <= size else x >= card.width - size
        return across && y >= card.height - size
    }

    // The gear's target: the card's top right corner, the bottom right one on a small tile.
    fun onGear(x: Float, y: Float): Boolean {
        val size = target()
        val down = if (small) y >= card.height - size else y <= size
        return x >= card.width - size && down
    }

    // The size of a target: [TARGET_DP], or a third of a narrow card, so on a small tile the gear and the grip
    // do not take its whole lower half between them.
    private fun target(): Float = minOf(TARGET_DP * density, card.width / TARGETS_ACROSS)

    fun draw(canvas: Canvas) {
        val gearInset = (GEAR_INSET_DP * density).toInt()
        val circle = (GEAR_BACKING_DP * density).toInt()
        val circleLeft = card.width - gearInset - circle
        val circleTop = if (small) card.height - gearInset - circle else gearInset
        backing.setBounds(circleLeft, circleTop, circleLeft + circle, circleTop + circle)
        backing.draw(canvas)
        val mark = (GEAR_DP * density).toInt()
        place(canvas, gear, circleLeft + (circle - mark) / 2, circleTop + (circle - mark) / 2, mark)
        val gripInset = (GRIP_INSET_DP * density).toInt()
        val gripSize = (GRIP_DP * density).toInt()
        val gripLeft = if (small) gripInset else card.width - gripInset - gripSize
        place(canvas, grip, gripLeft, card.height - gripInset - gripSize, gripSize)
    }

    private fun place(canvas: Canvas, drawable: Drawable?, left: Int, top: Int, size: Int) {
        drawable?.setBounds(left, top, left + size, top + size)
        drawable?.draw(canvas)
    }

    private companion object {
        const val GRIP_DP = 18f
        const val GRIP_INSET_DP = 8f
        const val GEAR_DP = 16f
        const val GEAR_BACKING_DP = 28f
        const val GEAR_INSET_DP = 6f
        const val TARGET_DP = 44f
        const val TARGETS_ACROSS = 3f
        const val BACKING_ALPHA = 0x40
        const val COLOR_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
    }
}
