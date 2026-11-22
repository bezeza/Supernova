package com.supernova.launcher.fork.categories

// What one Categories tile shows of a category's apps. The apps come in the
// drawer's own order, so the tile follows the sort the drawer already uses:
// the first few get a large icon each (a tap launches), the next few go into
// the small corner (a tap opens the whole category).
object CategoryLayout {

    const val LARGE_SLOTS = 3
    const val MINI_SLOTS = 4

    // [hidden] is how many apps fit nowhere on the tile.
    class Tile<T>(val large: List<T>, val mini: List<T>, val hidden: Int)

    fun <T> tile(apps: List<T>): Tile<T> {
        val large = apps.take(LARGE_SLOTS)
        val mini = apps.drop(LARGE_SLOTS).take(MINI_SLOTS)
        return Tile(large, mini, (apps.size - LARGE_SLOTS - MINI_SLOTS).coerceAtLeast(0))
    }

    // The newest installs for the full-width row over the tiles. An app with
    // no known install time is left out rather than ranked last.
    fun <T> recentlyInstalled(apps: List<T>, limit: Int, installTime: (T) -> Long): List<T> =
        apps.filter { installTime(it) > 0L }.sortedByDescending(installTime).take(limit)

    const val RECENT_LIMIT = 4
}
