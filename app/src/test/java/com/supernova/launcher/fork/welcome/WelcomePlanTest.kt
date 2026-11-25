package com.supernova.launcher.fork.welcome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Who is shown the tour, who the card, and who nothing.
class WelcomePlanTest {

    private fun due(done: Boolean = true, firstRun: Boolean, tourSeen: Boolean = false, newsSeen: Int = 0) =
        WelcomePlan.due(done, firstRun, tourSeen, newsSeen)

    @Test
    fun nothingBeforeTheFirstRunIsOver() {
        assertNull(due(done = false, firstRun = true))
        assertNull(due(done = false, firstRun = false))
    }

    @Test
    fun aFreshInstallGetsTheTourOnceAndNoCard() {
        assertEquals(WelcomePlan.Step.TOUR, due(firstRun = true))
        assertNull(due(firstRun = true, tourSeen = true, newsSeen = 0))
    }

    @Test
    fun whoHadTheLauncherGetsTheCardOncePerNews() {
        assertEquals(WelcomePlan.Step.NEWS_CARD, due(firstRun = false))
        assertNull(due(firstRun = false, newsSeen = WelcomePlan.NEWS))
        assertEquals(WelcomePlan.Step.NEWS_CARD, due(firstRun = false, tourSeen = true, newsSeen = 0))
    }
}
