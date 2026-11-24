package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// Where the rows of the Categories list lie: tiles in their cells, the other rows in order around them.
class MosaicRectsTest {

    private val width = 400
    private val unit = 100

    // A list laid out by position: [cells] has a cell for a tile and null for another row.
    @Suppress("LongParameterList")
    private fun layout(
        cells: List<Placed?>,
        spans: List<Int> = List(cells.size) { 4 },
        heights: List<Int> = List(cells.size) { 50 },
        columns: Int = 4,
        top: Int = 0,
        bottom: Int = 0
    ) = MosaicRects.compute(
        MosaicRects.Input(
            count = cells.size, cellAt = { cells[it] }, spanAt = { spans[it] }, heightAt = { heights[it] },
            width = width, columns = columns, unit = unit, top = top, bottom = bottom
        )
    )

    private fun p(col: Int, row: Int, w: Int, h: Int) = Placed("x", col, row, w, h)

    @Test
    fun aTileStandsInItsCell() {
        val out = layout(listOf(p(2, 1, 2, 2)))
        assertEquals(200, out.left(0))
        assertEquals(100, out.top(0))
        assertEquals(400, out.right(0))
        assertEquals(300, out.bottom(0))
    }

    @Test
    fun theListIsAsHighAsTheLowestTileReaches() {
        assertEquals(300, layout(listOf(p(0, 0, 2, 2), p(2, 1, 2, 2))).height)
    }

    @Test
    fun tilesSideBySideShareARow() {
        val out = layout(listOf(p(0, 0, 2, 2), p(2, 0, 2, 2)))
        assertEquals(out.top(0), out.top(1))
        assertEquals(out.right(0), out.left(1))
    }

    @Test
    fun aHoleIsLeftWhereNoTileStands() {
        val out = layout(listOf(p(0, 0, 1, 1), p(3, 0, 1, 1)))
        assertEquals(100, out.right(0))
        assertEquals(300, out.left(1))
    }

    @Test
    fun rowsBeforeTheFirstTileStandOverTheMosaic() {
        val out = layout(listOf(null, p(0, 0, 2, 2)), heights = listOf(80, 0))
        assertEquals(0, out.top(0))
        assertEquals(80, out.bottom(0))
        assertEquals(80, out.top(1))
    }

    @Test
    fun rowsAfterTheLastTileStandUnderTheMosaicAndPackAcrossTheirSpans() {
        val cells = listOf(p(0, 0, 4, 2), null, null, null, null, null)
        val spans = listOf(4, 4, 1, 1, 1, 1)
        val heights = listOf(0, 30, 60, 60, 60, 60)
        val out = layout(cells, spans, heights)
        assertEquals(200, out.top(1))
        assertEquals(230, out.top(2))
        assertEquals(230, out.top(5))
        assertEquals(300, out.left(5))
        assertEquals(290, out.height)
    }

    @Test
    fun aRowThatDoesNotFitBesideTheLastStartsTheNextUnderTheHighest() {
        val out = layout(listOf(null, null, null), listOf(3, 2, 1), listOf(10, 40, 20))
        assertEquals(0, out.top(0))
        assertEquals(10, out.top(1))
        assertEquals(10, out.top(2))
        assertEquals(200, out.left(2))
        assertEquals(0, out.left(1))
    }

    @Test
    fun withoutTilesAllRowsFlow() {
        val out = layout(listOf(null, null), heights = listOf(40, 40))
        assertEquals(40, out.top(1))
        assertEquals(80, out.height)
    }

    @Test
    fun paddingMovesTheContentAndAddsToTheHeight() {
        val out = layout(listOf(p(0, 0, 2, 2)), top = 10, bottom = 20)
        assertEquals(10, out.top(0))
        assertEquals(230, out.height)
    }

    @Test
    fun onSixColumnsTheColumnsAreNarrower() {
        val out = layout(listOf(p(4, 0, 2, 2)), columns = 6)
        assertEquals(400 * 4 / 6, out.left(0))
        assertEquals(400, out.right(0))
    }

    @Test
    fun noRowsNoHeight() {
        assertEquals(0, layout(emptyList()).height)
    }
}
