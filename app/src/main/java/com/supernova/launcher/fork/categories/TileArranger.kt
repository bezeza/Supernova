package com.supernova.launcher.fork.categories

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Arranging the tiles: while it is on the tiles shake, a hold drags one to
// another place, and the new order goes to [onOrder] once it is dropped.
internal class TileArranger(
    private val recycler: RecyclerView,
    private val rows: CategoriesAdapter,
    private val onOrder: (List<String>) -> Unit,
    // Told when arranging starts and stops: the bar that ends it comes and goes.
    private val onEditing: (Boolean) -> Unit = {}
) {

    private var dragStart: List<String> = emptyList()

    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            if (rows.isDraggable(viewHolder.bindingAdapterPosition)) makeMovementFlags(DRAG_FLAGS, 0) else 0

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = rows.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        // A hold on a resize grip is a resize: the tile is not picked up under the finger.
        override fun isLongPressDragEnabled(): Boolean = !rows.gripHeld

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) return
            dragStart = rows.tileIds()
            // A bigger tile changes places with a whole block of small ones, which keep together.
            if (viewHolder != null && !rows.isSmallTile(viewHolder.bindingAdapterPosition)) {
                keepingScroll { rows.gather(true) }
            }
        }

        // Dropped: the new order is the folders' own, kept.
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val now = rows.tileIds()
            if (now != dragStart) onOrder(now)
            dragStart = now
            keepingScroll { rows.gather(false) }
        }
    })

    val isOn: Boolean get() = rows.editing

    // The list's first visible row stays where it is on screen while [change] regroups the tiles under it.
    private fun keepingScroll(change: () -> Unit) {
        val layout = recycler.layoutManager as? LinearLayoutManager
        val first = layout?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        val anchor = if (first == RecyclerView.NO_POSITION) null else rows.anchorAt(first)
        val top = layout?.findViewByPosition(first)?.top ?: 0
        change()
        val at = anchor?.let { rows.positionOf(it) } ?: RecyclerView.NO_POSITION
        if (layout != null && at != RecyclerView.NO_POSITION) layout.scrollToPositionWithOffset(at, top)
    }

    fun start() {
        if (rows.editing) return
        keepingScroll { rows.setEditing(true) }
        onEditing(true)
        // The tiles make way for one another, but a changed tile keeps its own view: a drag is on it.
        recycler.itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }
        touchHelper.attachToRecyclerView(recycler)
    }

    // True when the tiles were being arranged: the Back that ended it is spent.
    fun stop(): Boolean {
        if (!rows.editing) return false
        // No animation as the pairs form again, and the list stays where it is.
        recycler.itemAnimator = null
        keepingScroll { rows.setEditing(false) }
        onEditing(false)
        touchHelper.attachToRecyclerView(null)
        return true
    }

    private companion object {
        const val DRAG_FLAGS = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or
            ItemTouchHelper.END
    }
}
