package com.supernova.launcher.fork.categories

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

// Arranging the tiles: while it is on the tiles shake, a tap on one is its menu, and a hold drags it (the mosaic's
// own drag, [MosaicDrag]). This only starts and stops it and tells the bar that ends it to come and go.
internal class TileArranger(
    private val recycler: RecyclerView,
    private val rows: CategoriesAdapter,
    // Told when arranging starts and stops: the bar that ends it comes and goes.
    private val onEditing: (Boolean) -> Unit = {}
) {

    val isOn: Boolean get() = rows.editing

    fun start() {
        if (rows.editing) return
        rows.setEditing(true)
        onEditing(true)
        // The tiles make way for one another, but a changed tile keeps its own view: a drag is on it.
        recycler.itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }
    }

    // True when the tiles were being arranged: the Back that ended it is spent.
    fun stop(): Boolean {
        if (!rows.editing) return false
        recycler.itemAnimator = null
        rows.setEditing(false)
        onEditing(false)
        return true
    }
}
