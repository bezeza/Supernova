package com.supernova.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerGroupManualOrderTest {

    @Test
    fun manualOrderSurvivesEncodingAndKeepsTheMembersInOrder() {
        val group = DrawerGroup(
            "Files", linkedSetOf("0/c/C", "0/a/A", "0/b/B"),
            order = 2, type = DrawerGroupType.FOLDER, manualOrder = true
        )
        val back = DrawerGroup.decode(group.encode())!!
        assertTrue(back.manualOrder)
        assertFalse(back.smart)
        assertEquals(listOf("0/c/C", "0/a/A", "0/b/B"), back.members.toList())
    }

    @Test
    fun aPlainGroupIsNotManual() {
        val back = DrawerGroup.decode(DrawerGroup("Chat", setOf("0/a/A"), type = DrawerGroupType.FOLDER).encode())!!
        assertFalse(back.manualOrder)
    }

    @Test
    fun aSmartGroupStaysSmart() {
        val group = DrawerGroup("Games", setOf("0/a/A"), type = DrawerGroupType.FOLDER, smart = true)
        val back = DrawerGroup.decode(group.encode())!!
        assertTrue(back.smart)
        assertFalse(back.manualOrder)
    }
}
