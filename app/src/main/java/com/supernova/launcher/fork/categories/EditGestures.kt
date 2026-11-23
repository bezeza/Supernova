package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.PorterDuff
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.supernova.launcher.R
import kotlin.math.hypot

// The widths a tile can be dragged to, in pixels, and how high a wide one is.
internal class ResizeTargets(val small: Float, val normal: Float, val wide: Float, val wideHeight: Float)

// What a tile's card does while the tiles are being arranged. Its icons stop
// taking taps (the card keeps every touch), a tap on the card is its click
// (its settings), and a drag that starts on the grip in its corner is a
// resize: an outline follows the finger over the list, one width and as flat as
// that width asks, and [onResize] is told how wide it was once the finger lets
// go. A hold is left to the list, which drags the tile.
internal class EditGestures(private val card: ViewGroup) {

    private val density = card.resources.displayMetrics.density
    private val slop = ViewConfiguration.get(card.context).scaledTouchSlop
    private val grip = ContextCompat.getDrawable(card.context, R.drawable.ic_fork_resize_handle)?.mutate()

    var editing = false
        set(value) {
            if (field == value) return
            field = value
            card.invalidate()
        }

    // The widths a tile can be dragged to, asked when a drag of the grip begins.
    var targets: () -> ResizeTargets? = { null }

    // Told how wide the tile was dragged, in pixels.
    var onResize: (Float) -> Unit = {}

    private val outline = GradientDrawable().apply { cornerRadius = density * OUTLINE_CORNER_DP }
    private var outlineColor = 0
    private var dragged = 0f

    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var onGrip = false
    // A gesture this card saw from its DOWN: the hold that started the arranging goes on past it, and is no tap.
    private var tracking = false

    fun setColor(color: Int) {
        outlineColor = color
        grip?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        card.invalidate()
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                moved = false
                tracking = true
                onGrip = onGrip(event.x, event.y)
                // A drag of the grip is the card's: the list must not take it for a tile drag.
                if (onGrip) card.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moved && hypot(event.x - downX, event.y - downY) > slop) moved = true
                if (onGrip && moved) follow(event.x - downX)
            }
            MotionEvent.ACTION_UP -> finish()
            MotionEvent.ACTION_CANCEL -> {
                tracking = false
                settle()
            }
        }
        return true
    }

    private fun finish() {
        when {
            !tracking -> Unit
            onGrip && moved -> onResize(dragged)
            !moved -> card.performClick()
        }
        onGrip = false
        tracking = false
        settle()
    }

    // The outline over the list takes the width the finger gives the tile's edge, at once.
    private fun follow(dx: Float) {
        val target = targets() ?: return
        val list = list() ?: return
        dragged = (card.width + dx).coerceIn(target.small, target.wide)
        val height = TileGeometry.outlineHeight(dragged, target.normal, target.wide, target.wideHeight)
        val at = Rect(0, 0, card.width, card.height)
        list.offsetDescendantRectToMyCoords(card, at)
        outline.setColor((outlineColor and COLOR_MASK) or (OUTLINE_FILL_ALPHA shl ALPHA_SHIFT))
        val stroke = (outlineColor and COLOR_MASK) or (OUTLINE_STROKE_ALPHA shl ALPHA_SHIFT)
        outline.setStroke((OUTLINE_STROKE_DP * density).toInt(), stroke)
        outline.setBounds(at.left, at.top, at.left + dragged.toInt(), at.top + height.toInt())
        if (!shown) {
            list.overlay.add(outline)
            shown = true
        }
        list.invalidate()
    }

    private var shown = false

    // The list the card is in: the outline is drawn over it, outside the card's own bounds.
    private fun list(): RecyclerView? =
        generateSequence(card.parent) { it.parent }.filterIsInstance<RecyclerView>().firstOrNull()

    private fun settle() {
        if (!shown) return
        list()?.overlay?.remove(outline)
        shown = false
    }

    // The grip's target: the card's bottom right corner.
    private fun onGrip(x: Float, y: Float): Boolean {
        val size = GRIP_TARGET_DP * density
        return x >= card.width - size && y >= card.height - size
    }

    fun draw(canvas: Canvas) {
        val drawable = grip ?: return
        if (!editing) return
        val size = (GRIP_DP * density).toInt()
        val inset = (GRIP_INSET_DP * density).toInt()
        val right = card.width - inset
        val bottom = card.height - inset
        drawable.setBounds(right - size, bottom - size, right, bottom)
        drawable.draw(canvas)
    }

    private companion object {
        const val GRIP_DP = 18f
        const val GRIP_INSET_DP = 8f
        const val GRIP_TARGET_DP = 44f
        const val OUTLINE_CORNER_DP = 24f
        const val OUTLINE_STROKE_DP = 2f
        const val OUTLINE_FILL_ALPHA = 0x30
        const val OUTLINE_STROKE_ALPHA = 0xB0
        const val COLOR_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
    }
}
