package com.supernova.launcher.fork.categories

// Small tiles go two to a column: the space of one normal tile holds four small
// ones, two across and two down, so the two beside a normal tile are a pair
// stood one over the other instead of leaving a gap under them. Plain, so it
// can be tested with strings.
internal object TilePairs {

    // The items with each two small ones that follow each other made one by [pair]. A small one
    // left over, or one beside a tile of another size, stays as it is, alone and not stretched.
    fun <T> group(items: List<T>, isSmall: (T) -> Boolean, pair: (T, T) -> T): List<T> {
        val out = ArrayList<T>(items.size)
        var at = 0
        while (at < items.size) {
            val first = items[at]
            val second = items.getOrNull(at + 1)
            if (second != null && isSmall(first) && isSmall(second)) {
                out.add(pair(first, second))
                at += 2
            } else {
                out.add(first)
                at += 1
            }
        }
        return out
    }

    // Small rows (pairs, or a lone small tile) fill two columns each, the space of a normal tile when two follow
    // each other. A run of them with an odd number of rows has one row left alone in its slot, and the tile after it
    // would stand in the middle of the grid, so a [gap] is put after that row to fill the other half of the slot.
    // The gap is made from the run's last row. No gap at the very end: nothing there needs it.
    fun <T> gaps(items: List<T>, isSmallRow: (T) -> Boolean, gap: (T) -> T): List<T> {
        val out = ArrayList<T>(items.size)
        var run = 0
        for (item in items) {
            if (isSmallRow(item)) {
                run += 1
            } else {
                if (run % 2 == 1) out.add(gap(out.last()))
                run = 0
            }
            out.add(item)
        }
        return out
    }

    // While a tile that is not small is dragged, the small ones stand in blocks: each run of small ones that
    // follow each other is cut into blocks of [BLOCK] (the last may hold fewer, even one) and each is made one item by
    // [block], so the dragged tile changes places with a whole block, the space of a normal tile, and the
    // small ones in it keep together.
    fun <T> blocks(items: List<T>, isSmall: (T) -> Boolean, block: (List<T>) -> T): List<T> {
        val out = ArrayList<T>(items.size)
        var at = 0
        while (at < items.size) {
            if (!isSmall(items[at])) {
                out.add(items[at])
                at += 1
                continue
            }
            var end = at
            while (end < items.size && end - at < BLOCK && isSmall(items[end])) end += 1
            out.add(block(items.subList(at, end)))
            at = end
        }
        return out
    }

    // The small tiles a block holds: four, the space of a normal tile.
    const val BLOCK = 4

    // The items with each pair taken apart into the two it holds ([parts] says which items are pairs).
    fun <T> split(items: List<T>, parts: (T) -> List<T>?): List<T> = items.flatMap { parts(it) ?: listOf(it) }
}
