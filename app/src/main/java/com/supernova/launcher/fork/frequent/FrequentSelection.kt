package com.supernova.launcher.fork.frequent

// Who is in the top. Plain, so it can be tested with strings for apps.
//
// The places are kept: an app that stays keeps its place, and a newcomer takes the place of the one that left.
// The order is never worked out again, so while the membership is the same the icons stand where they stood.
internal object FrequentSelection {

    // What the choice is made from: the stats, the moment, the apps that may be shown (installed, not hidden)
    // and the ones the user took off the tile, which always win over the score.
    class Input(
        val stats: Map<String, AppStat>,
        val now: Long,
        val eligible: Set<String>,
        val excluded: Set<String>
    ) {
        fun usable(key: String): Boolean = key in eligible && key !in excluded

        fun score(key: String): Double = stats[key]?.let { FrequentScore.decayed(it, now) } ?: 0.0
    }

    private class Ranked(val key: String, val score: Double, val lastMs: Long)

    // Only what is left of [slots] once the apps that cannot be shown are gone, their places filled by the best
    // of the rest. Nothing else moves: this is what a drawer opening does between two recalculations.
    fun settle(slots: List<String>, k: Int, input: Input): List<String> = fill(vacate(slots, k, input), k, input)

    // The top worked out again: [settle], then a newcomer takes the place of the weakest member only if it is a
    // candidate (a score of at least MIN_SCORE, so a stray launch pushes nobody out) and its score is more than
    // GAMMA times the member's (so an old launch holds no place against a real candidate, and a close call changes
    // nothing). An app under MIN_SCORE comes in only to fill a place that is empty.
    fun recalc(slots: List<String>, k: Int, input: Input): List<String> {
        val top = settle(slots, k, input).toMutableList()
        repeat(k) {
            if (!swapOnce(top, input)) return top
        }
        return top
    }

    private fun swapOnce(top: MutableList<String>, input: Input): Boolean {
        val weakestAt = top.indices.minWithOrNull(
            compareBy<Int>({ input.score(top[it]) }, { input.stats[top[it]]?.lastLaunchMs ?: 0L }, { top[it] })
        )
        val members = top.toSet()
        val challenger = ranked(input).firstOrNull { it.key !in members && it.score >= FrequentConfig.MIN_SCORE }
        if (weakestAt != null && challenger != null &&
            challenger.score > FrequentConfig.GAMMA * input.score(top[weakestAt])
        ) {
            top[weakestAt] = challenger.key
            return true
        }
        return false
    }

    // The places up to [k], an app that cannot be shown (or is there twice) leaving its place empty (null).
    private fun vacate(slots: List<String>, k: Int, input: Input): List<String?> {
        val seen = HashSet<String>()
        return slots.take(k).map { key -> key.takeIf { input.usable(it) && seen.add(it) } }
    }

    // The empty places and the ones up to [k] that were never there, taken by the apps outside, best first. A
    // place nothing is left for goes: the ones after it close up.
    private fun fill(slots: List<String?>, k: Int, input: Input): List<String> {
        val places = slots.toMutableList()
        while (places.size < k) places.add(null)
        val members = places.filterNotNull().toSet()
        val queue = ArrayDeque(ranked(input).filter { it.key !in members })
        return places.mapNotNull { it ?: queue.removeFirstOrNull()?.key }
    }

    // Every app that can be shown and was launched enough, best first: by score now, then the later launch, then
    // the key, so the same stats always give the same order.
    private fun ranked(input: Input): List<Ranked> = input.stats
        .filter { (key, stat) -> input.usable(key) && stat.launchCount >= FrequentConfig.FILL_MIN_LAUNCHES }
        .map { (key, stat) -> Ranked(key, input.score(key), stat.lastLaunchMs) }
        .sortedWith(compareByDescending<Ranked> { it.score }.thenByDescending { it.lastMs }.thenBy { it.key })
}
