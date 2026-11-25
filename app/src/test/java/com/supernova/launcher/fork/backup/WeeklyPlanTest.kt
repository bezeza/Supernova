package com.supernova.launcher.fork.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// When a copy is due and which files may be deleted: never one the launcher did not name.
class WeeklyPlanTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Test
    fun dueWhenNeverMadeOrAWeekOld() {
        assertTrue(WeeklyPlan.isDue(0, now))
        assertFalse(WeeklyPlan.isDue(now - 6 * day, now))
        assertTrue(WeeklyPlan.isDue(now - 7 * day, now))
    }

    @Test
    fun aClockSetBackALittleWaitsAndFarBackDoesNot() {
        assertFalse(WeeklyPlan.isDue(now + day, now))
        assertTrue(WeeklyPlan.isDue(now + 30 * day, now))
    }

    private fun folder(day: Int) = "supernova-weekly-2026-11-0$day-120000.json"

    @Test
    fun theNewestFourStayInTheFolder() {
        val names = (1..6).map(::folder)
        assertEquals(listOf(folder(2), folder(1)), WeeklyPlan.surplusInFolder(names, folder(6)))
    }

    @Test
    fun othersFilesAreNeverTouched() {
        val others = listOf(
            "backup.json", "supernova-weekly-notes.json", "supernova-weekly-2026-11-01-120000.json.bak",
            "supernova-weekly-2026-11-01-120000 (1).json", "layout-1.json"
        )
        assertEquals(emptyList<String>(), WeeklyPlan.surplusInFolder(others + (1..4).map(::folder), folder(4)))
    }

    @Test
    fun theOneJustWrittenStaysEvenWhenItsDateIsTheOldest() {
        val names = (1..6).map(::folder)
        assertFalse(folder(1) in WeeklyPlan.surplusInFolder(names, folder(1)))
    }

    @Test
    fun theNamesMadeAreTheNamesRecognised() {
        val made = WeeklyPlan.folderName(now)
        val older = (1..4).map { WeeklyPlan.folderName(now - it * 7 * day) }
        assertEquals(listOf(older.last()), WeeklyPlan.surplusInFolder(older + made, made))
        val local = WeeklyPlan.localName(now)
        val locals = (1..4).map { WeeklyPlan.localName(now - it * 7 * day) }
        assertEquals(listOf(locals.last()), WeeklyPlan.surplusLocal(locals + local, local))
    }
}
