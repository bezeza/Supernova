package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// What Back and a new list of folders do to a category's open screen.
class CategoryScreenStateTest {

    @Test
    fun nothingIsOpenAtFirstAndBackIsNotSpent() {
        val state = CategoryScreenState()
        assertFalse(state.isOpen)
        assertFalse(state.close())
    }

    @Test
    fun tappingATileOpensItsScreen() {
        val state = CategoryScreenState()
        state.open("Ігри")
        assertTrue(state.isOpen)
        assertEquals("Ігри", state.openId)
    }

    @Test
    fun backClosesTheScreenOnceThenIsTheDrawers() {
        val state = CategoryScreenState()
        state.open("ШІ")
        assertTrue(state.close())
        assertNull(state.openId)
        assertFalse(state.close())
    }

    @Test
    fun theScreenStaysWhileItsCategoryIsStillThere() {
        val state = CategoryScreenState()
        state.open("Медицина")
        assertTrue(state.sync(listOf("Ігри", "Медицина")))
        assertEquals("Медицина", state.openId)
    }

    @Test
    fun aDeletedCategoryTakesItsScreenWithIt() {
        val state = CategoryScreenState()
        state.open("Медицина")
        assertFalse(state.sync(listOf("Ігри")))
        assertFalse(state.isOpen)
    }

    @Test
    fun syncWithNothingOpenStaysClosed() {
        val state = CategoryScreenState()
        assertFalse(state.sync(listOf("Ігри")))
    }

    @Test
    fun aSwipeRightFarAndQuickGoesBack() {
        assertTrue(BackSwipe.isBack(dx = 300f, dy = 20f, velocityX = 1800f, density = 2f))
    }

    @Test
    fun aShortSwipeDoesNot() {
        assertFalse(BackSwipe.isBack(dx = 100f, dy = 0f, velocityX = 1800f, density = 2f))
    }

    @Test
    fun aSlowDragDoesNot() {
        assertFalse(BackSwipe.isBack(dx = 300f, dy = 0f, velocityX = 400f, density = 2f))
    }

    @Test
    fun aScrollMostlyDownDoesNot() {
        assertFalse(BackSwipe.isBack(dx = 300f, dy = 400f, velocityX = 1800f, density = 2f))
    }

    @Test
    fun aSwipeLeftDoesNot() {
        assertFalse(BackSwipe.isBack(dx = -300f, dy = 0f, velocityX = -1800f, density = 2f))
    }
}
