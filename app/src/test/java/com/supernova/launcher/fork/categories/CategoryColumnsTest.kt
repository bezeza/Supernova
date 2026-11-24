package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// How many columns an open category has, how big its icons are, and how its block of apps is centred.
class CategoryColumnsTest {

    private fun cols(setting: CategoryColumns, apps: Int, menu: Int = 7) =
        CategoryColumnsLayout.columns(setting, apps, menu)

    @Test
    fun autoGivesThreeColumnsToNineAppsAndFewer() {
        listOf(1, 2, 3, 9).forEach { assertEquals(3, cols(CategoryColumns.AUTO, it)) }
    }

    @Test
    fun autoGivesFourColumnsFromTenToSixteenApps() {
        listOf(10, 11, 16).forEach { assertEquals(4, cols(CategoryColumns.AUTO, it)) }
    }

    @Test
    fun autoGivesMoreThanSixteenAppsTheMenusColumns() {
        assertEquals(7, cols(CategoryColumns.AUTO, 17))
        assertEquals(5, cols(CategoryColumns.AUTO, 40, menu = 5))
    }

    @Test
    fun anAppAddedToANineAppCategoryTakesItToFourColumns() {
        val before = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 9, 7, 1f, 1080, 2.75f)
        val after = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 10, 7, 1f, 1080, 2.75f)
        assertEquals(3, before.shown)
        assertEquals(4, after.shown)
        assertTrue(after.columnPx < before.columnPx)
    }

    @Test
    fun aChosenNumberOfColumnsHoldsWhateverTheCount() {
        assertEquals(3, cols(CategoryColumns.THREE, 40))
        assertEquals(4, cols(CategoryColumns.FOUR, 1))
        assertEquals(5, cols(CategoryColumns.FIVE, 8))
        assertEquals(7, cols(CategoryColumns.MENU, 8))
    }

    @Test
    fun fewerAppsThanColumnsKeepTheColumnWidthAndCentreTheBlock() {
        // 1080 px of grid, three columns of 360; two apps fill two of them, a half column is left each side.
        val r = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 2, 7, 1f, 1080, 2.75f)
        assertEquals(3, r.columns)
        assertEquals(2, r.shown)
        assertEquals(360, r.columnPx)
        assertEquals(180, r.sidePx)
        val one = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 1, 7, 1f, 1080, 2.75f)
        assertEquals(1, one.shown)
        assertEquals(360, one.sidePx)
    }

    @Test
    fun aFullGridIsNotCentred() {
        val r = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 9, 7, 1f, 1080, 2.75f)
        assertEquals(3, r.shown)
        assertEquals(0, r.sidePx)
    }

    @Test
    fun theIconGrowsWithTheColumnButNeverBelowTheMenusOrPastTheCap() {
        val three = CategoryColumnsLayout.compute(CategoryColumns.THREE, 5, 7, 1f, 1080, 2.75f)
        val five = CategoryColumnsLayout.compute(CategoryColumns.FIVE, 5, 7, 1f, 1080, 2.75f)
        val menu = CategoryColumnsLayout.compute(CategoryColumns.MENU, 5, 7, 1.2f, 1080, 2.75f)
        assertTrue(three.iconScale > five.iconScale)
        assertTrue(five.iconScale >= 1f)
        assertEquals(1.2f, menu.iconScale, 0.0001f)
        val tablet = CategoryColumnsLayout.compute(CategoryColumns.THREE, 5, 7, 1f, 6000, 2.75f)
        assertEquals(2f, tablet.iconScale, 0.0001f)
    }

    @Test
    fun noAppsStillMakeAGrid() {
        val r = CategoryColumnsLayout.compute(CategoryColumns.AUTO, 0, 7, 1f, 1080, 2.75f)
        assertEquals(1, r.shown)
    }

    @Test
    fun theSettingReadsBackFromItsNameAndAnUnknownNameIsAuto() {
        assertEquals(CategoryColumns.FOUR, CategoryColumns.Names.parse("four"))
        assertEquals(CategoryColumns.AUTO, CategoryColumns.Names.parse("not-a-setting"))
    }
}
