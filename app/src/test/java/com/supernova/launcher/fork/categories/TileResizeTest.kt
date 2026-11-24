package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A corner dragged takes the size whose footprint is nearest to where it is let go, across and down.
class TileResizeTest {

    // Spans of 100: small 90 square, normal 190 square, double 390 by 190, tall 190 by 390, big 390 square.
    private fun size(width: Float, height: Float) = TileResize.sizeFor(width, height, 90f, 190f, 390f)

    @Test
    fun theExactFootprintsGiveTheirSizes() {
        assertEquals(CategorySize.SMALL, size(90f, 90f))
        assertEquals(CategorySize.NORMAL, size(190f, 190f))
        assertEquals(CategorySize.WIDE, size(390f, 190f))
        assertEquals(CategorySize.TALL, size(190f, 390f))
        assertEquals(CategorySize.LARGE, size(390f, 390f))
    }

    @Test
    fun draggingAcrossMakesADoubleTile() {
        assertEquals(CategorySize.WIDE, size(330f, 190f))
    }

    @Test
    fun draggingDownAsWellMakesABigTile() {
        assertEquals(CategorySize.LARGE, size(390f, 330f))
    }

    @Test
    fun draggingDownFromANormalTileMakesATallOne() {
        assertEquals(CategorySize.TALL, size(190f, 330f))
    }

    @Test
    fun aLittleDownFromANormalTileStaysNormal() {
        assertEquals(CategorySize.NORMAL, size(190f, 240f))
    }

    @Test
    fun draggingAcrossFromATallTileMakesABigOne() {
        assertEquals(CategorySize.LARGE, size(330f, 390f))
    }

    @Test
    fun draggingBackUpFromABigTileMakesItDoubleAgain() {
        assertEquals(CategorySize.WIDE, size(390f, 220f))
    }

    @Test
    fun beyondTheEndsHolds() {
        assertEquals(CategorySize.SMALL, size(10f, 10f))
        assertEquals(CategorySize.LARGE, size(900f, 900f))
    }

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
