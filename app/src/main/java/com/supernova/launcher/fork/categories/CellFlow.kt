package com.supernova.launcher.fork.categories

// How tiles stand when nobody has put them anywhere: in reading order, row after row, as the lists did before the
// mosaic. Two small tiles in a row stand one over the other, as the pairs did.
internal object CellFlow {

    // The two sizes tiles come in across and down: a normal tile's side, and the whole usual grid's.
    private const val HALF = 2
    private const val FULL = CellGrid.COLUMNS

    fun footprint(size: CategorySize): Footprint = when (size) {
        CategorySize.SMALL -> Footprint(1, 1)
        CategorySize.NORMAL -> Footprint(HALF, HALF)
        CategorySize.WIDE -> Footprint(FULL, HALF)
        CategorySize.TALL -> Footprint(HALF, FULL)
        CategorySize.LARGE -> Footprint(FULL, FULL)
        CategorySize.STRIP -> Footprint(FULL, 1)
    }

    // A unit is what stands in a row as one: a tile, or two small tiles one over the other.
    private class Unit(val parts: List<Entry>, val w: Int, val h: Int)

    private fun units(entries: List<Entry>): List<Unit> {
        val out = ArrayList<Unit>()
        var at = 0
        while (at < entries.size) {
            val first = entries[at]
            val second = entries.getOrNull(at + 1)
            if (first.size == CategorySize.SMALL && second?.size == CategorySize.SMALL) {
                out.add(Unit(listOf(first, second), 1, 2))
                at += 2
            } else {
                val f = footprint(first.size)
                out.add(Unit(listOf(first), f.w, f.h))
                at += 1
            }
        }
        return out
    }

    // The tiles in [entries] placed across [columns] columns, a row of units at a time: a unit that does not fit
    // beside the one before it starts the next row, which begins under the tallest of the row above.
    fun legacy(entries: List<Entry>, columns: Int): List<Placed> {
        val out = ArrayList<Placed>(entries.size)
        var col = 0
        var rowTop = 0
        var rowHeight = 0
        for (unit in units(entries)) {
            val width = minOf(unit.w, columns)
            if (col + width > columns) {
                rowTop += rowHeight
                col = 0
                rowHeight = 0
            }
            unit.parts.forEachIndexed { index, part ->
                val f = footprint(part.size)
                out.add(Placed(part.id, col, rowTop + index * f.h, minOf(f.w, columns), f.h))
            }
            col += width
            rowHeight = maxOf(rowHeight, unit.h)
        }
        return out
    }
}
