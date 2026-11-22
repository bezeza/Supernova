package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A tile's fill follows the drawer's own background opacity.
class CategoryTintTest {

    @Test
    fun aSolidDrawerKeepsTheLightTint() {
        assertEquals(CategoryTint.SOLID, CategoryTint.cardAlpha(1f))
    }

    @Test
    fun aClearDrawerGetsTheGlassFill() {
        assertEquals(CategoryTint.GLASS, CategoryTint.cardAlpha(0f))
    }

    @Test
    fun theClearerTheDrawerTheMoreBodyTheTile() {
        assertTrue(CategoryTint.cardAlpha(0.3f) > CategoryTint.cardAlpha(0.8f))
    }

    @Test
    fun anOpacityOutsideZeroToOneIsHeldToIt() {
        assertEquals(CategoryTint.cardAlpha(1f), CategoryTint.cardAlpha(1.7f))
        assertEquals(CategoryTint.cardAlpha(0f), CategoryTint.cardAlpha(-0.4f))
    }

    @Test
    fun theFillKeepsTheTextColourAndOnlyTakesItsAlpha() {
        val color = CategoryTint.cardColor(0xFF336699.toInt(), 1f)
        assertEquals(0x336699, color and 0xFFFFFF)
        assertEquals(CategoryTint.SOLID, (color ushr 24) and 0xFF)
    }
}
