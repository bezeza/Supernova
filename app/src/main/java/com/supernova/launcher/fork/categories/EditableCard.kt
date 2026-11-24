package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import kotlin.math.hypot

// A tile's card that can be arranged: while it is, the card keeps every touch (its icons take no taps) and a tap
// on it, one that does not move, is [onEditTap]: the tile's menu. A hold or a move is the mosaic's, which drags
// the tile ([MosaicDrag]).
internal abstract class EditableCard(context: Context) : ViewGroup(context) {

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var moved = false

    var editing = false

    var onEditTap: () -> Unit = {}

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = editing || super.onInterceptTouchEvent(ev)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editing) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                tracking = true
                moved = false
            }
            MotionEvent.ACTION_MOVE ->
                if (!moved && hypot(event.x - downX, event.y - downY) > slop) moved = true
            MotionEvent.ACTION_UP -> {
                if (tracking && !moved) onEditTap()
                tracking = false
            }
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
