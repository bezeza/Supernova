package com.supernova.launcher.fork.categories

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.hypot

// The widths a tile can be dragged to, in pixels.
internal class ResizeTargets(val small: Float, val normal: Float, val wide: Float)

// What a tile's card does while the tiles are being arranged. Its icons stop
// taking taps (the card keeps every touch). A gear in its top corner opens its
// settings ([onGear]); a tap elsewhere on it does nothing. A drag that starts on
// the grip in its bottom corner is a resize: an outline follows the finger over
// the list, [onPreview] says how wide it is as it goes (the tile takes that size
// at once and the others make way), and [onResize] once the finger lets go. A
// hold is left to the list, which drags the tile.
internal class EditGestures(private val card: ViewGroup) {

    private val density = card.resources.displayMetrics.density
    private val slop = ViewConfiguration.get(card.context).scaledTouchSlop
    private val marks = EditMarks(card)

    var editing = false
        set(value) {
            if (field == value) return
            field = value
            card.invalidate()
            keepGestureOff()
        }

    init {
        // The card's size changes as the others make way, and so do its corners.
        card.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> keepGestureOff() }
    }

    // The widths a tile can be dragged to, asked when a drag of the grip begins.
    var targets: () -> ResizeTargets? = { null }

    // Told how wide and how high the tile is dragged as the finger goes, and once it lets go.
    var onPreview: (Float, Float) -> Unit = { _, _ -> }
    var onResize: (Float, Float) -> Unit = { _, _ -> }

    // A tap on the gear.
    var onGear: () -> Unit = {}

    private val outline = GradientDrawable().apply { cornerRadius = density * OUTLINE_CORNER_DP }
    private var color = 0
    private var dragged = 0f
    private var draggedHeight = 0f
    private var startWidth = 0
    private var startHeight = 0
    private var downRawY = 0f
    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var moved = false
    private var onGrip = false
    private var gearHit = false
    private var shown = false

    // A gesture this card saw from its DOWN: the hold that started the arranging goes on past it, and is no tap.
    private var tracking = false

    // A small tile wears its marks in the bottom corners, the gear at the right.
    var small: Boolean
        get() = marks.small
        set(value) {
            marks.small = value
            keepGestureOff()
        }

    fun setColor(color: Int) {
        this.color = color
        marks.setColor(color)
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> down(event)
            MotionEvent.ACTION_MOVE -> {
                if (!moved && hypot(event.x - downX, event.y - downY) > slop) moved = true
                if (onGrip && moved) follow(event.rawX - downRawX, event.rawY - downRawY)
            }
            MotionEvent.ACTION_UP -> finish()
            MotionEvent.ACTION_CANCEL -> {
                tracking = false
                settle()
            }
        }
        return true
    }

    private fun down(event: MotionEvent) {
        downX = event.x
        downY = event.y
        // The finger's own distance, not the card's: the card moves and grows as the others make way.
        downRawX = event.rawX
        downRawY = event.rawY
        startWidth = card.width
        startHeight = card.height
        moved = false
        tracking = true
        gearHit = marks.onGear(event.x, event.y)
        onGrip = !gearHit && marks.onGrip(event.x, event.y)
        // A drag of the grip is the card's: the list must not take it for a tile drag.
        if (onGrip) card.parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun finish() {
        when {
            !tracking -> Unit
            onGrip && moved -> onResize(dragged, draggedHeight)
            !moved && gearHit -> onGear()
        }
        onGrip = false
        gearHit = false
        tracking = false
        settle()
    }

    // While arranging, the system's Back gesture at the screen's edge does not take a touch on the gear or the
    // grip (a wide tile's reach the edge). The system keeps only so many of these per edge, those of the
    // tiles on the screen first.
    private fun keepGestureOff() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        card.systemGestureExclusionRects = if (editing) marks.zones() else emptyList()
    }

    fun draw(canvas: Canvas) {
        if (editing) marks.draw(canvas)
    }

    // The outline over the list takes the width and height the finger gives the tile's corner, at once.
    private fun follow(dx: Float, dy: Float) {
        val target = targets() ?: return
        val list = list() ?: return
        dragged = (startWidth + dx).coerceIn(target.small, target.wide)
        draggedHeight = (startHeight + dy).coerceIn(target.small, target.wide)
        val height = draggedHeight
        val at = Rect(0, 0, card.width, card.height)
        list.offsetDescendantRectToMyCoords(card, at)
        outline.setColor((color and COLOR_MASK) or (OUTLINE_FILL_ALPHA shl ALPHA_SHIFT))
        val stroke = (color and COLOR_MASK) or (OUTLINE_STROKE_ALPHA shl ALPHA_SHIFT)
        outline.setStroke((OUTLINE_STROKE_DP * density).toInt(), stroke)
        outline.setBounds(at.left, at.top, at.left + dragged.toInt(), at.top + height.toInt())
        if (!shown) {
            list.overlay.add(outline)
            shown = true
        }
        list.invalidate()
        onPreview(dragged, draggedHeight)
    }

    private fun settle() {
        if (!shown) return
        list()?.overlay?.remove(outline)
        shown = false
    }

    // The list the card is in: the outline is drawn over it, outside the card's own bounds.
    private fun list(): RecyclerView? =
        generateSequence(card.parent) { it.parent }.filterIsInstance<RecyclerView>().firstOrNull()

    private companion object {
        const val OUTLINE_CORNER_DP = 24f
        const val OUTLINE_STROKE_DP = 2f
        const val OUTLINE_FILL_ALPHA = 0x30
        const val OUTLINE_STROKE_ALPHA = 0xB0
        const val COLOR_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
    }
}
