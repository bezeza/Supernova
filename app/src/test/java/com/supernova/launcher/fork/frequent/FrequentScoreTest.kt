package com.supernova.launcher.fork.frequent

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequentScoreTest {

    private val day = FrequentConfig.DAY_MS

    @Test
    fun theFirstLaunchScoresOne() {
        val stat = FrequentScore.launched(null, now = 500L)
        assertEquals(AppStat(1.0, 500L, 1), stat)
    }

    @Test
    fun theScoreHalvesEveryHalfLife() {
        val stat = AppStat(4.0, 0L, 4)
        assertEquals(4.0, FrequentScore.decayed(stat, 0L), EPS)
        assertEquals(2.0, FrequentScore.decayed(stat, 6 * day), EPS)
        assertEquals(1.0, FrequentScore.decayed(stat, 12 * day), EPS)
    }

    @Test
    fun aLaunchAddsOneToWhatIsLeft() {
        val stat = FrequentScore.launched(AppStat(2.0, 0L, 2), now = 6 * day)
        assertEquals(2.0, stat.score, EPS)
        assertEquals(6 * day, stat.lastLaunchMs)
        assertEquals(3, stat.launchCount)
    }

    @Test
    fun aClockThatWentBackDoesNotRaiseTheScore() {
        assertEquals(3.0, FrequentScore.decayed(AppStat(3.0, 1_000L, 3), now = 0L), EPS)
    }

    private companion object {
        const val EPS = 1e-9
    }
}
