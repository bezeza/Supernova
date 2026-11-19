package com.supernova.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderManualOrderTest {

    @Test
    fun clearingTheFlagIsWhatGetsStoredAndTheMembersStayInPlace() {
        val files = DrawerGroup(
            "Files", linkedSetOf("0/c/C", "0/a/A", "0/b/B"),
            type = DrawerGroupType.FOLDER, manualOrder = true
        )
        val other = DrawerGroup(
            "Chat", linkedSetOf("0/d/D"),
            type = DrawerGroupType.FOLDER, manualOrder = true
        )
        val cleared = FolderManualOrder.cleared(listOf(files, other), files.id)
        val stored = DrawerGroup.decode(cleared[0].encode())!!
        assertFalse(stored.manualOrder)
        assertFalse(stored.smart)
        assertEquals(listOf("0/c/C", "0/a/A", "0/b/B"), stored.members.toList())
        assertTrue(cleared[1].manualOrder)
        assertEquals(other.members.toList(), cleared[1].members.toList())
    }

    @Test
    fun undoRestoresTheFlagWithoutReordering() {
        val files = DrawerGroup(
            "Files", linkedSetOf("0/c/C", "0/a/A"),
            type = DrawerGroupType.FOLDER, manualOrder = true
        )
        val stored = DrawerGroup.decode(FolderManualOrder.cleared(listOf(files), files.id).single().encode())!!
        val back = DrawerGroup.decode(FolderManualOrder.restored(listOf(stored), stored.id).single().encode())!!
        assertTrue(back.manualOrder)
        assertEquals(listOf("0/c/C", "0/a/A"), back.members.toList())
    }

    @Test
    fun aFolderThatAlreadyFollowsTheDrawerIsLeftAsStored() {
        val files = DrawerGroup("Files", linkedSetOf("0/a/A"), type = DrawerGroupType.FOLDER)
        val cleared = FolderManualOrder.cleared(listOf(files), files.id).single()
        assertFalse(cleared.manualOrder)
        assertEquals(files.encode(), cleared.encode())
    }

    @Test
    fun anotherIdIsNotTouched() {
        val files = DrawerGroup("Files", linkedSetOf("0/a/A"), type = DrawerGroupType.FOLDER, manualOrder = true)
        val groups = listOf(files)
        assertSame(files, FolderManualOrder.cleared(groups, "missing").single())
        assertSame(files, FolderManualOrder.restored(groups, "missing").single())
    }

    @Test
    fun aSmartFolderStaysSmart() {
        val games = DrawerGroup("Games", setOf("0/a/A"), type = DrawerGroupType.FOLDER, smart = true)
        val cleared = FolderManualOrder.cleared(listOf(games), games.id).single()
        assertTrue(cleared.smart)
        assertFalse(cleared.manualOrder)
    }
}
