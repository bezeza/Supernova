package com.supernova.launcher.data

// Puts apps that sit in no drawer folder into the folders the user has made,
// by what those folders already hold. Nothing here knows a category: a
// token of an app's package or label (a package segment such as "bank" or
// "vpn", or a word of its name) votes for the folders whose apps share it.
// A token only counts once at least two folder apps carry it, and an app
// moves only when one folder gets nearly all the votes, so a doubtful app is
// left where it is.
object FolderAutoSort {

    class Candidate(val key: String, val packageName: String, val label: String)

    // Folder members, as the folder stores them, with the package and label
    // of each one that is still installed.
    class Placed(val folderId: String, val packageName: String, val label: String)

    private val NOISE = setOf(
        "com", "org", "net", "app", "apps", "android", "mobile", "free", "pro", "lite",
        "io", "ua", "ru", "www", "the", "and", "for", "main", "client", "release",
        "google", "ios", "plus", "new", "dev", "debug", "official", "my"
    )

    // Words that live inside longer names ("protonvpn", "privatbank"): a token
    // that holds one also votes as that word.
    private val FRAGMENTS = listOf(
        "bank", "vpn", "pdf", "video", "music", "photo", "game", "watch", "icon", "wallet",
        "pay", "mail", "torrent", "radio", "player", "camera", "wallpaper", "keyboard",
        "browser", "launcher", "reader", "translate", "weather", "calendar", "clock"
    )

    // Share of the votes a folder needs, and how many carriers a token needs.
    private const val MIN_SHARE = 0.75
    private const val MIN_CARRIERS = 2

    // The shortest word that counts as a token.
    private const val MIN_TOKEN_LENGTH = 3

    // How many times the winner's votes must beat the runner-up's.
    private const val LEAD_FACTOR = 2

    private val PACKAGE_SEPARATORS = Regex("[._-]")
    private val LABEL_SEPARATORS = Regex("[ ._&+/-]")

    private fun words(text: String, separators: Regex): List<String> =
        text.lowercase().split(separators).filter { it.length >= MIN_TOKEN_LENGTH && it !in NOISE }

    fun tokens(packageName: String, label: String): Set<String> {
        val out = linkedSetOf<String>()
        out.addAll(words(packageName, PACKAGE_SEPARATORS))
        out.addAll(words(label, LABEL_SEPARATORS))
        out.toList().forEach { t -> FRAGMENTS.filter { it in t && it != t }.forEach(out::add) }
        return out
    }

    // token -> folder id -> number of that folder's apps carrying it
    private fun carriersOf(placed: List<Placed>): Map<String, Map<String, Int>> {
        val carriers = HashMap<String, HashMap<String, Int>>()
        for (p in placed) for (t in tokens(p.packageName, p.label)) {
            carriers.getOrPut(t) { HashMap() }.merge(p.folderId, 1, Int::plus)
        }
        return carriers
    }

    // The folder one token points at, and how one-sided that is. A token that
    // every folder shares says nothing, so it is weighed by that share.
    private fun voteOf(byFolder: Map<String, Int>?): Pair<String, Double>? {
        val total = byFolder?.values?.sum() ?: 0
        val best = if (total >= MIN_CARRIERS) byFolder?.maxByOrNull { it.value } else null
        val share = if (best != null) best.value.toDouble() / total else 0.0
        return if (best != null && share >= MIN_SHARE) best.key to share else null
    }

    // The folder a candidate belongs to, when one folder clearly leads.
    private fun homeOf(candidate: Candidate, carriers: Map<String, Map<String, Int>>): String? {
        val votes = HashMap<String, Double>()
        for (t in tokens(candidate.packageName, candidate.label)) {
            voteOf(carriers[t])?.let { (folder, share) -> votes.merge(folder, share, Double::plus) }
        }
        val top = votes.maxByOrNull { it.value }
        val rival = votes.filterKeys { it != top?.key }.values.maxOrNull() ?: 0.0
        val clear = top != null && top.value >= MIN_SHARE && top.value >= rival * LEAD_FACTOR
        return if (clear) top?.key else null
    }

    // key -> folder id, for the candidates that have a clear home.
    fun suggest(placed: List<Placed>, candidates: List<Candidate>): Map<String, String> {
        val carriers = carriersOf(placed)
        val result = LinkedHashMap<String, String>()
        for (c in candidates) {
            homeOf(c, carriers)?.let { result[c.key] = it }
        }
        return result
    }
}
