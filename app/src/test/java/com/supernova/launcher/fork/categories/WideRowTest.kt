package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// A wide tile shows up to six icons, and "+N" instead of the last when there are more.
class WideRowTest {

    @Test
    fun noAppsShowNothing() {
        val plan = WideRow.plan(0)
        assertEquals(0, plan.shown)
        assertEquals(0, plan.more)
    }

    @Test
    fun upToSixAppsAreAllShown() {
        listOf(1, 5, 6).forEach {
            val plan = WideRow.plan(it)
            assertEquals(it, plan.shown)
            assertEquals(0, plan.more)
        }
    }

    @Test
    fun sevenAppsShowFiveAndPlusTwo() {
        val plan = WideRow.plan(7)
        assertEquals(5, plan.shown)
        assertEquals(2, plan.more)
    }

    @Test
    fun theCountInPlusNIsEveryAppNotShown() {
        val plan = WideRow.plan(40)
        assertEquals(5, plan.shown)
        assertEquals(35, plan.more)
    }
}
