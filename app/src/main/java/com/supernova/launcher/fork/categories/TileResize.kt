package com.supernova.launcher.fork.categories

import kotlin.math.hypot

// What dragging a tile's corner does to its size: the size whose footprint is
// nearest to where the finger has taken the tile's corner, across and down.
// Plain arithmetic, so it is testable without a view.
internal object TileResize {

    // [width] and [height] are how far the corner is dragged. [small] and [normal] are the sides of a small and a
    // normal tile (both square), [wide] the width of a double tile and the height of a tall one, the side of a big
    // one: as long as two normal tiles with the gap between them are. A double tile is [wide] by [normal], a tall
    // one [normal] by [wide], a big one [wide] by [wide].
    fun sizeFor(width: Float, height: Float, small: Float, normal: Float, wide: Float): CategorySize =
        listOf(
            CategorySize.SMALL to (small to small),
            CategorySize.NORMAL to (normal to normal),
            CategorySize.WIDE to (wide to normal),
            CategorySize.TALL to (normal to wide),
            CategorySize.LARGE to (wide to wide)
        ).minByOrNull { (_, footprint) -> hypot(footprint.first - width, footprint.second - height) }?.first
            ?: CategorySize.NORMAL
}
