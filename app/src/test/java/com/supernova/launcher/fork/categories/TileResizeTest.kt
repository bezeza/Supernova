package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A corner dragged to the right grows a tile one step per unit, to the left shrinks it.
class TileResizeTest {

    private val unit = 100f

    @Test
    fun aSmallDragChangesNothing() {
        assertEquals(CategorySize.NORMAL, TileResize.resize(CategorySize.NORMAL, 20f, unit))
        assertEquals(CategorySize.NORMAL, TileResize.resize(CategorySize.NORMAL, -40f, unit))
    }

    @Test
    fun rightGrowsNormalToWide() {
        assertEquals(CategorySize.WIDE, TileResize.resize(CategorySize.NORMAL, 60f, unit))
    }

    @Test
    fun leftShrinksNormalToSmall() {
        assertEquals(CategorySize.SMALL, TileResize.resize(CategorySize.NORMAL, -60f, unit))
    }

    @Test
    fun smallToNormalToWide() {
        assertEquals(CategorySize.NORMAL, TileResize.resize(CategorySize.SMALL, 100f, unit))
        assertEquals(CategorySize.WIDE, TileResize.resize(CategorySize.SMALL, 200f, unit))
    }

    @Test
    fun wideBackToSmallInOneDrag() {
        assertEquals(CategorySize.SMALL, TileResize.resize(CategorySize.WIDE, -200f, unit))
    }

    @Test
    fun theEndsHold() {
        assertEquals(CategorySize.WIDE, TileResize.resize(CategorySize.WIDE, 500f, unit))
        assertEquals(CategorySize.SMALL, TileResize.resize(CategorySize.SMALL, -500f, unit))
    }

    @Test
    fun noUnitChangesNothing() {
        assertEquals(CategorySize.NORMAL, TileResize.resize(CategorySize.NORMAL, 300f, 0f))
    }
}
