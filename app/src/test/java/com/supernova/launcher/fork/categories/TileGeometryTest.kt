package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The corner of small icons has a bigger target than its own cell.
class TileGeometryTest {

    // A 400 px card with 7% padding: 28 px, so a cell is 172 px.
    private val side = 400
    private val pad = 28
    private val cell = 172

    @Test
    fun theTargetReachesTheCardsBottomRightEdge() {
        val target = TileGeometry.cornerTarget(side, pad, cell)
        assertEquals(side, target.right)
        assertEquals(side, target.bottom)
    }

    @Test
    fun theTargetIsBiggerThanTheCornerCell() {
        val target = TileGeometry.cornerTarget(side, pad, cell)
        assertTrue(target.width > cell)
        assertTrue(target.height > cell)
    }

    @Test
    fun theTargetReachesBackIntoTheCellsBesideItByTheReach() {
        val target = TileGeometry.cornerTarget(side, pad, cell)
        val reach = (cell * TileGeometry.CORNER_REACH).toInt()
        assertEquals(pad + cell - reach, target.left)
        assertEquals(pad + cell - reach, target.top)
    }

    @Test
    fun theTargetStaysInsideTheCard() {
        val target = TileGeometry.cornerTarget(side, pad, cell)
        assertTrue(target.left >= 0 && target.top >= 0)
    }
}
