package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// The folders' order after tiles are dragged: the ones without a tile keep their places.
class CategoryReorderTest {

    @Test
    fun allShownJustTakesTheNewOrder() {
        assertEquals(listOf("c", "a", "b"), CategoryReorder.merged(listOf("a", "b", "c"), listOf("c", "a", "b")))
    }

    @Test
    fun aHiddenFolderKeepsItsPlace() {
        // b has no tile: it stays second, a and c swap around it.
        assertEquals(listOf("c", "b", "a"), CategoryReorder.merged(listOf("a", "b", "c"), listOf("c", "a")))
    }

    @Test
    fun foldersWithoutTilesAtTheEndAndTheStartStay() {
        val all = listOf("x", "a", "b", "c", "y")
        assertEquals(listOf("x", "c", "a", "b", "y"), CategoryReorder.merged(all, listOf("c", "a", "b")))
    }

    @Test
    fun noChangeLeavesTheOrderAlone() {
        val all = listOf("a", "b", "c", "d")
        assertEquals(all, CategoryReorder.merged(all, listOf("a", "c", "d")))
    }

    @Test
    fun anUnknownIdIsDropped() {
        assertEquals(listOf("b", "a"), CategoryReorder.merged(listOf("a", "b"), listOf("b", "ghost", "a")))
    }

    @Test
    fun aRepeatedIdCountsOnce() {
        assertEquals(listOf("b", "a"), CategoryReorder.merged(listOf("a", "b"), listOf("b", "b", "a")))
    }

    @Test
    fun noTilesLeavesEverythingAsItWas() {
        assertEquals(listOf("a", "b"), CategoryReorder.merged(listOf("a", "b"), emptyList()))
    }
}
