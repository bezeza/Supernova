package com.supernova.launcher.fork.categories

// Where the icons of a square tile go, as fractions of the card's inner
// square: a large icon fills its cell, a small one is a quarter of the card
// (or a ninth). Plain data, so each layout can be tested for room and overlap.
internal object TileSlots {

    class Slot(val x: Float, val y: Float, val size: Float, val large: Boolean)

    private const val HALF = 0.5f
    private const val QUARTER = 0.25f
    private const val THIRD = 1f / 3f
    private const val CELLS = 4
    private const val LARGE_CELLS = 2
    private const val GRID_THREE = 3
    private const val GRID_TWO = 2
    private const val DOUBLE_COLUMNS = 4
    private const val DOUBLE_LARGE = 7
    private const val CORNER_X = 1.5f

    // [large] 3 / 1 / 0 large icons, the rest of the card small icons.
    fun normal(large: Int): List<Slot> = when (large) {
        0 -> grid(THIRD, GRID_THREE, large = false)
        1 -> listOf(Slot(0f, 0f, HALF, true)) + miniCells(skip = { x, y -> x < LARGE_CELLS && y < LARGE_CELLS })
        else -> listOf(Slot(0f, 0f, HALF, true), Slot(HALF, 0f, HALF, true), Slot(0f, HALF, HALF, true)) +
            miniCells(skip = { x, y -> !(x >= LARGE_CELLS && y >= LARGE_CELLS) })
    }

    // A double tile is two tiles' width and one's height, in units of its height: two rows of four cells of a
    // half, the last cell a corner of four small icons, so seven large icons.
    fun double(): List<Slot> = buildList {
        for (cell in 0 until DOUBLE_LARGE) {
            add(Slot((cell % DOUBLE_COLUMNS) * HALF, (cell / DOUBLE_COLUMNS) * HALF, HALF, true))
        }
        for (y in 0 until GRID_TWO) {
            for (x in 0 until GRID_TWO) add(Slot(CORNER_X + x * QUARTER, HALF + y * QUARTER, QUARTER, false))
        }
    }

    // How many heights of the card the icons run across: 1 for a square tile, 2 for a double one.
    fun contentWidth(slots: List<Slot>): Float = slots.maxOfOrNull { it.x + it.size } ?: 1f

    // A small tile is a 2 by 2 of small icons.
    fun small(): List<Slot> = grid(HALF, GRID_TWO, large = false)

    fun largeCount(slots: List<Slot>): Int = slots.count { it.large }

    fun miniCount(slots: List<Slot>): Int = slots.count { !it.large }

    // The quarter cells of the card, row by row, but those [skip] says are taken.
    private fun miniCells(skip: (Int, Int) -> Boolean): List<Slot> = buildList {
        for (y in 0 until CELLS) {
            for (x in 0 until CELLS) {
                if (!skip(x, y)) add(Slot(x * QUARTER, y * QUARTER, QUARTER, false))
            }
        }
    }

    private fun grid(size: Float, count: Int, large: Boolean): List<Slot> = buildList {
        for (y in 0 until count) {
            for (x in 0 until count) add(Slot(x * size, y * size, size, large))
        }
    }
}
