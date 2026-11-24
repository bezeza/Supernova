package com.supernova.launcher.fork.categories

// Where the icons of a square tile go, as fractions of the card's inner
// square: a large icon fills its cell, a small one is a quarter of the card
// (or a ninth). Plain data, so each layout can be tested for room and overlap.
// One function for each layout a tile can have, so there are many.
@Suppress("TooManyFunctions")
internal object TileSlots {

    class Slot(val x: Float, val y: Float, val size: Float, val large: Boolean)

    private const val HALF = 0.5f
    private const val QUARTER = 0.25f
    private const val THIRD = 1f / 3f
    private const val CELLS = 4
    private const val LARGE_CELLS = 2
    private const val GRID_THREE = 3
    private const val GRID_TWO = 2
    const val SMALL_NAME = 0.22f
    private const val DOUBLE_COLUMNS = 4
    private const val DOUBLE_LARGE = 7
    private const val CORNER_X = 1.5f
    private const val BIG_LARGE = 15
    private const val BIG_CORNER = 0.75f
    private const val EIGHTH = 0.125f
    private const val TALL_CORNER_X = 0.25f

    // [large] 3 / 2 / 1 / 0 large icons, the rest of the card small icons.
    fun normal(large: Int): List<Slot> = when (large) {
        0 -> grid(THIRD, GRID_THREE, large = false)
        1 -> listOf(Slot(0f, 0f, HALF, true)) + miniCells(skip = { x, y -> x < LARGE_CELLS && y < LARGE_CELLS })
        2 -> listOf(Slot(0f, 0f, HALF, true), Slot(HALF, 0f, HALF, true)) +
            miniCells(skip = { _, y -> y < LARGE_CELLS })
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

    // A tall tile is two normal ones one over the other: two columns of four rows of cells of a quarter of its
    // height, the last cell a corner of four small icons, so seven large icons.
    fun tall(): List<Slot> = buildList {
        for (cell in 0 until DOUBLE_LARGE) {
            add(Slot((cell % GRID_TWO) * QUARTER, (cell / GRID_TWO) * QUARTER, QUARTER, true))
        }
        for (y in 0 until GRID_TWO) {
            for (x in 0 until GRID_TWO) add(Slot(TALL_CORNER_X + x * EIGHTH, BIG_CORNER + y * EIGHTH, EIGHTH, false))
        }
    }

    // A big tile is two double ones one over the other: a square of four by four cells of a quarter, the last
    // cell a corner of four small icons, so fifteen large icons.
    fun big(): List<Slot> = buildList {
        for (cell in 0 until BIG_LARGE) add(Slot((cell % CELLS) * QUARTER, (cell / CELLS) * QUARTER, QUARTER, true))
        for (y in 0 until GRID_TWO) {
            for (x in 0 until GRID_TWO) add(Slot(BIG_CORNER + x * EIGHTH, BIG_CORNER + y * EIGHTH, EIGHTH, false))
        }
    }

    // A strip is one row: [count] large icons across its whole width, each as large as the strip is high.
    fun strip(count: Int): List<Slot> = List(count) { Slot(it.toFloat(), 0f, 1f, true) }

    // How many heights of the card the icons run across: 1 for a square tile, 2 for a double one.
    fun contentWidth(slots: List<Slot>): Float = slots.maxOfOrNull { it.x + it.size } ?: 1f

    // A small tile keeps a strip along its top for its name, [SMALL_NAME] of its height, and under it is a 2 by 2 of
    // small icons.
    fun small(): List<Slot> = buildList {
        val cell = (1f - SMALL_NAME) / GRID_TWO
        for (y in 0 until GRID_TWO) {
            for (x in 0 until GRID_TWO) add(Slot(x * cell, SMALL_NAME + y * cell, cell, false))
        }
    }

    // The share of the card's height the icons leave free along its top, for the name inside the card.
    fun nameShare(slots: List<Slot>): Float = slots.minOfOrNull { it.y } ?: 0f

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
