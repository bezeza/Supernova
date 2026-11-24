package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A corner dragged takes the size whose footprint is nearest to where it is let go, across and down.
class TileResizeTest {

    @Test
    fun aCornerAtTheSameFootprintKeepsTheSize() {
        assertEquals(CategorySize.NORMAL, TileResize.sizeForCells(2f, 2f))
        assertEquals(CategorySize.SMALL, TileResize.sizeForCells(1f, 1f))
        assertEquals(CategorySize.LARGE, TileResize.sizeForCells(4f, 4f))
    }

    @Test
    fun aCornerDraggedWideSnapsToAWideTileOrAStrip() {
        assertEquals(CategorySize.WIDE, TileResize.sizeForCells(4f, 2f))
        assertEquals(CategorySize.STRIP, TileResize.sizeForCells(4f, 1f))
        assertEquals(CategorySize.STRIP, TileResize.sizeForCells(3.6f, 1.2f))
    }

    @Test
    fun aCornerDraggedToTwoByOneSnapsToAHalfTile() {
        assertEquals(CategorySize.HALF, TileResize.sizeForCells(2f, 1f))
        assertEquals(CategorySize.HALF, TileResize.sizeForCells(2.4f, 1.3f))
        assertEquals(CategorySize.SMALL, TileResize.sizeForCells(1.2f, 1f))
        assertEquals(CategorySize.NORMAL, TileResize.sizeForCells(2f, 1.8f))
    }

    @Test
    fun aCornerDraggedDownSnapsToATallTile() {
        assertEquals(CategorySize.TALL, TileResize.sizeForCells(2f, 4f))
        assertEquals(CategorySize.TALL, TileResize.sizeForCells(2.3f, 3.4f))
    }

    @Test
    fun aCornerBetweenTwoFootprintsTakesTheNearer() {
        // Halfway between one and two cells is 1.5: past it the corner has taken the tile to a normal one.
        assertEquals(CategorySize.NORMAL, TileResize.sizeForCells(1.6f, 1.6f))
        assertEquals(CategorySize.SMALL, TileResize.sizeForCells(1.4f, 1.4f))
    }
}
