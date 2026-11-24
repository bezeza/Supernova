package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Which tiles a drag takes along: a block of four small ones, or one tile.
class DragGroupTest {

    private fun p(id: String, col: Int, row: Int, w: Int = 1, h: Int = 1) = Placed(id, col, row, w, h)

    private fun square(prefix: String, col: Int, row: Int) = listOf(
        p("${prefix}1", col, row), p("${prefix}2", col + 1, row),
        p("${prefix}3", col, row + 1), p("${prefix}4", col + 1, row + 1)
    )

    @Test
    fun aDragTakesTheWholeBlockUnlessTheTilesAreBeingArranged() {
        val items = square("s", 2, 0) + p("n", 0, 0, 2, 2)
        assertEquals(listOf("s1", "s2", "s3", "s4"), DragGroup.of(items, "s3", arranging = false))
        assertEquals(listOf("s3"), DragGroup.of(items, "s3", arranging = true))
    }

    @Test
    fun aTileOutsideABlockIsDraggedAloneWhateverTheMode() {
        val items = square("s", 2, 0) + p("n", 0, 0, 2, 2)
        assertEquals(listOf("n"), DragGroup.of(items, "n", arranging = false))
        assertEquals(listOf("n"), DragGroup.of(items, "n", arranging = true))
    }

    @Test
    fun aSmallTileTakenOutOfABlockIsNoBlockAnyMore() {
        val items = square("s", 0, 0)
        val moved = CellGrid.drop(items, "s4", 2, 0)
        assertTrue(CellGrid.block(moved, "s1").isEmpty())
        assertEquals(listOf("s1"), DragGroup.of(moved, "s1", arranging = false))
    }
}
