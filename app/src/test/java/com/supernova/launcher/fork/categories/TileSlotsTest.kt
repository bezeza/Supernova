package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Every tile layout has the icons it promises, inside the card and not on top of each other.
class TileSlotsTest {

    private val eps = 0.0001f

    private fun inside(slots: List<TileSlots.Slot>) = slots.all {
        it.x >= -eps && it.y >= -eps && it.x + it.size <= 1f + eps && it.y + it.size <= 1f + eps
    }

    private fun overlap(a: TileSlots.Slot, b: TileSlots.Slot): Boolean =
        a.x < b.x + b.size - eps && b.x < a.x + a.size - eps && a.y < b.y + b.size - eps && b.y < a.y + a.size - eps

    private fun noOverlap(slots: List<TileSlots.Slot>): Boolean {
        for (i in slots.indices) for (j in i + 1 until slots.size) if (overlap(slots[i], slots[j])) return false
        return true
    }

    @Test
    fun theUsualTileHasThreeLargeAndFourSmall() {
        val slots = TileSlots.normal(3)
        assertEquals(3, TileSlots.largeCount(slots))
        assertEquals(4, TileSlots.miniCount(slots))
    }

    @Test
    fun oneLargeTileHasOneLargeAndTwelveSmall() {
        val slots = TileSlots.normal(1)
        assertEquals(1, TileSlots.largeCount(slots))
        assertEquals(12, TileSlots.miniCount(slots))
    }

    @Test
    fun noLargeTileIsThreeByThreeSmall() {
        val slots = TileSlots.normal(0)
        assertEquals(0, TileSlots.largeCount(slots))
        assertEquals(9, TileSlots.miniCount(slots))
    }

    @Test
    fun aSmallTileIsTwoByTwoSmall() {
        val slots = TileSlots.small()
        assertEquals(0, TileSlots.largeCount(slots))
        assertEquals(4, TileSlots.miniCount(slots))
    }

    @Test
    fun everyLayoutStaysInsideTheCard() {
        listOf(TileSlots.normal(3), TileSlots.normal(1), TileSlots.normal(0), TileSlots.small()).forEach {
            assertTrue(inside(it))
        }
    }

    @Test
    fun noLayoutPutsTwoIconsOnTopOfEachOther() {
        listOf(TileSlots.normal(3), TileSlots.normal(1), TileSlots.normal(0), TileSlots.small()).forEach {
            assertTrue(noOverlap(it))
        }
    }

    @Test
    fun theLargeIconsComeFirstInTheOrderOfTheSlots() {
        assertTrue(TileSlots.normal(3).take(3).all { it.large })
    }

    @Test
    fun appsFillTheLargeSlotsThenTheSmallOnes() {
        val tile = CategoryLayout.tile((1..20).toList(), TileSlots.normal(1))
        assertEquals(listOf(1), tile.large)
        assertEquals((2..13).toList(), tile.mini)
        assertEquals(7, tile.hidden)
    }

    @Test
    fun aNoLargeTileTakesNineAppsAndLeavesTheRest() {
        val tile = CategoryLayout.tile((1..12).toList(), TileSlots.normal(0))
        assertTrue(tile.large.isEmpty())
        assertEquals(9, tile.mini.size)
        assertEquals(3, tile.hidden)
    }

    @Test
    fun aDoubleTileHasSevenLargeAndAFourSmallCorner() {
        val slots = TileSlots.double()
        assertEquals(7, TileSlots.largeCount(slots))
        assertEquals(4, TileSlots.miniCount(slots))
        assertTrue(slots.take(7).all { it.large })
    }

    @Test
    fun aDoubleTileRunsTwoHeightsAcrossAndStaysOneHigh() {
        val slots = TileSlots.double()
        assertEquals(2f, TileSlots.contentWidth(slots), eps)
        assertTrue(slots.all { it.x >= -eps && it.y >= -eps && it.x + it.size <= 2f + eps })
        assertTrue(slots.all { it.y + it.size <= 1f + eps })
        assertTrue(noOverlap(slots))
    }

    @Test
    fun aSquareTileRunsOneHeightAcross() {
        assertEquals(1f, TileSlots.contentWidth(TileSlots.normal(3)), eps)
    }

    @Test
    fun aDoubleTileTakesTheFirstSevenAppsLargeAndTheNextFourSmall() {
        val tile = CategoryLayout.tile((1..20).toList(), TileSlots.double())
        assertEquals((1..7).toList(), tile.large)
        assertEquals((8..11).toList(), tile.mini)
        assertEquals(9, tile.hidden)
    }

    @Test
    fun aBigTileHasFifteenLargeAndAFourSmallCorner() {
        val slots = TileSlots.big()
        assertEquals(15, TileSlots.largeCount(slots))
        assertEquals(4, TileSlots.miniCount(slots))
        assertTrue(slots.take(15).all { it.large })
    }

    @Test
    fun aBigTileIsASquareAndItsIconsStayInsideAndApart() {
        val slots = TileSlots.big()
        assertEquals(1f, TileSlots.contentWidth(slots), eps)
        assertTrue(inside(slots))
        assertTrue(noOverlap(slots))
    }

    @Test
    fun aBigTileTakesTheFirstFifteenAppsLargeAndTheNextFourSmall() {
        val tile = CategoryLayout.tile((1..30).toList(), TileSlots.big())
        assertEquals((1..15).toList(), tile.large)
        assertEquals((16..19).toList(), tile.mini)
        assertEquals(11, tile.hidden)
    }

    @Test
    fun aTallTileHasSevenLargeAndAFourSmallCorner() {
        val slots = TileSlots.tall()
        assertEquals(7, TileSlots.largeCount(slots))
        assertEquals(4, TileSlots.miniCount(slots))
        assertTrue(slots.take(7).all { it.large })
    }

    @Test
    fun aTallTileRunsAHalfHeightAcrossAndStaysInsideAndApart() {
        val slots = TileSlots.tall()
        assertEquals(0.5f, TileSlots.contentWidth(slots), eps)
        assertTrue(inside(slots))
        assertTrue(noOverlap(slots))
    }
}
