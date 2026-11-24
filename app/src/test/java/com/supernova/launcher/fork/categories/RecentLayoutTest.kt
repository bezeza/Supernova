package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentLayoutTest {

    @Test
    fun theStripKeepsTheSettingsCountInOneRowAndTakesTheWholeWidth() {
        listOf(2, 4, 8).forEach { slots ->
            assertEquals(slots, RecentLayout.columns(CategorySize.STRIP, slots))
            assertEquals(1, RecentLayout.rows(CategorySize.STRIP))
            assertEquals(slots, RecentLayout.capacity(CategorySize.STRIP, slots))
            assertEquals(4, RecentLayout.span(CategorySize.STRIP, 4))
        }
    }

    @Test
    fun theHalfWidthCardIsTwoByTwoAndTakesTwoSpans() {
        assertEquals(4, RecentLayout.capacity(CategorySize.NORMAL, 8))
        assertEquals(4, RecentLayout.capacity(CategorySize.NORMAL, 2))
        assertEquals(2, RecentLayout.span(CategorySize.NORMAL, 4))
    }

    @Test
    fun theFullWidthCardIsFourByTwoAndTakesTheWholeWidth() {
        assertEquals(8, RecentLayout.capacity(CategorySize.WIDE, 4))
        assertEquals(4, RecentLayout.span(CategorySize.WIDE, 4))
    }

    @Test
    fun theSizesAreTheTilesOfTheSameFootprint() {
        assertEquals(4 to 1, CellFlow.footprint(CategorySize.STRIP).let { it.w to it.h })
        assertEquals(2 to 2, CellFlow.footprint(CategorySize.NORMAL).let { it.w to it.h })
        assertEquals(4 to 2, CellFlow.footprint(CategorySize.WIDE).let { it.w to it.h })
    }

    @Test
    fun aSizeTheRowCannotHaveOrNoChoiceIsTheStrip() {
        assertEquals(CategorySize.STRIP, RecentLayout.sizeOf(null))
        assertEquals(CategorySize.STRIP, RecentLayout.sizeOf(CategoryOptions(size = CategorySize.TALL)))
        assertEquals(CategorySize.WIDE, RecentLayout.sizeOf(CategoryOptions(size = CategorySize.WIDE)))
    }
}
