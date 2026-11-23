package com.supernova.launcher.fork.categories

import kotlin.math.abs

// What dragging a tile's corner does to its size: the size whose width is
// nearest to where the finger has taken the tile's edge. Plain arithmetic, so
// it is testable without a view.
internal object TileResize {

    // [width] is how wide the tile is dragged; the three are the widths of a small, a normal and a wide tile.
    fun sizeFor(width: Float, small: Float, normal: Float, wide: Float): CategorySize =
        listOf(CategorySize.SMALL to small, CategorySize.NORMAL to normal, CategorySize.WIDE to wide)
            .minByOrNull { abs(it.second - width) }?.first ?: CategorySize.NORMAL
}
