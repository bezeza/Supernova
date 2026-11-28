package com.supernova.launcher.fork.frequent

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequentSelectionTest {

    // Every launch at the moment 0 and the moment asked for 0 as well, so a score reads as itself.
    private fun input(
        scores: Map<String, Double>,
        excluded: Set<String> = emptySet(),
        eligible: Set<String> = scores.keys,
        now: Long = 0L,
        counts: Map<String, Int> = emptyMap()
    ) = FrequentSelection.Input(
        scores.mapValues { (key, score) -> AppStat(score, 0L, counts[key] ?: score.toInt().coerceAtLeast(1)) },
        now, eligible, excluded
    )

    @Test
    fun anEmptyTopTakesTheBestFirst() {
        val top = FrequentSelection.recalc(emptyList(), 2, input(mapOf("a" to 5.0, "b" to 3.0, "c" to 2.0)))
        assertEquals(listOf("a", "b"), top)
    }

    @Test
    fun theSameMembersKeepTheirPlaces() {
        val top = FrequentSelection.recalc(listOf("b", "a"), 2, input(mapOf("a" to 5.0, "b" to 3.0)))
        assertEquals(listOf("b", "a"), top)
    }

    @Test
    fun aCloseNewcomerChangesNothing() {
        // 3.75 is 1.25 times 3.0: not more than that.
        val scores = mapOf("a" to 4.0, "b" to 3.0, "c" to 3.75)
        assertEquals(listOf("a", "b"), FrequentSelection.recalc(listOf("a", "b"), 2, input(scores)))
    }

    @Test
    fun aClearlyBetterNewcomerTakesTheWeakestsPlaceAndNothingElseMoves() {
        val scores = mapOf("a" to 4.0, "b" to 3.0, "c" to 6.0, "d" to 3.8)
        // b is the weaker, in the second place; d is not 1.25 times better than a.
        assertEquals(listOf("a", "c"), FrequentSelection.recalc(listOf("a", "b"), 2, input(scores)))
        // The places were the other way round: c takes the first.
        assertEquals(listOf("c", "a"), FrequentSelection.recalc(listOf("b", "a"), 2, input(scores)))
    }

    @Test
    fun aNewcomerUnderTheThresholdDisplacesNobody() {
        val scores = mapOf("a" to 0.5, "b" to 1.9)
        assertEquals(listOf("a"), FrequentSelection.recalc(listOf("a"), 1, input(scores)))
    }

    @Test
    fun aMemberThatDecayedGivesWayToARealCandidate() {
        val scores = mapOf("a" to 0.5, "b" to 2.0)
        assertEquals(listOf("b"), FrequentSelection.recalc(listOf("a"), 1, input(scores)))
    }

    @Test
    fun fewCandidatesAreFilledFromTheRestThatWasLaunched() {
        val scores = mapOf("a" to 5.0, "b" to 1.5, "c" to 1.2)
        assertEquals(listOf("a", "b", "c"), FrequentSelection.recalc(emptyList(), 3, input(scores)))
    }

    @Test
    fun anAppNeverLaunchedIsNotFilledIn() {
        val stats = FrequentSelection.Input(
            mapOf("a" to AppStat(5.0, 0L, 5), "z" to AppStat(0.0, 0L, 0)), 0L, setOf("a", "z"), emptySet()
        )
        assertEquals(listOf("a"), FrequentSelection.recalc(emptyList(), 3, stats))
    }

    @Test
    fun anExcludedAppLeavesAtOnceAndItsPlaceGoesToTheNextBest() {
        val scores = mapOf("a" to 5.0, "b" to 4.0, "c" to 3.0)
        val top = FrequentSelection.settle(listOf("a", "b"), 2, input(scores, excluded = setOf("a")))
        assertEquals(listOf("c", "b"), top)
    }

    @Test
    fun anExcludedAppIsNeverBroughtBack() {
        val scores = mapOf("a" to 50.0, "b" to 4.0)
        assertEquals(listOf("b"), FrequentSelection.recalc(emptyList(), 2, input(scores, excluded = setOf("a"))))
    }

    @Test
    fun anAppThatCannotBeShownLeavesToo() {
        val scores = mapOf("a" to 5.0, "b" to 4.0, "c" to 3.0)
        val top = FrequentSelection.settle(listOf("a", "b"), 2, input(scores, eligible = setOf("b", "c")))
        assertEquals(listOf("c", "b"), top)
    }

    @Test
    fun withNothingToFillThePlacesCloseUp() {
        val scores = mapOf("b" to 2.0, "c" to 2.0, "a" to 9.0)
        val top = FrequentSelection.settle(listOf("a", "b", "c"), 3, input(scores, excluded = setOf("a")))
        assertEquals(listOf("b", "c"), top)
    }

    @Test
    fun fewerPlacesCutTheTailAndMoreFillFromTheRest() {
        val scores = mapOf("a" to 5.0, "b" to 4.0, "c" to 3.0)
        assertEquals(listOf("a"), FrequentSelection.settle(listOf("a", "b", "c"), 1, input(scores)))
        assertEquals(listOf("a", "b", "c"), FrequentSelection.settle(listOf("a"), 3, input(scores)))
    }

    @Test
    fun theScoreIsTheOneNowNotTheOneOfTheLastLaunch() {
        val day = FrequentConfig.DAY_MS
        val stats = mapOf(
            "old" to AppStat(8.0, 0L, 8),
            "new" to AppStat(3.0, 12 * day, 3)
        )
        // At 12 days "old" is worth 2.0, below "new" at 3.0.
        val input = FrequentSelection.Input(stats, 12 * day, stats.keys, emptySet())
        assertEquals(listOf("new", "old"), FrequentSelection.recalc(emptyList(), 2, input))
    }

    @Test
    fun theSameStatsAlwaysGiveTheSameOrder() {
        val scores = mapOf("b" to 3.0, "a" to 3.0, "c" to 3.0)
        assertEquals(listOf("a", "b", "c"), FrequentSelection.recalc(emptyList(), 3, input(scores)))
    }
}
