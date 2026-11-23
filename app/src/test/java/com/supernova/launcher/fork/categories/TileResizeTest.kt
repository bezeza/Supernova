package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A corner dragged takes the size whose width is nearest to where the edge is let go.
class TileResizeTest {

    // A row of two spans each 100 wide, less margins: small 90, normal 190, wide 390 (four spans).
    private fun size(width: Float) = TileResize.sizeFor(width, 90f, 190f, 390f)

    @Test
    fun theExactWidthsGiveTheirSizes() {
        assertEquals(CategorySize.SMALL, size(90f))
        assertEquals(CategorySize.NORMAL, size(190f))
        assertEquals(CategorySize.WIDE, size(390f))
    }

    @Test
    fun theNearestOneWins() {
        assertEquals(CategorySize.SMALL, size(130f))
        assertEquals(CategorySize.NORMAL, size(150f))
        assertEquals(CategorySize.NORMAL, size(280f))
        assertEquals(CategorySize.WIDE, size(300f))
    }

    @Test
    fun beyondTheEndsHolds() {
        assertEquals(CategorySize.SMALL, size(10f))
        assertEquals(CategorySize.WIDE, size(900f))
    }
}
