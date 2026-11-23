package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// Tiles nobody has put anywhere stand as the lists before the mosaic set them.
class CellFlowTest {

    private fun e(id: String, size: CategorySize) = Entry(id, size)

    private fun three() = listOf(e("a", CategorySize.NORMAL), e("b", CategorySize.NORMAL), e("c", CategorySize.NORMAL))

    private fun at(out: List<Placed>, id: String) = out.first { it.id == id }.let { it.col to it.row }

    @Test
    fun twoNormalTilesStandSideBySideAndTheNextRowStartsBelow() {
        val out = CellFlow.legacy(three(), 4)
        assertEquals(0 to 0, at(out, "a"))
        assertEquals(2 to 0, at(out, "b"))
        assertEquals(0 to 2, at(out, "c"))
    }

    @Test
    fun twoSmallTilesInARowStandOneOverTheOther() {
        val out = CellFlow.legacy(listOf(e("a", CategorySize.SMALL), e("b", CategorySize.SMALL)), 4)
        assertEquals(0 to 0, at(out, "a"))
        assertEquals(0 to 1, at(out, "b"))
    }

    @Test
    fun aPairBesideANormalTileIsAsHighAsIt() {
        val entries = listOf(
            e("a", CategorySize.NORMAL), e("s1", CategorySize.SMALL), e("s2", CategorySize.SMALL),
            e("z", CategorySize.NORMAL)
        )
        val out = CellFlow.legacy(entries, 4)
        assertEquals(2 to 0, at(out, "s1"))
        assertEquals(2 to 1, at(out, "s2"))
        assertEquals(0 to 2, at(out, "z"))
    }

    @Test
    fun aLoneSmallTileStandsAtTheTopOfItsSpace() {
        val out = CellFlow.legacy(listOf(e("s", CategorySize.SMALL), e("n", CategorySize.NORMAL)), 4)
        assertEquals(0 to 0, at(out, "s"))
        assertEquals(1 to 0, at(out, "n"))
    }

    @Test
    fun aWideTileTakesTheWholeRow() {
        val out = CellFlow.legacy(listOf(e("n", CategorySize.NORMAL), e("w", CategorySize.WIDE)), 4)
        assertEquals(0 to 2, at(out, "w"))
    }

    @Test
    fun aTallTileMakesItsRowTallSoTheNextRowStartsBelowIt() {
        val entries = listOf(e("t", CategorySize.TALL), e("n", CategorySize.NORMAL), e("z", CategorySize.NORMAL))
        val out = CellFlow.legacy(entries, 4)
        assertEquals(2 to 0, at(out, "n"))
        assertEquals(0 to 4, at(out, "z"))
    }

    @Test
    fun aStripTakesTheRowAndOneRowOfHeight() {
        val out = CellFlow.legacy(listOf(e("s", CategorySize.STRIP), e("n", CategorySize.NORMAL)), 4)
        assertEquals(0 to 1, at(out, "n"))
    }

    @Test
    fun onSixColumnsThreeNormalTilesStandInOneRow() {
        val out = CellFlow.legacy(three(), 6)
        assertEquals(4 to 0, at(out, "c"))
    }
}
