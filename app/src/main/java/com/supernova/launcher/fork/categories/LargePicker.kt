package com.supernova.launcher.fork.categories

// Choosing a category's large icons by tapping apps one after another: the
// first tap is the first large icon, the second tap the second, and so on up
// to [slots]. Plain data, so what a tap does is testable without a view.
internal class LargePicker<T>(val slots: Int) {

    private val chosen = mutableListOf<T>()

    val picked: List<T> get() = chosen.toList()

    // Which large icon the next tap chooses, from 1.
    val next: Int get() = chosen.size + 1

    val isComplete: Boolean get() = chosen.size >= slots

    // Which large icon [item] is, from 1, null while it is not chosen.
    fun orderOf(item: T): Int? = chosen.indexOf(item).takeIf { it >= 0 }?.plus(1)

    // A tap on an app already chosen takes it back: the ones chosen after it move up a place. False for any other.
    fun unpick(item: T): Boolean = chosen.remove(item)

    // False for an app already chosen: one app cannot fill two slots.
    fun pick(item: T): Boolean {
        if (isComplete || item in chosen) return false
        chosen.add(item)
        return true
    }
}
