package com.supernova.launcher.fork.categories

// How far the bottom of a list lies under what is drawn over it, in pixels, so the last row and the bar that ends
// the arranging can be kept clear. Plain numbers, so it can be tested.
internal object BottomClearance {

    // [bottom] is where the list ends; each of [tops] is where something over it begins (null: nothing there).
    // The most that reaches up into the list, never less than none.
    fun over(bottom: Int, tops: List<Int?>): Int =
        tops.filterNotNull().maxOfOrNull { bottom - it }?.coerceAtLeast(0) ?: 0

    // What the navigation bar covers of a list that ends at [bottomInWindow] in a window [windowHeight] high. Never
    // more than the bar itself: a list measured while its sheet is still below the screen is not covered by all of
    // that distance, and a margin taken from it would put the plates over the list's bottom off the screen.
    fun underNavigation(bottomInWindow: Int, windowHeight: Int, barHeight: Int): Int =
        (bottomInWindow - (windowHeight - barHeight)).coerceIn(0, barHeight.coerceAtLeast(0))
}
