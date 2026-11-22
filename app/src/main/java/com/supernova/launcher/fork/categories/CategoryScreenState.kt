package com.supernova.launcher.fork.categories

import kotlin.math.abs

// Which category's screen is open, if any, by the folder's id. Plain data, so
// what Back does and what a new list of folders does to an open screen can be
// tested without a view.
internal class CategoryScreenState {

    var openId: String? = null
        private set

    val isOpen: Boolean get() = openId != null

    fun open(id: String) {
        openId = id
    }

    // True when a screen was open and is now closed: the Back that closed it
    // is spent, and the next one is the drawer's.
    fun close(): Boolean {
        val wasOpen = isOpen
        openId = null
        return wasOpen
    }

    // After the folders changed: a category that is gone (deleted, or the
    // style left) takes its screen with it. True while the screen stays open.
    fun sync(ids: Collection<String>): Boolean {
        if (openId !in ids) openId = null
        return isOpen
    }
}

// A swipe to the right across the screen goes back, as Back does: far enough,
// quick enough, and mostly sideways so a scroll never counts.
internal object BackSwipe {
    const val MIN_DISTANCE_DP = 72f
    const val MIN_VELOCITY_DP = 500f
    private const val SIDEWAYS = 2f

    fun isBack(dx: Float, dy: Float, velocityX: Float, density: Float): Boolean =
        dx >= MIN_DISTANCE_DP * density &&
            velocityX >= MIN_VELOCITY_DP * density &&
            dx >= SIDEWAYS * abs(dy)
}
