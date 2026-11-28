package com.supernova.launcher.fork.frequent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequentEngineTest {

    private val day = FrequentConfig.DAY_MS
    private val hour = 60L * 60 * 1000
    private val apps = setOf("a", "b", "c", "d", "e")

    // 12 launches in three apps, the moment 0 the first of them: warm at once. a is launched most.
    private fun warmState(): FrequentState {
        var state = FrequentState()
        repeat(6) { state = FrequentEngine.record(state, "a", it * hour) }
        repeat(4) { state = FrequentEngine.record(state, "b", it * hour) }
        repeat(2) { state = FrequentEngine.record(state, "c", it * hour) }
        return state
    }

    private fun open(state: FrequentState, now: Long, excluded: Set<String> = emptySet(), k: Int = 3) =
        FrequentEngine.open(state, now, k, apps, excluded)

    @Test
    fun theFirstLaunchStartsTheClockOfTheWarmUp() {
        val state = FrequentEngine.record(FrequentState(), "a", now = 777L)
        assertEquals(777L, state.enabledAtMs)
        assertTrue(state.on)
        assertEquals(1, state.stats.getValue("a").launchCount)
    }

    @Test
    fun turningItOnAgainStartsTheWarmUpAgainAndKeepsTheStats() {
        var state = FrequentEngine.record(FrequentState(), "a", now = 100L)
        state = FrequentEngine.setEnabled(state, on = false, now = 200L)
        assertFalse(state.on)
        assertEquals(100L, state.enabledAtMs)
        state = FrequentEngine.setEnabled(state, on = true, now = 900L)
        assertEquals(900L, state.enabledAtMs)
        assertEquals(1, state.stats.getValue("a").launchCount)
    }

    @Test
    fun beforeTheWarmUpTheTileHasNothingToShow() {
        val opened = open(FrequentEngine.record(FrequentState(), "a", 0L), now = hour)
        assertFalse(opened.warm)
        assertTrue(opened.shown.isEmpty())
        assertNull(opened.state.pending)
    }

    @Test
    fun theTopWorkedOutAtAnOpeningIsShownAtTheNext() {
        val first = open(warmState(), now = 10 * hour)
        assertTrue(first.warm)
        assertTrue(first.shown.isEmpty())
        assertEquals(listOf("a", "b", "c"), first.state.pending)

        val second = open(first.state, now = 11 * hour)
        assertEquals(listOf("a", "b", "c"), second.shown)
        assertNull(second.state.pending)
    }

    @Test
    fun nothingIsWorkedOutAgainWithinADay() {
        val shown = open(open(warmState(), 10 * hour).state, 11 * hour).state
        val burst = FrequentEngine.record(
            FrequentEngine.record(FrequentEngine.record(shown, "d", 12 * hour), "d", 12 * hour), "d", 12 * hour
        )
        val opened = open(burst, now = 13 * hour)
        assertEquals(listOf("a", "b", "c"), opened.shown)
        assertNull(opened.state.pending)
    }

    @Test
    fun aDayLaterANewTopIsWorkedOutButShownOnlyAtTheNextOpening() {
        val settled = open(open(warmState(), 10 * hour).state, 11 * hour).state
        // d is launched a lot: far more than c, which had two launches.
        var burst = settled
        repeat(20) { burst = FrequentEngine.record(burst, "d", 30 * hour) }

        val due = open(burst, now = 36 * hour)
        assertEquals("what is shown does not change at this opening", listOf("a", "b", "c"), due.shown)
        val pending = due.state.pending.orEmpty()
        assertTrue("d is worked in", "d" in pending)
        assertFalse("c is worked out", "c" in pending)

        val next = open(due.state, now = 37 * hour)
        assertTrue("d is in", "d" in next.shown)
        assertFalse("c is out", "c" in next.shown)
        assertEquals("a keeps its place", "a", next.shown.first())
    }

    @Test
    fun aTopThatDidNotChangeLeavesNothingPending() {
        val settled = open(open(warmState(), 10 * hour).state, 11 * hour).state
        val due = open(settled, now = 11 * hour + day)
        assertNull(due.state.pending)
        assertEquals(listOf("a", "b", "c"), due.shown)
    }

    @Test
    fun anExcludedAppIsGoneFromTheTileAtThatOpening() {
        val settled = open(open(warmState(), 10 * hour).state, 11 * hour).state
        val opened = open(settled, now = 12 * hour, excluded = setOf("a"))
        assertEquals(listOf("b", "c"), opened.shown)
    }

    @Test
    fun visibleDropsAnExcludedAppWithoutWaitingForAnOpening() {
        val settled = open(open(warmState(), 10 * hour).state, 11 * hour).state
        val shown = FrequentEngine.visible(settled, 12 * hour, 3, apps, setOf("b"))
        assertEquals(listOf("a", "c"), shown)
    }

    @Test
    fun onceWarmItStaysWarm() {
        val warm = open(warmState(), 10 * hour).state
        val without = warm.copy(stats = emptyMap())
        assertTrue(open(without, 11 * hour).warm)
    }

    @Test
    fun recordsOfAppsThatAreGoneAreDropped() {
        val pruned = FrequentEngine.prune(
            warmState().copy(shown = listOf("a", "c"), pending = listOf("a", "b")), installed = setOf("a", "b")
        )
        assertEquals(setOf("a", "b"), pruned.stats.keys)
        assertEquals(listOf("a"), pruned.shown)
        assertEquals(listOf("a", "b"), pruned.pending)
    }

    @Test
    fun theTimeWarmUpOpensTheTileAfterADayWithSixLaunches() {
        var state = FrequentState()
        repeat(6) { state = FrequentEngine.record(state, "a", it * hour) }
        assertFalse(open(state, now = 23 * hour).warm)
        assertTrue(open(state, now = 25 * hour).warm)
    }

    @Test
    fun withNothingToShowTheTopIsTriedAgainAtEveryOpening() {
        // Warm, but the only launched apps cannot be shown yet.
        val state = warmState()
        val none = FrequentEngine.open(state, 10 * hour, 3, eligible = emptySet(), excluded = emptySet())
        assertTrue(none.shown.isEmpty())
        assertNull(none.state.pending)
        val later = FrequentEngine.open(none.state, 11 * hour, 3, apps, emptySet())
        assertEquals(listOf("a", "b", "c"), later.state.pending)
    }
}
