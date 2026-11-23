package com.supernova.launcher.fork.categories

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

// Arranging the tiles: while it is on the tiles shake, a hold drags one to
// another place, and the new order goes to [onOrder] once it is dropped.
internal class TileArranger(
    private val recycler: RecyclerView,
    private val rows: CategoriesAdapter,
    private val onOrder: (List<String>) -> Unit
) {

    private var dragStart: List<String> = emptyList()

    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            if (rows.isTile(viewHolder.bindingAdapterPosition)) makeMovementFlags(DRAG_FLAGS, 0) else 0

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = rows.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) dragStart = rows.tileIds()
        }

        // Dropped: the new order is the folders' own, kept.
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val now = rows.tileIds()
            if (now != dragStart) onOrder(now)
            dragStart = now
        }
    })

    val isOn: Boolean get() = rows.editing

    fun start() {
        if (rows.editing) return
        rows.setEditing(true)
        touchHelper.attachToRecyclerView(recycler)
        recycler.scrollToPosition(0)
    }

    // True when the tiles were being arranged: the Back that ended it is spent.
    fun stop(): Boolean {
        if (!rows.editing) return false
        rows.setEditing(false)
        touchHelper.attachToRecyclerView(null)
        return true
    }

    private companion object {
        const val DRAG_FLAGS = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or
            ItemTouchHelper.END
    }
}
