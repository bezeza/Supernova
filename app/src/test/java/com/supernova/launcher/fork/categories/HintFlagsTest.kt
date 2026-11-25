package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// When a hint is due, and that one seen stays seen.
class HintFlagsTest {

    private val written = mutableListOf<String>()
    private val flags = HintFlags({ it in written }, { written += it })

    @Test
    fun theTilesHintNeedsTilesOnScreenAndNoArranging() {
        assertTrue(flags.tileMenuDue(tilesShown = true, hasTiles = true, arranging = false))
        assertFalse(flags.tileMenuDue(tilesShown = false, hasTiles = true, arranging = false))
        assertFalse(flags.tileMenuDue(tilesShown = true, hasTiles = false, arranging = false))
        assertFalse(flags.tileMenuDue(tilesShown = true, hasTiles = true, arranging = true))
    }

    @Test
    fun theColumnsHintWaitsOutTheChoosing() {
        assertFalse(flags.columnsDue(picking = true))
        assertTrue(flags.columnsDue(picking = false))
    }

    @Test
    fun aSeenHintIsNotToldAgainAndTheOtherStillIs() {
        flags.markSeen(Hint.TILE_MENU)
        flags.markSeen(Hint.TILE_MENU)
        assertEquals(listOf("tile_menu"), written)
        assertFalse(flags.tileMenuDue(tilesShown = true, hasTiles = true, arranging = false))
        assertTrue(flags.columnsDue(picking = false))
    }
}
