package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// How far a list's bottom lies under a dock, a page indicator or the navigation bar.
class BottomClearanceTest {

    @Test
    fun nothingOverTheListIsNoClearance() {
        assertEquals(0, BottomClearance.over(1000, listOf(null, null)))
        assertEquals(0, BottomClearance.over(1000, emptyList()))
    }

    @Test
    fun aDockAboveTheListsBottomTakesTheOverlap() {
        assertEquals(120, BottomClearance.over(1000, listOf(880, null)))
    }

    @Test
    fun theLargestOverlapWins() {
        assertEquals(200, BottomClearance.over(1000, listOf(880, 800)))
    }

    @Test
    fun somethingBelowTheListIsNoOverlap() {
        assertEquals(0, BottomClearance.over(1000, listOf(1100, 1500)))
    }

    @Test
    fun theNavigationBarCoversWhatIsBelowItsTop() {
        assertEquals(48, BottomClearance.underNavigation(bottomInWindow = 2400, windowHeight = 2400, barHeight = 48))
        assertEquals(0, BottomClearance.underNavigation(bottomInWindow = 2000, windowHeight = 2400, barHeight = 48))
        assertEquals(0, BottomClearance.underNavigation(bottomInWindow = 2400, windowHeight = 2400, barHeight = 0))
    }
}
