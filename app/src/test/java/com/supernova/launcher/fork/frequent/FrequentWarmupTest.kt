package com.supernova.launcher.fork.frequent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequentWarmupTest {

    private val day = FrequentConfig.DAY_MS

    private fun stats(vararg counts: Int): Map<String, AppStat> =
        counts.withIndex().associate { (i, n) -> "app$i" to AppStat(n.toDouble(), 0L, n) }

    @Test
    fun twelveLaunchesInThreeAppsWarmItUp() {
        assertTrue(FrequentWarmup.isWarm(stats(4, 4, 4), enabledAtMs = 1L, now = 2L))
    }

    @Test
    fun twelveLaunchesInTwoAppsDoNot() {
        assertFalse(FrequentWarmup.isWarm(stats(6, 6), enabledAtMs = 1L, now = 2L))
    }

    @Test
    fun elevenLaunchesInThreeAppsDoNot() {
        assertFalse(FrequentWarmup.isWarm(stats(4, 4, 3), enabledAtMs = 1L, now = 2L))
    }

    @Test
    fun aDayAndSixLaunchesWarmItUpWhateverTheApps() {
        assertTrue(FrequentWarmup.isWarm(stats(6), enabledAtMs = 1_000L, now = 1_000L + day))
    }

    @Test
    fun aDayAndFiveLaunchesDoNot() {
        assertFalse(FrequentWarmup.isWarm(stats(5), enabledAtMs = 1_000L, now = 1_000L + day))
    }

    @Test
    fun lessThanADayAndSixLaunchesDoNot() {
        assertFalse(FrequentWarmup.isWarm(stats(6), enabledAtMs = 1_000L, now = 1_000L + day - 1))
    }

    @Test
    fun withoutAMomentOfTurningOnTimeCountsForNothing() {
        assertFalse(FrequentWarmup.isWarm(stats(6), enabledAtMs = 0L, now = 100 * day))
    }
}
