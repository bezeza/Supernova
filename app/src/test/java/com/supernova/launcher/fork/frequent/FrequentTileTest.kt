package com.supernova.launcher.fork.frequent

import com.supernova.launcher.fork.categories.CategoryOptions
import com.supernova.launcher.fork.categories.CategoryOptionsStore
import com.supernova.launcher.fork.categories.CategorySize
import com.supernova.launcher.fork.categories.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The Frequent tile: its size, its places, and where it goes the first time.
class FrequentTileTest {

    private fun normals() = listOf(Entry("a", CategorySize.NORMAL), Entry("b", CategorySize.NORMAL))

    @Test
    fun theTileIsTheLargestOneByDefault() {
        assertEquals(CategorySize.LARGE, FrequentTile.optionsOf(null).size)
    }

    @Test
    fun aStoredSizeBeatsTheDefault() {
        val own = CategoryOptions(size = CategorySize.WIDE)
        assertEquals(CategorySize.WIDE, FrequentTile.optionsOf(own).size)
    }

    @Test
    fun theLargestTileHoldsNineteenApps() {
        assertEquals(FrequentConfig.MAX_SLOTS, FrequentTile.capacity(FrequentTile.DEFAULT))
    }

    @Test
    fun aSmallerTileHoldsFewer() {
        assertTrue(FrequentTile.capacity(CategoryOptions(size = CategorySize.WIDE)) < FrequentConfig.MAX_SLOTS)
    }

    @Test
    fun onlyItsOwnIdIsFrequent() {
        assertTrue(FrequentTile.isFrequent(FrequentTile.ID))
        assertFalse(FrequentTile.isFrequent("Ігри"))
        assertFalse(FrequentTile.isFrequent(null))
    }

    @Test
    fun theFirstPlacementPutsTheTileAtTheTopAsTheLargestSize() {
        val changes = FrequentTile.firstPlacement(normals())
        val own = changes.getValue(FrequentTile.ID)
        assertEquals(0, own.col)
        assertEquals(0, own.row)
        assertEquals(CategorySize.LARGE, own.size)
    }

    @Test
    fun theTilesThatStoodThereGoBelowAndKeepTheirSizes() {
        val changes = FrequentTile.firstPlacement(
            listOf(Entry("a", CategorySize.NORMAL, 0, 0), Entry("b", CategorySize.NORMAL, 2, 0))
        )
        assertTrue(changes.getValue("a").row >= 4)
        assertTrue(changes.getValue("b").row >= 4)
        assertEquals(null, changes.getValue("a").size)
    }

    @Test
    fun everyTileGetsACellSoNothingMovesByItselfAfterwards() {
        val changes = FrequentTile.firstPlacement(
            listOf(Entry("a", CategorySize.NORMAL), Entry("b", CategorySize.SMALL))
        )
        assertEquals(setOf(FrequentTile.ID, "a", "b"), changes.keys)
    }

    @Test
    fun theOrderStartsWithTheFrequentTile() {
        val changes = FrequentTile.firstPlacement(normals())
        assertEquals(FrequentTile.ID, FrequentTile.orderAfter(changes).first())
    }

    @Test
    fun theStoreKeepsTheTilesEntryEvenWhenItIsTheDefault() {
        val raw = CategoryOptionsStore.encode(mapOf(FrequentTile.ID to FrequentTile.DEFAULT))
        assertTrue(FrequentTile.ID in CategoryOptionsStore.decode(raw))
    }

    @Test
    fun theStoreDoesNotPruneTheTilesEntryAsAFolderThatIsGone() {
        val raw = CategoryOptionsStore.encode(mapOf(FrequentTile.ID to FrequentTile.DEFAULT))
        val updated = CategoryOptionsStore.update(raw, "x", listOf("x")) { it.copy(hidden = true) }
        assertTrue(FrequentTile.ID in CategoryOptionsStore.decode(updated))
    }
}
