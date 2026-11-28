package com.supernova.launcher.fork.frequent

// The steps the Frequent category takes, on a state and nothing else: no clock, no store, no Android. The store
// reads a state, hands it here with the moment, and writes back what comes out.
internal object FrequentEngine {

    // [shown] is what the tile shows now; empty means it is still collecting (or has no apps to show yet).
    class Opened(val state: FrequentState, val shown: List<String>, val warm: Boolean)

    // An app launched from the launcher.
    fun record(state: FrequentState, key: String, now: Long): FrequentState {
        val stats = state.stats + (key to FrequentScore.launched(state.stats[key], now))
        // A launch counted before the switch was ever flipped starts the clock of the warm-up too.
        val started = state.enabledAtMs == 0L
        return state.copy(
            stats = stats,
            on = state.on || started,
            enabledAtMs = if (started) now else state.enabledAtMs
        )
    }

    // The category turned on or off. Turning it on starts the warm-up's 24 hours again; nothing is dropped.
    fun setEnabled(state: FrequentState, on: Boolean, now: Long): FrequentState = when {
        on == state.on -> state
        on -> state.copy(on = true, enabledAtMs = now)
        else -> state.copy(on = false)
    }

    // The drawer was opened. What was worked out at the last opening is shown now; a top is worked out again if
    // a day has passed, and shown at the next opening. [k] is the places the tile has.
    fun open(state: FrequentState, now: Long, k: Int, eligible: Set<String>, excluded: Set<String>): Opened {
        val places = k.coerceIn(0, FrequentConfig.MAX_SLOTS)
        val usable = state.stats.filterKeys { it in eligible && it !in excluded }
        val warm = state.warm || FrequentWarmup.isWarm(usable, state.enabledAtMs, now)
        if (!warm) return Opened(state, emptyList(), false)

        val input = FrequentSelection.Input(state.stats, now, eligible, excluded)
        // The first top is only worked out at an opening and shown at the next: until then nothing is shown.
        val before = state.pending ?: state.shown
        val shown = if (before.isEmpty()) emptyList() else FrequentSelection.settle(before, places, input)
        // With nothing shown there is nothing to keep steady: it is tried again at every opening.
        val due = shown.isEmpty() || state.lastRecalcMs == 0L ||
            now - state.lastRecalcMs >= FrequentConfig.RECALC_INTERVAL_MS
        val next = if (due) FrequentSelection.recalc(shown, places, input) else null
        val moved = FrequentState(
            stats = state.stats,
            shown = shown,
            // What was pending is shown now; a new one is kept only if it differs.
            pending = next?.takeIf { it != shown },
            lastRecalcMs = if (due) now else state.lastRecalcMs,
            enabledAtMs = state.enabledAtMs,
            on = state.on,
            warm = true
        )
        return Opened(moved, shown, true)
    }

    // What the tile shows between two openings: the places as they are, without an app the user just took off
    // or one that was just removed. The place it leaves goes to the best of the rest at once.
    fun visible(state: FrequentState, now: Long, k: Int, eligible: Set<String>, excluded: Set<String>): List<String> {
        if (!state.warm || state.shown.isEmpty()) return emptyList()
        val input = FrequentSelection.Input(state.stats, now, eligible, excluded)
        return FrequentSelection.settle(state.shown, k.coerceIn(0, FrequentConfig.MAX_SLOTS), input)
    }

    // The records of apps that are no longer installed go. [installed] is every app the device has, hidden or not.
    fun prune(state: FrequentState, installed: Set<String>): FrequentState = state.copy(
        stats = state.stats.filterKeys { it in installed },
        shown = state.shown.filter { it in installed },
        pending = state.pending?.filter { it in installed }
    )
}
