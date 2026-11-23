package com.supernova.launcher.fork.folders

import com.supernova.launcher.ui.FolderPreviewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Folders' own options in one preference, read leniently; and what a look asks of the icon.
class FolderOptionsStoreTest {

    @Test
    fun nothingStoredIsNoOptions() {
        assertTrue(FolderOptionsStore.decode("").isEmpty())
        assertTrue(FolderOptionsStore.decode("{bad").isEmpty())
    }

    @Test
    fun optionsRoundTrip() {
        val raw = FolderOptionsStore.encode(
            mapOf("f1" to FolderOptions(columns = 4, tint = 60, hideTitle = true, look = FolderLook.GRID_3X3))
        )
        val back = FolderOptionsStore.decode(raw).getValue("f1")
        assertEquals(4, back.columns)
        assertEquals(60, back.tint)
        assertTrue(back.hideTitle)
        assertEquals(FolderLook.GRID_3X3, back.look)
    }

    @Test
    fun defaultsLeaveNoTrace() {
        assertEquals("", FolderOptionsStore.encode(mapOf("f1" to FolderOptions())))
    }

    @Test
    fun anOldValueMissingFieldsReadsAsDefaults() {
        val back = FolderOptionsStore.decode("""{"f1":{"columns":3}}""").getValue("f1")
        assertEquals(3, back.columns)
        assertNull(back.tint)
        assertEquals(FolderLook.DEFAULT, back.look)
    }

    @Test
    fun valuesOutOfRangeReadAsDefaults() {
        val back = FolderOptionsStore.decode("""{"f1":{"columns":99,"tint":150,"look":"SPIRAL"}}""").getValue("f1")
        assertEquals(0, back.columns)
        assertNull(back.tint)
        assertEquals(FolderLook.DEFAULT, back.look)
    }

    @Test
    fun updateChangesOneFolderAndKeepsTheOthers() {
        val first = FolderOptionsStore.update("", "a") { it.copy(columns = 2) }
        val second = FolderOptionsStore.update(first, "b") { it.copy(hideTitle = true) }
        val back = FolderOptionsStore.decode(second)
        assertEquals(2, back.getValue("a").columns)
        assertTrue(back.getValue("b").hideTitle)
    }

    @Test
    fun theDefaultLookAsksNothingOfTheIcon() {
        assertNull(FolderLooks.spec(FolderLook.DEFAULT))
    }

    @Test
    fun gridLooksAskForTheirStyle() {
        assertEquals(FolderPreviewStyle.GRID, FolderLooks.spec(FolderLook.GRID)?.style)
        assertEquals(FolderPreviewStyle.GRID_3X3, FolderLooks.spec(FolderLook.GRID_3X3)?.style)
    }

    @Test
    fun theSingleLookDrawsOnlyTheFirstAppLarge() {
        val spec = requireNotNull(FolderLooks.spec(FolderLook.SINGLE))
        assertEquals(1, spec.limit)
        assertTrue(spec.prioritizeFirst)
    }

    @Test
    fun aNamedFolderIsFoundOnlyJustAfter() {
        val options = FolderOptions(columns = 3)
        val pending = PendingFolder(options, at = 1000)
        assertEquals(options, PendingFolder.usable(pending, now = 1500))
        assertNull(PendingFolder.usable(pending, now = 1000 + PendingFolder.VALID_MS + 1))
        assertNull(PendingFolder.usable(null, now = 1000))
    }
}
