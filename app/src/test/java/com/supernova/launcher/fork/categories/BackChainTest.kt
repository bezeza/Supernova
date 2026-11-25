package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// One Back, one thing closed, the topmost first.
class BackChainTest {

    private val open = mutableSetOf<String>()

    private fun back(): Boolean = BackChain.run(
        menu = { open.remove("menu") },
        arranging = { open.remove("arranging") },
        picking = { open.remove("picking") },
        screen = { open.remove("screen") }
    )

    @Test
    fun eachBackClosesTheTopmostThingOnly() {
        open += listOf("screen", "picking", "arranging", "menu")
        assertTrue(back())
        assertEquals(setOf("screen", "picking", "arranging"), open)
        assertTrue(back())
        assertEquals(setOf("screen", "picking"), open)
        assertTrue(back())
        assertEquals(setOf("screen"), open)
        assertTrue(back())
        assertEquals(emptySet<String>(), open)
    }

    @Test
    fun choosingStopsAndTheScreenStays() {
        open += listOf("screen", "picking")
        assertTrue(back())
        assertEquals(setOf("screen"), open)
    }

    @Test
    fun withNothingOpenTheBackIsNotSpent() {
        assertFalse(back())
    }
}
