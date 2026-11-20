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

    fun tokens(packageName: String, label: String): Set<String> {
        val out = linkedSetOf<String>()
        packageName.lowercase().split('.', '_', '-').filter { it.length >= 3 && it !in NOISE }.forEach(out::add)
        label.lowercase().split(' ', '-', '_', '.', '&', '+', '/').filter { it.length >= 3 && it !in NOISE }.forEach(out::add)
        out.toList().forEach { t -> FRAGMENTS.filter { it in t && it != t }.forEach(out::add) }
        return out
    }

    // key -> folder id, for the candidates that have a clear home.
    fun suggest(placed: List<Placed>, candidates: List<Candidate>): Map<String, String> {
        // token -> folder id -> number of that folder's apps carrying it
        val carriers = HashMap<String, HashMap<String, Int>>()
        for (p in placed) for (t in tokens(p.packageName, p.label)) {
            carriers.getOrPut(t) { HashMap() }.merge(p.folderId, 1, Int::plus)
        }
        val result = LinkedHashMap<String, String>()
        for (c in candidates) {
            val votes = HashMap<String, Double>()
            for (t in tokens(c.packageName, c.label)) {
                val byFolder = carriers[t] ?: continue
                val total = byFolder.values.sum()
                if (total < MIN_CARRIERS) continue
                // A token that every folder shares says nothing: weigh it by
                // how one-sided it is.
                val best = byFolder.maxByOrNull { it.value } ?: continue
                val share = best.value.toDouble() / total
                if (share >= MIN_SHARE) votes.merge(best.key, share, Double::plus)
            }
            val top = votes.maxByOrNull { it.value } ?: continue
            val rival = votes.filterKeys { it != top.key }.values.maxOrNull() ?: 0.0
            if (top.value >= MIN_SHARE && top.value >= rival * 2) result[c.key] = top.key
        }
        return result
    }
}
