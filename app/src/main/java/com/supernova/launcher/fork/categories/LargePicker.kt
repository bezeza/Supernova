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

    // False for an app already chosen: one app cannot fill two slots.
    fun pick(item: T): Boolean {
        if (isComplete || item in chosen) return false
        chosen.add(item)
        return true
    }
}
