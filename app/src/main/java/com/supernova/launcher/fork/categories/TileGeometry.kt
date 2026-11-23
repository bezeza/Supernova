package com.supernova.launcher.fork.categories

// Where a tile's tap targets lie, in pixels of the square card. Plain
// arithmetic, so it is testable without a view.
internal object TileGeometry {

    // How far past its own cell the corner's target reaches into the cells of
    // the large icons beside it, as a share of a cell: a thumb that aims at
    // the corner of small icons lands on it, not on a large icon by mistake.
    const val CORNER_REACH = 0.3f

    // A wide tile is a strip this high.
    const val WIDE_HEIGHT_DP = 76f

    class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    // The corner cell is the card's bottom right one, inside [pad] of padding.
    // Its target runs from [CORNER_REACH] of a cell before the cell to the
    // card's own edge, padding included.
    fun cornerTarget(side: Int, pad: Int, cell: Int): Rect {
        val reach = (cell * CORNER_REACH).toInt()
        val start = pad + cell
        return Rect(start - reach, start - reach, side, side)
    }

    // The outline that follows a corner being dragged: [width] wide, square up to a normal tile, then
    // growing flatter to the strip of a wide one. Plain arithmetic, so it is testable without a view.
    fun outlineHeight(width: Float, normal: Float, wide: Float, wideHeight: Float): Float {
        if (width <= normal || wide <= normal) return width.coerceAtMost(normal)
        val share = ((width - normal) / (wide - normal)).coerceIn(0f, 1f)
        return normal + (wideHeight - normal) * share
    }
}
