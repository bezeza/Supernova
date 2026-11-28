package com.supernova.launcher.fork.frequent

// How the pieces of a state are written as text. The store puts them in its own file; a piece that cannot be
// read is left out, so a damaged entry costs one app's record and no more.
internal object FrequentCodec {

    private const val SEP = '\t'

    private const val FIELDS = 4

    fun encodeStat(key: String, stat: AppStat): String =
        listOf(key, stat.score.toString(), stat.lastLaunchMs.toString(), stat.launchCount.toString())
            .joinToString("$SEP")

    fun decodeStat(entry: String): Pair<String, AppStat>? {
        val parts = entry.split(SEP)
        if (parts.size != FIELDS || parts[0].isEmpty()) return null
        val score = parts[1].toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
        val last = parts[2].toLongOrNull()
        val count = parts[3].toIntOrNull()?.takeIf { it >= 0 }
        return if (score != null && last != null && count != null) parts[0] to AppStat(score, last, count) else null
    }

    fun encodeStats(stats: Map<String, AppStat>): Set<String> =
        stats.mapTo(LinkedHashSet()) { encodeStat(it.key, it.value) }

    fun decodeStats(entries: Set<String>): Map<String, AppStat> = entries.mapNotNull(::decodeStat).toMap()

    // The apps of a list, one a line (an app key holds no line break).
    fun encodeList(keys: List<String>): String = keys.joinToString("\n")

    fun decodeList(raw: String?): List<String> = raw.orEmpty().split('\n').filter { it.isNotBlank() }
}
