package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.supernova.launcher.R
import kotlin.math.hypot

// What a tile's card does while the tiles are being arranged. Its icons stop
// taking taps (the card keeps every touch), a tap on the card is its click
// (its settings), and a drag that starts on the grip in its corner is a
// resize, told to [onResize] as the distance the finger went to the right
// once it lets go. A hold is left to the list, which drags the tile.
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

    var onResize: (Float) -> Unit = {}

    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var onGrip = false
    // A gesture this card saw from its DOWN: the hold that started the arranging goes on past it, and is no tap.
    private var tracking = false

    fun setColor(color: Int) {
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
            MotionEvent.ACTION_MOVE ->
                if (!moved && hypot(event.x - downX, event.y - downY) > slop) moved = true
            MotionEvent.ACTION_UP -> finish(event)
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return true
    }

    private fun finish(event: MotionEvent) {
        when {
            !tracking -> Unit
            onGrip && moved -> onResize(event.x - downX)
            !moved -> card.performClick()
        }
        onGrip = false
        tracking = false
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

    // The grip of a card whose parent is [view]: one step of a resize is the width of a normal tile.
    fun stepPx(list: View?): Float = (list?.width ?: 0) / 2f

    private companion object {
        const val GRIP_DP = 18f
        const val GRIP_INSET_DP = 8f
        const val GRIP_TARGET_DP = 44f
    }
}
