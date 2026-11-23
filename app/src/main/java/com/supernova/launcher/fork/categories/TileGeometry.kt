package com.supernova.launcher.fork.categories

// Where a tile's tap targets lie, in pixels of the square card. Plain
// arithmetic, so it is testable without a view.
internal object TileGeometry {

    // How far past its own cell the corner's target reaches into the cells of
    // the large icons beside it, as a share of a cell: a thumb that aims at
    // the corner of small icons lands on it, not on a large icon by mistake.
    const val CORNER_REACH = 0.3f

    class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    // The corner of small icons starts at ([left], [top]) and the card ends at ([right], [bottom]). Its target
    // runs from [CORNER_REACH] of a [cell] before the corner to the card's own edge, padding included.
    fun cornerTarget(left: Int, top: Int, right: Int, bottom: Int, cell: Int): Rect {
        val reach = (cell * CORNER_REACH).toInt()
        return Rect(left - reach, top - reach, right, bottom)
    }

    // A double tile is two normal ones wide with the margin between them, and as high as one:
    // the width less the margin that fell between, halved.
    fun doubleHeight(width: Int, margin: Int): Int = width / 2 - margin

    // A tall tile is two normal ones one over the other with the margins between them: twice the width and
    // the margins that fell between.
    fun tallHeight(width: Int, margin: Int): Int = 2 * width + 2 * margin
}
