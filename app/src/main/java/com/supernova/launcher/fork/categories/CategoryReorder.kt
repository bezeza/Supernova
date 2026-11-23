package com.supernova.launcher.fork.categories

// The order of the folders when tiles are dragged. The tiles show only some of
// the folders: one taken off them, one with nothing in it and a folder of
// another kind sit in the stored order but have no tile. They keep their
// places, and the shown folders fill the places the shown ones held, in the
// order the tiles now have. Plain, so it can be tested with strings.
internal object CategoryReorder {

    // [all] is every folder id in the stored order, [shown] the ids of the
    // tiles in their new order. An id of [shown] that is not in [all] is dropped.
    fun merged(all: List<String>, shown: List<String>): List<String> {
        val known = shown.filter { it in all }.distinct()
        val slots = all.indices.filter { all[it] in known }
        val queue = ArrayDeque(known)
        return all.mapIndexed { index, id -> if (index in slots) queue.removeFirst() else id }
    }

    // [list] with the item at [from] taken out and put at [to].
    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val out = list.toMutableList()
        out.add(to, out.removeAt(from))
        return out
    }
}
