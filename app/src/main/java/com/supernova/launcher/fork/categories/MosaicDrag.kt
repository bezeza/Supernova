package com.supernova.launcher.fork.categories

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.hypot

// Dragging a tile of the mosaic from cell to cell. A finger that holds a tile (its menu is up, or the tiles are being
// arranged) and then moves takes the tile off: the menu goes, the tile follows the finger, and the cell it is over is
// shown at once with the tiles it would push down moved out of its way. Let go, and it stays there, the others where
// they went. This is a touch listener of the list: it sees the moves of a touch whose tile has the finger.
@Suppress("TooManyFunctions") // A touch listener is a few overrides and the steps of one gesture.
internal class MosaicDrag(
    private val recycler: RecyclerView,
    private val host: Host
) : RecyclerView.OnItemTouchListener {

    // What the drag needs of the list and what it changes.
    interface Host {
        // The tiles as they stand, each in its cell.
        fun placed(): List<Placed>

        // The cells to show instead (null: as they stand).
        fun preview(cells: List<Placed>?)

        // The tile was put down: these are the cells it and the others now have.
        fun commit(cells: List<Placed>)

        // The folder id of the tile a view is, null for a view that is no tile.
        fun idOf(view: View): String?

        // The drag began: whatever was open over the tile goes.
        fun dragStarted()

        // True while the tiles are being arranged: a tile is taken by a touch and a move, with no hold.
        fun arranging(): Boolean
    }

    private val slop = ViewConfiguration.get(recycler.context).scaledTouchSlop
    private val edgeZone = (EDGE_ZONE_DP * recycler.resources.displayMetrics.density).toInt()
    private val edgeStep = (EDGE_STEP_DP * recycler.resources.displayMetrics.density).toInt()

    private var armed: View? = null
    private var armedId: String? = null
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var grabX = 0f
    private var grabY = 0f
    private var start: List<Placed> = emptyList()
    private var shown: List<Placed>? = null
    private var lastCell: Pair<Int, Int>? = null
    private var fingerY = 0f

    private val scroller = object : Runnable {
        override fun run() {
            if (!dragging) return
            val toward = when {
                fingerY < edgeZone -> -edgeStep
                fingerY > recycler.height - edgeZone -> edgeStep
                else -> 0
            }
            if (toward != 0) {
                recycler.scrollBy(0, toward)
                follow(lastRawX, lastRawY)
                recycler.postOnAnimation(this)
            }
        }
    }
    private var lastRawX = 0f
    private var lastRawY = 0f

    val isDragging: Boolean get() = dragging

    // Where the finger last went down on the list, on the screen: a tile's menu opens there.
    var lastDownX = 0f
        private set
    var lastDownY = 0f
        private set

    // The finger holds this tile: a move of it from here takes the tile.
    fun arm(view: View, id: String, rawX: Float, rawY: Float) {
        armed = view
        armedId = id
        downX = rawX
        downY = rawY
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastDownX = e.rawX
                lastDownY = e.rawY
                if (host.arranging()) armUnder(e)
            }
            MotionEvent.ACTION_MOVE -> if (armed != null && !dragging && moved(e)) begin(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (!dragging) disarm()
        }
        return dragging
    }

    private fun armUnder(e: MotionEvent) {
        val view = recycler.findChildViewUnder(e.x, e.y) ?: return
        val id = host.idOf(view) ?: return
        arm(view, id, e.rawX, e.rawY)
    }

    private fun moved(e: MotionEvent): Boolean = hypot(e.rawX - downX, e.rawY - downY) > slop

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> if (dragging) follow(e.rawX, e.rawY)
            MotionEvent.ACTION_UP -> finish(commit = true)
            MotionEvent.ACTION_CANCEL -> finish(commit = false)
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    private fun begin(e: MotionEvent) {
        val view = armed ?: return
        host.dragStarted()
        dragging = true
        start = host.placed()
        shown = null
        lastCell = null
        val at = IntArray(2)
        recycler.getLocationOnScreen(at)
        grabX = e.rawX - at[0] - view.left
        grabY = e.rawY - at[1] - view.top
        view.translationZ = LIFT_DP * recycler.resources.displayMetrics.density
        view.alpha = LIFTED_ALPHA
        view.scaleX = LIFTED_SCALE
        view.scaleY = LIFTED_SCALE
        recycler.parent?.requestDisallowInterceptTouchEvent(true)
        follow(e.rawX, e.rawY)
    }

    // The tile goes where the finger is, and the cell it is over decides where the others stand.
    private fun follow(rawX: Float, rawY: Float) {
        val view = armed ?: return
        lastRawX = rawX
        lastRawY = rawY
        val at = IntArray(2)
        recycler.getLocationOnScreen(at)
        val x = rawX - at[0]
        val y = rawY - at[1]
        fingerY = y
        view.translationX = x - grabX - view.left
        view.translationY = y - grabY - view.top
        previewAt(x - grabX, y - grabY)
        recycler.removeCallbacks(scroller)
        if (y < edgeZone || y > recycler.height - edgeZone) recycler.postOnAnimation(scroller)
    }

    // The tile's top left corner is at ([left], [top]) of the list: the cell it is over, a little inside, decides
    // where the others stand.
    private fun previewAt(left: Float, top: Float) {
        val lm = recycler.layoutManager as? MosaicLayoutManager
        val id = armedId
        if (lm != null && id != null) {
            val inside = lm.columnWidth() / 2f
            val cell = lm.cellAt(left + inside, top + inside)
            if (cell != lastCell) {
                lastCell = cell
                val cells = CellGrid.drop(start, id, cell.first, cell.second, lm.spanCount)
                shown = cells
                host.preview(cells)
            }
        }
    }

    private fun finish(commit: Boolean) {
        if (!dragging) {
            disarm()
            return
        }
        recycler.removeCallbacks(scroller)
        val view = armed
        view?.animate()?.translationX(0f)?.translationY(0f)?.translationZ(0f)?.alpha(1f)?.scaleX(1f)?.scaleY(1f)
            ?.setDuration(SETTLE_MS)?.start()
        val cells = shown
        dragging = false
        if (commit && cells != null) host.commit(cells) else host.preview(null)
        disarm()
    }

    private fun disarm() {
        armed = null
        armedId = null
        lastCell = null
        shown = null
    }

    private companion object {
        const val EDGE_ZONE_DP = 72f
        const val EDGE_STEP_DP = 14f
        const val LIFT_DP = 12f
        const val LIFTED_ALPHA = 0.92f
        const val LIFTED_SCALE = 1.04f
        const val SETTLE_MS = 120L
    }
}
