package com.supernova.launcher.fork.categories

// What the drawer lends for ordering: by name, by launches, and the key that
// ties an app to the folder's members.
internal class Orderings<T>(
    val alphabetical: (List<T>) -> List<T>,
    val byUse: (List<T>) -> List<T>,
    val keyOf: (T) -> String
)

// A category's apps in the order its option asks for. Plain, so it can be
// tested with strings for apps.
internal object CategoryOrder {

    fun <T> apply(apps: List<T>, sort: CategorySort, members: List<String>, orderings: Orderings<T>): List<T> =
        when (sort) {
            CategorySort.DRAWER -> apps
            CategorySort.MANUAL -> manual(apps, members, orderings.keyOf)
            CategorySort.ALPHA -> orderings.alphabetical(apps)
            CategorySort.FREQUENCY -> orderings.byUse(apps)
        }

    // The folder's members as stored; an app it does not list goes last, in the order it came.
    fun <T> manual(apps: List<T>, members: List<String>, keyOf: (T) -> String): List<T> {
        val rank = members.withIndex().associate { it.value to it.index }
        return apps.sortedBy { rank[keyOf(it)] ?: Int.MAX_VALUE }
    }
}
