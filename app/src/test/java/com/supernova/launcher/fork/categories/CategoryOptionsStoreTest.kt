package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The categories' options in one preference: read leniently, defaults left out.
class CategoryOptionsStoreTest {

    @Test
    fun nothingStoredIsNoOptions() {
        assertTrue(CategoryOptionsStore.decode("").isEmpty())
    }

    @Test
    fun anUnreadableValueIsNoOptionsNotACrash() {
        assertTrue(CategoryOptionsStore.decode("{not json").isEmpty())
        assertTrue(CategoryOptionsStore.decode("[1,2]").isEmpty())
    }

    @Test
    fun optionsRoundTrip() {
        val games = CategoryOptions(CategorySort.ALPHA, hidden = true)
        val ai = CategoryOptions(CategorySort.FREQUENCY)
        val raw = CategoryOptionsStore.encode(mapOf("Ігри" to games, "ШІ" to ai))
        val back = CategoryOptionsStore.decode(raw)
        assertEquals(CategorySort.ALPHA, back.getValue("Ігри").sort)
        assertTrue(back.getValue("Ігри").hidden)
        assertEquals(CategorySort.FREQUENCY, back.getValue("ШІ").sort)
        assertFalse(back.getValue("ШІ").hidden)
    }

    @Test
    fun anUnknownSortNameReadsAsTheDrawers() {
        val back = CategoryOptionsStore.decode("""{"Ігри":{"sort":"SPIRAL","hidden":true}}""")
        assertEquals(CategorySort.DRAWER, back.getValue("Ігри").sort)
        assertTrue(back.getValue("Ігри").hidden)
    }

    @Test
    fun aFieldAFutureBuildAddsIsIgnored() {
        val back = CategoryOptionsStore.decode("""{"Ігри":{"sort":"ALPHA","hidden":false,"size":"WIDE"}}""")
        assertEquals(CategorySort.ALPHA, back.getValue("Ігри").sort)
    }

    @Test
    fun aCategorySetBackToDefaultsLeavesNoTrace() {
        assertEquals("", CategoryOptionsStore.encode(mapOf("Ігри" to CategoryOptions())))
    }

    @Test
    fun updateChangesOneCategoryAndKeepsTheOthers() {
        val first = CategoryOptionsStore.update("", "Ігри", listOf("Ігри", "ШІ")) { it.copy(hidden = true) }
        val ids = listOf("Ігри", "ШІ")
        val second = CategoryOptionsStore.update(first, "ШІ", ids) { it.copy(sort = CategorySort.MANUAL) }
        val back = CategoryOptionsStore.decode(second)
        assertTrue(back.getValue("Ігри").hidden)
        assertEquals(CategorySort.MANUAL, back.getValue("ШІ").sort)
    }

    @Test
    fun updateDropsCategoriesThatAreGone() {
        val first = CategoryOptionsStore.update("", "Старе", listOf("Старе")) { it.copy(hidden = true) }
        val second = CategoryOptionsStore.update(first, "Ігри", listOf("Ігри")) { it.copy(hidden = true) }
        assertEquals(setOf("Ігри"), CategoryOptionsStore.decode(second).keys)
    }
}
