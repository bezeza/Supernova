package com.supernova.launcher.fork.frequent

// What is kept of one app: the score as it stood at [lastLaunchMs] (it decays from there, see FrequentScore),
// and how many times it was launched from the launcher.
internal data class AppStat(val score: Double, val lastLaunchMs: Long, val launchCount: Int)

// Everything the Frequent category keeps, by app key (the drawer's hiddenKey).
internal data class FrequentState(
    val stats: Map<String, AppStat> = emptyMap(),
    // The apps the tile shows, by place. The order is the places': it changes only when an app leaves its place.
    val shown: List<String> = emptyList(),
    // The top worked out at the last opening of the drawer; it is shown from the next opening.
    val pending: List<String>? = null,
    val lastRecalcMs: Long = 0L,
    // When the category was turned on, for the warm-up's 24 hours; 0 while it never was.
    val enabledAtMs: Long = 0L,
    val on: Boolean = false,
    // Once warm, always warm: the tile does not go back to collecting.
    val warm: Boolean = false
)
