package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// How high the outline that follows a dragged corner is for its width.
class OutlineHeightTest {

    private fun height(width: Float) = TileGeometry.outlineHeight(width, normal = 200f, wide = 400f, wideHeight = 100f)

    @Test
    fun squareUpToANormalTile() {
        assertEquals(80f, height(80f), 0.001f)
        assertEquals(200f, height(200f), 0.001f)
    }

    @Test
    fun flatterAsItGrowsToAWideOne() {
        assertEquals(150f, height(300f), 0.001f)
        assertEquals(100f, height(400f), 0.001f)
    }

    @Test
    fun pastTheWideWidthItStaysAStrip() {
        assertEquals(100f, height(900f), 0.001f)
    }
}
