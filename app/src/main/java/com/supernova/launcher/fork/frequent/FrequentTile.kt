package com.supernova.launcher.fork.frequent

import com.supernova.launcher.fork.categories.CategoryOptions
import com.supernova.launcher.fork.categories.CategorySize
import com.supernova.launcher.fork.categories.CellFlow
import com.supernova.launcher.fork.categories.CellGrid
import com.supernova.launcher.fork.categories.Entry
import com.supernova.launcher.fork.categories.Placed
import com.supernova.launcher.fork.categories.TileChange
import com.supernova.launcher.fork.categories.TileSlots

// The Frequent category as a tile of the mosaic. It is no folder: the drawer makes a group of this id for it, with
// the apps of the top. Its size and cell are kept with the other tiles' options under this id.
internal object FrequentTile {

    const val ID = "fork:frequent"

    // The largest tile there is, until the user picks another size.
    val DEFAULT = CategoryOptions(size = CategorySize.LARGE)

    fun isFrequent(id: String?): Boolean = id == ID

    // What the tile has stored, or its default while it was never placed.
    fun optionsOf(stored: CategoryOptions?): CategoryOptions = stored ?: DEFAULT

    // How many apps the tile has places for: its large icons and the small ones of its corner.
    fun capacity(options: CategoryOptions): Int {
        return TileSlots.of(options).size.coerceAtMost(FrequentConfig.MAX_SLOTS)
    }

    // The first time the category is turned on: the tile goes to the top as the largest size, and the tiles that
    // stood there make way, going down. Every tile gets a cell of its own, so nothing moves by itself afterwards.
    // [existing] are the tiles shown now, with the cells they were left in.
    fun firstPlacement(existing: List<Entry>): Map<String, TileChange> {
        val size = CellFlow.footprint(CategorySize.LARGE)
        val placed = CellGrid.arrange(existing) + Placed(ID, 0, 0, size.w, size.h)
        return CellGrid.drop(placed, ID, 0, 0).associate { cell ->
            cell.id to TileChange(CategorySize.LARGE.takeIf { cell.id == ID }, cell.col, cell.row)
        }
    }

    // The folders' order the tiles have in these cells, for the other drawer styles.
    fun orderAfter(changes: Map<String, TileChange>): List<String> =
        changes.entries.sortedWith(compareBy({ it.value.row }, { it.value.col })).map { it.key }
}
