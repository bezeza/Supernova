package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A tile's menu keeps clear of the arc at the tile's corner.
class MenuSpotTest {

    private val room = MenuSpot.Room(width = 1000, height = 2000, margin = 10, gap = 20)
    private val arc = MenuSpot.Box(800, 1000, 900, 1100)

    @Test
    fun aMenuThatDoesNotMeetTheArcStaysWhereItWas() {
        assertEquals(100 to 200, MenuSpot.place(MenuSpot.Box(100, 200, 400, 600), arc, room))
    }

    @Test
    fun noArcNoChange() {
        assertEquals(700 to 900, MenuSpot.place(MenuSpot.Box(700, 900, 1000, 1300), null, room))
    }

    @Test
    fun aMenuOverTheArcMovesAboveIt() {
        // 400 high, so above the arc it begins at 1000 - 20 - 400 = 580.
        assertEquals(700 to 580, MenuSpot.place(MenuSpot.Box(700, 900, 1000, 1300), arc, room))
    }

    @Test
    fun withNoRoomAboveItGoesBelow() {
        val high = MenuSpot.Box(800, 100, 900, 200)
        assertEquals(700 to 220, MenuSpot.place(MenuSpot.Box(700, 150, 1000, 550), high, room))
    }

    @Test
    fun withNoRoomAboveOrBelowItGoesToTheLeft() {
        val tall = MenuSpot.Room(width = 1000, height = 500, margin = 10, gap = 20)
        val zone = MenuSpot.Box(800, 100, 900, 400)
        assertEquals(480 to 150, MenuSpot.place(MenuSpot.Box(700, 150, 1000, 550), zone, tall))
    }

    @Test
    fun withNoRoomAnywhereItStaysWhereItWas() {
        val small = MenuSpot.Room(width = 400, height = 450, margin = 10, gap = 20)
        val zone = MenuSpot.Box(100, 100, 300, 400)
        assertEquals(50 to 20, MenuSpot.place(MenuSpot.Box(50, 20, 350, 420), zone, small))
    }
}
