package com.supernova.launcher.fork.categories

// What one Categories tile shows of a category's apps. The apps come in the
// drawer's own order, so the tile follows the sort the drawer already uses:
// the first few get a large icon each (a tap launches), the next few go into
// the small corner (a tap opens the whole category).
object CategoryLayout {

    const val LARGE_SLOTS = 3

    // The most large icons any tile has: a double tile's seven.
    const val LARGE_MAX = 7
    const val MINI_SLOTS = 4
    const val MINI_MAX = 12

    // [hidden] is how many apps fit nowhere on the tile.
    class Tile<T>(val large: List<T>, val mini: List<T>, val hidden: Int)

    fun <T> tile(apps: List<T>): Tile<T> = tile(apps, TileSlots.normal(LARGE_SLOTS))

    // The apps in a layout's slots: the first ones in the large slots, the next in the small ones.
    internal fun <T> tile(apps: List<T>, slots: List<TileSlots.Slot>): Tile<T> {
        val largeSlots = TileSlots.largeCount(slots)
        val miniSlots = TileSlots.miniCount(slots)
        val large = apps.take(largeSlots)
        val mini = apps.drop(largeSlots).take(miniSlots)
        return Tile(large, mini, (apps.size - largeSlots - miniSlots).coerceAtLeast(0))
    }

    // The newest installs for the full-width row over the tiles. An app with
    // no known install time is left out rather than ranked last.
    fun <T> recentlyInstalled(apps: List<T>, limit: Int, installTime: (T) -> Long): List<T> =
        apps.filter { installTime(it) > 0L }.sortedByDescending(installTime).take(limit)

    const val RECENT_MIN = 2
    const val RECENT_DEFAULT = 4
    const val RECENT_MAX = 8
}
