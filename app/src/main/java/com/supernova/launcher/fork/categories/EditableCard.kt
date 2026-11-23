package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.ViewGroup

// A tile's card that can be arranged: while it is, the card keeps every touch
// (its icons take no taps) and its corner is a resize grip, see [EditGestures].
internal abstract class EditableCard(context: Context) : ViewGroup(context) {

    val edit = EditGestures(this)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = edit.editing || super.onInterceptTouchEvent(ev)

    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (edit.editing) edit.onTouch(event) else super.onTouchEvent(event)

    override fun performClick(): Boolean = super.performClick()

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        edit.draw(canvas)
    }
}
