package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The mosaic's rules: where tiles stand, what a drop and a resize do to the others, holes, compacting.
class CellGridTest {

    private fun p(id: String, col: Int, row: Int, w: Int = 1, h: Int = 1) = Placed(id, col, row, w, h)

    private fun noOverlaps(items: List<Placed>) {
        for (i in items.indices) for (j in i + 1 until items.size) {
            assertFalse("${items[i]} overlaps ${items[j]}", CellGrid.overlaps(items[i], items[j]))
        }
    }

    @Test
    fun tilesSideBySideDoNotOverlapAndOnesOnTopOfEachOtherDo() {
        assertFalse(CellGrid.overlaps(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2)))
        assertFalse(CellGrid.overlaps(p("a", 0, 0, 2, 2), p("b", 0, 2, 2, 2)))
        assertTrue(CellGrid.overlaps(p("a", 0, 0, 2, 2), p("b", 1, 1, 2, 2)))
    }

    @Test
    fun aTileDroppedOnFreeCellsMovesAndNothingElseDoes() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2))
        val out = CellGrid.drop(items, "b", 2, 4)
        assertEquals(p("b", 2, 4, 2, 2), out.first { it.id == "b" })
        assertEquals(p("a", 0, 0, 2, 2), out.first { it.id == "a" })
    }

    @Test
    fun aTileDroppedOnAnotherPushesItDown() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2), p("c", 0, 2, 2, 2))
        val out = CellGrid.drop(items, "c", 0, 0)
        assertEquals(p("c", 0, 0, 2, 2), out.first { it.id == "c" })
        assertEquals(p("a", 0, 2, 2, 2), out.first { it.id == "a" })
        assertEquals(p("b", 2, 0, 2, 2), out.first { it.id == "b" })
        noOverlaps(out)
    }

    @Test
    fun theTilesPushedDownPushOthersDownInTurn() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 0, 2, 2, 2), p("c", 0, 4, 2, 2), p("d", 2, 0, 2, 2))
        val out = CellGrid.drop(items, "d", 0, 0)
        assertEquals(listOf(0, 2, 4, 6), listOf("d", "a", "b", "c").map { id -> out.first { it.id == id }.row })
        noOverlaps(out)
    }

    @Test
    fun aDroppedTileIsKeptInsideTheGrid() {
        val items = listOf(p("a", 0, 0, 2, 2))
        assertEquals(2, CellGrid.drop(items, "a", 3, 0).first().col)
        assertEquals(0, CellGrid.drop(items, "a", -5, -3).first().let { it.col + it.row })
    }

    @Test
    fun aWideTileDroppedInTheRightColumnIsPulledLeftToFit() {
        val items = listOf(p("w", 0, 0, 4, 2))
        assertEquals(0, CellGrid.drop(items, "w", 3, 1).first().col)
    }

    @Test
    fun droppingAnUnknownTileChangesNothing() {
        val items = listOf(p("a", 0, 0))
        assertEquals(items, CellGrid.drop(items, "zzz", 1, 1))
    }

    @Test
    fun aTileMadeBiggerPushesTheOnesItNowCoversDown() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2), p("c", 0, 2, 2, 2))
        val out = CellGrid.resize(items, "a", Footprint(4, 2))
        assertEquals(p("a", 0, 0, 4, 2), out.first { it.id == "a" })
        assertEquals(p("b", 2, 2, 2, 2), out.first { it.id == "b" })
        // c is not under what a covers now, and b went down beside it, not onto it.
        assertEquals(p("c", 0, 2, 2, 2), out.first { it.id == "c" })
        noOverlaps(out)
    }

    @Test
    fun aTileMadeSmallerLeavesAHoleAndNothingMoves() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2), p("c", 0, 2, 2, 2))
        val out = CellGrid.resize(items, "a", Footprint(1, 1))
        assertEquals(p("a", 0, 0, 1, 1), out.first { it.id == "a" })
        assertEquals(p("c", 0, 2, 2, 2), out.first { it.id == "c" })
    }

    @Test
    fun aTileMadeWideAtTheRightIsPulledLeft() {
        val out = CellGrid.resize(listOf(p("a", 2, 0, 2, 2)), "a", Footprint(4, 2))
        assertEquals(p("a", 0, 0, 4, 2), out.first())
    }

    @Test
    fun compactingPullsTilesUpInTheirColumnsAndClosesTheHoles() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 0, 5, 2, 2), p("c", 2, 3, 2, 2))
        val out = CellGrid.compact(items)
        assertEquals(p("b", 0, 2, 2, 2), out.first { it.id == "b" })
        assertEquals(p("c", 2, 0, 2, 2), out.first { it.id == "c" })
        noOverlaps(out)
    }

    @Test
    fun compactingWhatHasNoHolesChangesNothing() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 2, 0, 2, 2), p("c", 0, 2, 4, 2))
        assertEquals(items, CellGrid.compact(items))
    }

    @Test
    fun theFirstFreeCellIsFoundRowAfterRowLeftToRight() {
        val items = listOf(p("a", 0, 0, 2, 2), p("b", 3, 0))
        assertEquals(2 to 0, CellGrid.firstFree(items, Footprint(1, 1)))
        assertEquals(2 to 1, CellGrid.firstFree(items, Footprint(2, 2)))
        assertEquals(0 to 2, CellGrid.firstFree(items, Footprint(4, 1)))
    }

    @Test
    fun theOrderIsReadingOrder() {
        val items = listOf(p("c", 0, 2), p("b", 2, 0), p("a", 0, 0), p("d", 1, 2))
        assertEquals(listOf("a", "b", "c", "d"), CellGrid.order(items))
    }

    @Test
    fun tilesWithCellsStandInThemAndAnUnplacedOneTakesTheFirstFreeCell() {
        val entries = listOf(
            Entry("a", CategorySize.NORMAL, 0, 4),
            Entry("b", CategorySize.SMALL, 3, 0),
            Entry("n", CategorySize.NORMAL)
        )
        val out = CellGrid.arrange(entries)
        assertEquals(p("a", 0, 4, 2, 2), out.first { it.id == "a" })
        assertEquals(p("b", 3, 0), out.first { it.id == "b" })
        assertEquals(p("n", 0, 0, 2, 2), out.first { it.id == "n" })
        noOverlaps(out)
    }

    @Test
    fun cellsThatClashAreSettledNotKept() {
        val entries = listOf(Entry("a", CategorySize.NORMAL, 0, 0), Entry("b", CategorySize.NORMAL, 1, 1))
        noOverlaps(CellGrid.arrange(entries))
    }

    @Test
    fun cellsOutsideTheGridAreBroughtInside() {
        val out = CellGrid.arrange(listOf(Entry("a", CategorySize.WIDE, 3, 1)))
        assertEquals(p("a", 0, 1, 4, 2), out.first())
    }

    @Test
    fun onAWideScreenTheTilesStandInReadingOrderWhateverCellsWereKept() {
        val entries = listOf(
            Entry("b", CategorySize.NORMAL, 2, 0),
            Entry("a", CategorySize.NORMAL, 0, 0),
            Entry("c", CategorySize.NORMAL, 0, 2)
        )
        val out = CellGrid.arrange(entries, columns = 6)
        val cells = listOf("a", "b", "c").map { id -> out.first { it.id == id }.let { it.col to it.row } }
        assertEquals(listOf(0 to 0, 2 to 0, 4 to 0), cells)
    }
}
