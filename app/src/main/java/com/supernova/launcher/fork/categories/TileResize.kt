package com.supernova.launcher.fork.categories

import kotlin.math.roundToInt

// What dragging a tile's corner does to its size: one step to the right of the
// span of a normal tile for each [unit] dragged, to the left a step back, over
// SMALL, NORMAL and WIDE. Plain arithmetic, so it is testable without a view.
internal object TileResize {

    private val ORDER = listOf(CategorySize.SMALL, CategorySize.NORMAL, CategorySize.WIDE)

    // [dx] is how far the finger moved right (left is negative), [unit] how far is one step.
    fun resize(current: CategorySize, dx: Float, unit: Float): CategorySize {
        if (unit <= 0f) return current
        val steps = (dx / unit).roundToInt()
        val at = ORDER.indexOf(current) + steps
        return ORDER[at.coerceIn(0, ORDER.lastIndex)]
    }
}
