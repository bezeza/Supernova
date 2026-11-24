package com.supernova.launcher.fork.categories

import kotlin.math.hypot

// What dragging a tile's corner does to its size: the size whose footprint is
// nearest to where the finger has taken the tile's corner, across and down.
// Plain arithmetic, so it is testable without a view.
internal object TileResize {

    // The size whose footprint, in cells across and down, is nearest to where a corner dragged to [cols] by [rows]
    // cells has taken the tile: the one the tile snaps to while its corner is held.
    fun sizeForCells(cols: Float, rows: Float): CategorySize =
        CategorySize.entries.minByOrNull { size ->
            val footprint = CellFlow.footprint(size)
            hypot(footprint.w - cols, footprint.h - rows)
        } ?: CategorySize.NORMAL

}
