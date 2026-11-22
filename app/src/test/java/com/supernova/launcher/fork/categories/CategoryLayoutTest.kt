package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Which apps a Categories tile puts in its large icons and which in the corner.
class CategoryLayoutTest {

    private fun names(count: Int) = (1..count).map { "app$it" }

    @Test
    fun emptyCategoryHasNothingToShow() {
        val tile = CategoryLayout.tile(emptyList<String>())
        assertTrue(tile.large.isEmpty())
        assertTrue(tile.mini.isEmpty())
        assertEquals(0, tile.hidden)
    }

    @Test
    fun smallCategoryFillsOnlyLargeSlots() {
        val tile = CategoryLayout.tile(names(2))
        assertEquals(listOf("app1", "app2"), tile.large)
        assertTrue(tile.mini.isEmpty())
        assertEquals(0, tile.hidden)
    }

    @Test
    fun threeAppsAreAllLargeAndCornerStaysEmpty() {
        val tile = CategoryLayout.tile(names(3))
        assertEquals(names(3), tile.large)
        assertTrue(tile.mini.isEmpty())
    }

    @Test
    fun fourthAppStartsTheCorner() {
        val tile = CategoryLayout.tile(names(5))
        assertEquals(names(3), tile.large)
        assertEquals(listOf("app4", "app5"), tile.mini)
    }

    @Test
    fun fullTileHoldsSevenAndCountsTheRest() {
        val tile = CategoryLayout.tile(names(10))
        assertEquals(3, tile.large.size)
        assertEquals(listOf("app4", "app5", "app6", "app7"), tile.mini)
        assertEquals(3, tile.hidden)
    }

    @Test
    fun exactlySevenHidesNothing() {
        assertEquals(0, CategoryLayout.tile(names(7)).hidden)
    }

    @Test
    fun orderOfTheAppsIsKept() {
        val tile = CategoryLayout.tile(listOf("z", "a", "m", "b"))
        assertEquals(listOf("z", "a", "m"), tile.large)
        assertEquals(listOf("b"), tile.mini)
    }

    @Test
    fun recentlyInstalledTakesNewestFirstUpToTheLimit() {
        val times = mapOf("a" to 10L, "b" to 50L, "c" to 30L, "d" to 40L, "e" to 20L)
        val recent = CategoryLayout.recentlyInstalled(times.keys.toList(), 3) { times.getValue(it) }
        assertEquals(listOf("b", "d", "c"), recent)
    }

    @Test
    fun recentlyInstalledSkipsAppsWithoutInstallTime() {
        val times = mapOf("a" to 0L, "b" to 5L)
        val recent = CategoryLayout.recentlyInstalled(times.keys.toList(), 4) { times.getValue(it) }
        assertEquals(listOf("b"), recent)
    }

    @Test
    fun recentlyInstalledOfNothingIsEmpty() {
        assertTrue(CategoryLayout.recentlyInstalled(emptyList<String>(), 4) { 1L }.isEmpty())
    }
}
