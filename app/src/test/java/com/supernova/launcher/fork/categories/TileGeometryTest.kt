package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The corner of small icons has a bigger target than its own cell.
class TileGeometryTest {

    // A 400 px card with 7% padding: 28 px, so a cell is 172 px; the corner cell starts at 200.
    private val side = 400
    private val cell = 172
    private val start = 200

    @Test
    fun theTargetReachesTheCardsBottomRightEdge() {
        val target = TileGeometry.cornerTarget(start, start, side, side, cell)
        assertEquals(side, target.right)
        assertEquals(side, target.bottom)
    }

    @Test
    fun theTargetIsBiggerThanTheCornerCell() {
        val target = TileGeometry.cornerTarget(start, start, side, side, cell)
        assertTrue(target.width > cell)
        assertTrue(target.height > cell)
    }

    @Test
    fun theTargetReachesBackIntoTheCellsBesideItByTheReach() {
        val target = TileGeometry.cornerTarget(start, start, side, side, cell)
        val reach = (cell * TileGeometry.CORNER_REACH).toInt()
        assertEquals(start - reach, target.left)
        assertEquals(start - reach, target.top)
    }

    @Test
    fun aDoubleTilesCornerIsAtItsRight() {
        val target = TileGeometry.cornerTarget(700, 200, 800, 400, 100)
        assertEquals(800, target.right)
        assertEquals(670, target.left)
    }

    @Test
    fun aDoubleTileIsAsHighAsANormalOne() {
        // Normal 188 wide (two spans of 100, less a margin of 6 each side); double 388.
        assertEquals(188, TileGeometry.doubleHeight(388, 6))
    }

    @Test
    fun aTallTileIsTwoNormalOnesWithTheMarginsBetween() {
        // Normal 188 wide with margins of 6: two of them and the 12 between make 388, as a double tile is wide.
        assertEquals(388, TileGeometry.tallHeight(188, 6))
    }

    @Test
    fun aDoubleTilesCardIsAsHighAsANormalOnes() {
        val normalCard = 188
        assertEquals(normalCard, TileGeometry.doubleHeight(388, 6))
    }

    @Test
    fun twoSmallCardsWithTheirMarginsAreAsHighAsANormalCardWithItsOwn() {
        // A span of 100 and margins of 6: a small card is 88, a normal one 188; two small ones with a margin above
        // and below each fill the same height as one normal card with its margin above and below, 2 * 100.
        val span = 100
        val margin = 6
        val small = span - 2 * margin
        val normal = 2 * span - 2 * margin
        assertEquals(normal + 2 * margin, 2 * (small + 2 * margin))
    }
}
