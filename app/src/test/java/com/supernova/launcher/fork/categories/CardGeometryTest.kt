package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A tile card's icons fit inside it, centred, for each layout it can have.
class CardGeometryTest {

    // A normal tile 188 wide with margins of 6, as two spans of 100 make it.
    private val normal = 188
    private val gap = 6

    private fun check(slots: List<TileSlots.Slot>, width: Int) {
        val geometry = CardGeometry({ slots }, { gap })
        val height = geometry.height(width)
        val ref = geometry.reference(width, height)
        val content = TileSlots.contentWidth(slots)
        assertTrue("fits across", (content * ref).toInt() <= width)
        assertTrue("fits down", ref <= height)
        assertTrue(geometry.left(width, height) >= 0)
        assertTrue(geometry.top(width, height) >= 0)
        // Centred: the room left over is the same on both sides.
        val right = width - geometry.left(width, height) - (content * ref).toInt()
        assertTrue(kotlin.math.abs(geometry.left(width, height) - right) <= 1)
    }

    @Test
    fun aSquareTileIsAsHighAsItIsWide() {
        val geometry = CardGeometry({ TileSlots.normal(3) }, { gap })
        assertEquals(normal, geometry.height(normal))
        check(TileSlots.normal(3), normal)
        check(TileSlots.small(), normal / 2)
    }

    @Test
    fun aSmallTileStaysSquareWithItsNameInTheCard() {
        // A span of 100 less margins of 6 each side: 88. Its icons run 0.78 of a height across, still not a tall tile.
        val width = 88
        assertEquals(width, CardGeometry({ TileSlots.small() }, { gap }).height(width))
    }

    @Test
    fun aDoubleTileIsAsHighAsANormalOne() {
        val geometry = CardGeometry({ TileSlots.double() }, { gap })
        assertEquals(normal, geometry.height(2 * normal + 2 * gap))
        check(TileSlots.double(), 2 * normal + 2 * gap)
    }

    @Test
    fun aTallTileIsAsHighAsTwoNormalOnes() {
        val geometry = CardGeometry({ TileSlots.tall() }, { gap })
        assertEquals(2 * normal + 2 * gap, geometry.height(normal))
        check(TileSlots.tall(), normal)
    }

    @Test
    fun aStripIsOneRowAsHighAsAnIconIsWideAndItsIconsFit() {
        listOf(4, 5).forEach { count ->
            val slots = TileSlots.strip(count)
            val wide = 4 * normal / 2 + 2 * gap
            val geometry = CardGeometry({ slots }, { gap })
            assertEquals(wide / count - gap, geometry.height(wide))
            check(slots, wide)
        }
    }

    @Test
    fun aBigTileIsSquare() {
        val width = 2 * normal + 2 * gap
        assertEquals(width, CardGeometry({ TileSlots.big() }, { gap }).height(width))
        check(TileSlots.big(), width)
    }

    @Test
    fun aSmallCardIsAsHighAsItIsWideAndTwoOfThemWithTheirGapAreANormalCard() {
        // A span of 100 and margins of 6: the card of a small tile is 88 wide, the card of a normal one 188. A pair
        // is two small cards, each with a margin above and below, so the gap between them is two margins.
        val smallWidth = 100 - 2 * gap
        val normalWidth = 2 * 100 - 2 * gap
        val small = CardGeometry({ TileSlots.small() }, { gap }).height(smallWidth)
        val normalCard = CardGeometry({ TileSlots.normal(3) }, { gap }).height(normalWidth)
        assertEquals(smallWidth, small)
        assertEquals(normalCard, 2 * small + 2 * gap)
    }

    @Test
    fun theNameOfASmallTileTakesAStripAndItsIconsStillFit() {
        val slots = TileSlots.small()
        val geometry = CardGeometry({ slots }, { gap })
        val width = 100 - 2 * gap
        val height = geometry.height(width)
        val ref = geometry.reference(width, height)
        val strip = (TileSlots.nameShare(slots) * ref).toInt()
        // The strip and the two rows of icons together are the card's inner height.
        assertTrue(strip > 0)
        assertTrue(ref <= height)
    }
}
