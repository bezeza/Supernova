package com.supernova.launcher.fork.frequent

// Every number of the Frequent category, in one place. Nothing else in the package spells one out.
internal object FrequentConfig {

    const val DAY_MS: Long = 24L * 60 * 60 * 1000

    // A launch counts for half as much after this long: the score halves every H.
    const val HALF_LIFE_MS: Long = 6 * DAY_MS

    // A newcomer takes a member's place only when its score is this many times the member's.
    const val GAMMA: Double = 1.25

    // Only an app scoring at least this is a candidate for the top.
    const val MIN_SCORE: Double = 2.0

    // Warm-up: this many launches in at least this many different apps ...
    const val WARM_LAUNCHES: Int = 12
    const val WARM_APPS: Int = 3

    // ... or this long since it was turned on, with at least this many launches.
    const val WARM_AFTER_MS: Long = DAY_MS
    const val WARM_MIN_LAUNCHES: Int = 6

    // The top is worked out again at the first drawer opening after this long.
    const val RECALC_INTERVAL_MS: Long = DAY_MS

    // An app short of places is filled in from the rest only if it was launched at least this often.
    const val FILL_MIN_LAUNCHES: Int = 1

    // The most places a tile has (a 4x4 tile: 15 large icons and 4 small ones).
    const val MAX_SLOTS: Int = 19
}
