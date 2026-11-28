package com.supernova.launcher.fork.frequent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The top of the Frequent category as the drawer holds it: read from the store once, worked out as the drawer
// opens (the store does it, see FrequentEngine.open) and shown from then on. [onChanged] is told when it moved.
internal class FrequentTop(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit
) {

    var state: FrequentState = FrequentState()
        private set

    private var requested = false
    private var fresh = false

    // The tile has nothing to show yet because the statistics are still being collected (or the first top is
    // worked out and waits for the next opening).
    val collecting: Boolean get() = !state.warm || state.pending != null

    // Reads what the store holds, once. An opening that was worked out meanwhile is newer and is kept.
    fun load() {
        if (requested) return
        requested = true
        scope.launch {
            val read = FrequentStore.read(context)
            if (!fresh) state = read
            onChanged()
        }
    }

    // The drawer is opening: what was worked out at the last opening is shown, and a new top is worked out.
    fun opened(places: Int, installed: Set<String>, eligible: Set<String>, excluded: Set<String>) {
        scope.launch {
            state = FrequentStore.open(context, places, installed, eligible, excluded).state
            fresh = true
            onChanged()
        }
    }

    // The apps to show now, by place: the ones of the last opening, without any the user has taken off since.
    fun shown(places: Int, eligible: Set<String>, excluded: Set<String>): List<String> =
        FrequentEngine.visible(state, FrequentClock.now(context), places, eligible, excluded)
}
