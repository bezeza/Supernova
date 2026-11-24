package com.supernova.launcher.fork.categories

// Which tiles a drag takes along. Plain, so it is testable without a view.
internal object DragGroup {

    // The folder ids a drag of tile [id] takes along: its block of four small tiles, else it alone. While the tiles
    // are arranged each goes by itself, so one can be taken out of a block.
    fun of(items: List<Placed>, id: String, arranging: Boolean): List<String> =
        if (arranging) listOf(id) else CellGrid.block(items, id).map { it.id }.ifEmpty { listOf(id) }
}
