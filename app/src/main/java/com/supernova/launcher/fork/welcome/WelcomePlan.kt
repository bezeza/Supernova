package com.supernova.launcher.fork.welcome

// What of the fork's welcome is due when the launcher comes to the front.
internal object WelcomePlan {

    // Raise it when a release has news worth the card again.
    const val NEWS = 1

    enum class Step { TOUR, NEWS_CARD }

    // [firstRun]: this install went through the first run with the fork in it, so it gets the tour. Anyone else had
    // the launcher already and gets the card of what is new, once for each [NEWS]. Nothing before the first run is
    // over, and nothing once the tour has been up.
    fun due(onboardingDone: Boolean, firstRun: Boolean, tourSeen: Boolean, newsSeen: Int): Step? = when {
        !onboardingDone -> null
        firstRun -> Step.TOUR.takeIf { !tourSeen }
        newsSeen < NEWS -> Step.NEWS_CARD
        else -> null
    }
}
