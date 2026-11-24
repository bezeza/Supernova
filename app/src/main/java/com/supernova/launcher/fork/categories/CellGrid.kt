package com.supernova.launcher.fork.categories

// What a tile covers in the mosaic: [w] columns across, [h] rows down. A normal tile is 2 by 2, so a small
// one (1 by 1) is a quarter of it and two small ones one over the other are as high as a normal tile.
internal class Footprint(val w: Int, val h: Int)

// A tile placed in the mosaic: its cell, the top left one, and what it covers.
internal data class Placed(val id: String, val col: Int, val row: Int, val w: Int, val h: Int) {
    val right: Int get() = col + w
    val bottom: Int get() = row + h
}

// A tile to be placed: its size, and the cell it was left in, if it was ever put in one.
internal class Entry(val id: String, val size: CategorySize, val col: Int? = null, val row: Int? = null)

// The mosaic's rules, plain arithmetic so they can be tested without a view. The grid has [COLUMNS] columns
// and as many rows as the tiles need; a tile may stand anywhere, so there can be holes. A tile put down on others
// moves them: they go down, below it, and what they then cover goes down in turn. Nothing overlaps.
internal object CellGrid {

    const val COLUMNS = 4

    // The small tiles of a block: a square of two by two.
    private const val BLOCK = 4

    fun overlaps(a: Placed, b: Placed): Boolean =
        a.col < b.right && b.col < a.right && a.row < b.bottom && b.row < a.bottom

    // [moved] put down, the others in [items] pushed down out of its way, each below what is in its way.
    private fun settle(moved: List<Placed>, rest: List<Placed>): List<Placed> {
        val out = ArrayList<Placed>(moved)
        for (item in rest.sortedWith(compareBy({ it.row }, { it.col }))) {
            var at = item
            while (true) {
                val hit = out.firstOrNull { overlaps(at, it) } ?: break
                at = at.copy(row = hit.bottom)
            }
            out.add(at)
        }
        return out.sortedWith(compareBy({ it.row }, { it.col }))
    }

    // Tile [id] dropped with its top left corner in ([col], [row]): kept inside the grid, the others make way.
    fun drop(items: List<Placed>, id: String, col: Int, row: Int, columns: Int = COLUMNS): List<Placed> {
        val tile = items.firstOrNull { it.id == id } ?: return items
        val moved = tile.copy(col = col.coerceIn(0, columns - tile.w), row = row.coerceAtLeast(0))
        return settle(listOf(moved), items.filter { it.id != id })
    }

    // The four small tiles that stand as a square of 2 by 2 cells with tile [id] among them, in reading order; empty
    // when [id] is no small tile or is in no full square. Such a block is what a drag takes as one.
    fun block(items: List<Placed>, id: String): List<Placed> {
        val tile = items.firstOrNull { it.id == id && it.w == 1 && it.h == 1 } ?: return emptyList()
        val small = items.filter { it.w == 1 && it.h == 1 }
        val squares = (tile.row - 1..tile.row).flatMap { top ->
            (tile.col - 1..tile.col).filter { it >= 0 && top >= 0 }.map { left ->
                small.filter { it.col in left..left + 1 && it.row in top..top + 1 }
            }
        }
        return squares.firstOrNull { it.size == BLOCK }?.sortedWith(compareBy({ it.row }, { it.col })).orEmpty()
    }

    // The tiles [ids] moved together by ([dCol], [dRow]) cells, the group kept inside the grid and the others
    // pushed down out of its way.
    fun dropGroup(items: List<Placed>, ids: Set<String>, dCol: Int, dRow: Int, columns: Int = COLUMNS): List<Placed> {
        val group = items.filter { it.id in ids }
        if (group.isEmpty()) return items
        val shiftCol = dCol.coerceIn(-group.minOf { it.col }, columns - group.maxOf { it.right })
        val shiftRow = dRow.coerceAtLeast(-group.minOf { it.row })
        val moved = group.map { it.copy(col = it.col + shiftCol, row = it.row + shiftRow) }
        return settle(moved, items.filter { it.id !in ids })
    }

    // Tile [id] given another size, in its own cell where it still fits, else pulled left so it does.
    fun resize(items: List<Placed>, id: String, size: Footprint, columns: Int = COLUMNS): List<Placed> {
        val tile = items.firstOrNull { it.id == id } ?: return items
        val grown = tile.copy(col = tile.col.coerceIn(0, columns - size.w), w = size.w, h = size.h)
        return settle(listOf(grown), items.filter { it.id != id })
    }

    // Every tile pulled up as far as it goes in its own columns, so the holes above tiles close.
    fun compact(items: List<Placed>): List<Placed> {
        val out = ArrayList<Placed>(items.size)
        for (item in items.sortedWith(compareBy({ it.row }, { it.col }))) {
            var at = item.copy(row = 0)
            while (true) {
                val hit = out.firstOrNull { overlaps(at, it) } ?: break
                at = at.copy(row = hit.bottom)
            }
            out.add(at)
        }
        return out.sortedWith(compareBy({ it.row }, { it.col }))
    }

    // The first free cell, row after row and left to right, that a tile of [size] fits in.
    fun firstFree(items: List<Placed>, size: Footprint, columns: Int = COLUMNS): Pair<Int, Int> {
        var row = 0
        while (true) {
            for (col in 0..(columns - size.w).coerceAtLeast(0)) {
                val probe = Placed("", col, row, size.w, size.h)
                if (items.none { overlaps(probe, it) }) return col to row
            }
            row += 1
        }
    }

    // The ids in reading order: row after row, left to right. It is the order the folders are stored in.
    fun order(items: List<Placed>): List<String> =
        items.sortedWith(compareBy({ it.row }, { it.col })).map { it.id }

    // The tiles placed: in their own cells when the grid is the usual one, in reading order otherwise (a wide
    // screen has more columns, so a cell kept for four of them means nothing there). A tile never put in a cell
    // takes the first free one; with no cells kept at all, the tiles stand as the lists before the mosaic set them.
    fun arrange(entries: List<Entry>, columns: Int = COLUMNS): List<Placed> {
        val kept = entries.filter { it.col != null && it.row != null }
        if (kept.isEmpty() || columns != COLUMNS) {
            val ordered = if (kept.isEmpty()) {
                entries
            } else {
                entries.sortedWith(compareBy({ it.row ?: Int.MAX_VALUE }, { it.col ?: 0 }))
            }
            return CellFlow.legacy(ordered, columns)
        }
        val fixed = kept.map {
            val f = CellFlow.footprint(it.size)
            Placed(it.id, it.col!!.coerceIn(0, columns - f.w), it.row!!.coerceAtLeast(0), f.w, f.h)
        }
        val settled = settle(emptyList(), fixed)
        return entries.filter { it.col == null || it.row == null }.fold(settled) { placed, entry ->
            val f = CellFlow.footprint(entry.size)
            val (col, row) = firstFree(placed, f, columns)
            placed + Placed(entry.id, col, row, f.w, f.h)
        }.sortedWith(compareBy({ it.row }, { it.col }))
    }
}
