package com.supernova.launcher.search

// Label matching shared by the drawer filter, the search window and card
// search. It is forgiving in three ways beyond a plain prefix, and the
// bolding shows each: a query may start at any
// word ("cal" finds Google Calendar), may run across word starts ("pla"
// finds Pro Launcher as P + La, "ym" YouTube Music), and may be one typo
// off ("pla" finds Olauncher). Tiers rank the results; ranges say which
// letters to embolden.
object AppMatcher {

    // Lower tier = better. Ranges are char indices into the label.
    class Match(val tier: Int, val ranges: List<IntRange>)

    const val TIER_PREFIX = 0
    const val TIER_WORD_PREFIX = 1
    const val TIER_SUBSTRING = 2
    // A run across word starts.
    const val TIER_LOOSE = 3
    // One typo off a word start.
    const val TIER_TYPO = 4

    fun matches(label: String, query: String): Boolean = match(label, query) != null

    // A filtered list (drawer, Select apps): typo matches only when nothing
    // matches better, so "calc" lists Calculator without Calendar.
    fun <T> filter(items: List<T>, query: String, label: (T) -> String): List<T> {
        val found = items.mapNotNull { item -> match(label(item), query)?.let { item to it.tier } }
        val exact = found.filter { it.second < TIER_TYPO }
        return (exact.ifEmpty { found }).map { it.first }
    }

    // A query typed with the wrong keyboard layout (Ukrainian or Russian
    // letters on the Latin keys, or the other way round) is retried as the
    // other layout, so "мшиук" finds Viber and "ntktuhfv" finds Телеграм.
    // Same length, so the bolded ranges stay valid.
    fun match(label: String, query: String): Match? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        matchExact(label, q)?.let { return it }
        val swapped = swapLayout(q)
        if (swapped != q) matchExact(label, swapped)?.let { return it }
        return matchTransliterated(label, q)
    }

    // Last resorts, for a query and a name written in different alphabets:
    // a spoken name ("вайбер", "телеграм") read as the Latin brand, and
    // either side transliterated. A match on the name itself keeps its
    // bolding; one found through a transliterated name can't point at
    // letters of the real one, so it has no ranges.
    private fun matchTransliterated(label: String, q: String): Match? {
        val queries = linkedSetOf<String>()
        ALIASES.forEach { (spoken, brand) -> if (q.length >= ALIAS_MIN_CHARS && spoken.startsWith(q)) queries.add(brand) }
        if (q.any { it in CYRILLIC }) queries.addAll(transliterate(q))
        for (v in queries) matchExact(label, v)?.let { return it }
        val l = label.lowercase()
        if (l.any { it in CYRILLIC }) {
            val names = transliterate(l)
            val all = linkedSetOf(q, swapLayout(q)).also { set -> set.addAll(queries) }
            for (name in names) for (v in all) {
                if (v.any { it in CYRILLIC }) continue
                matchExact(name, v)?.let { return Match(it.tier, emptyList()) }
            }
        }
        return null
    }

    private const val ALIAS_MIN_CHARS = 3
    private const val CYRILLIC_LETTERS = "абвгґдеёжзиіїйклмнопрстуфхцчшщъыьэєюя"
    private val CYRILLIC: Set<Char> = CYRILLIC_LETTERS.toSet()

    // Spoken (Cyrillic) names of apps people search for, and the brand as it
    // is written on the icon.
    private val ALIASES: List<Pair<String, String>> = listOf(
        "вайбер" to "viber", "вибер" to "viber", "ватсап" to "whatsapp", "вотсап" to "whatsapp",
        "ватсапп" to "whatsapp", "телеграм" to "telegram", "телега" to "telegram",
        "инстаграм" to "instagram", "інстаграм" to "instagram", "инста" to "instagram",
        "фейсбук" to "facebook", "фэйсбук" to "facebook", "мессенджер" to "messenger",
        "ютуб" to "youtube", "ютьюб" to "youtube", "гугл" to "google", "гугль" to "google",
        "хром" to "chrome", "твиттер" to "twitter", "тикток" to "tiktok", "тік ток" to "tiktok",
        "дискорд" to "discord", "спотифай" to "spotify", "нетфликс" to "netflix",
        "сигнал" to "signal", "скайп" to "skype", "зум" to "zoom", "яндекс" to "yandex",
        "вконтакте" to "vk", "одноклассники" to "ok", "тимс" to "teams", "джимейл" to "gmail",
        "гмейл" to "gmail", "почта" to "mail", "карты" to "maps", "мапс" to "maps",
        "приват" to "privat", "монобанк" to "mono", "моно" to "mono", "дія" to "diia", "дия" to "diia",
        "анидеск" to "anydesk", "тор" to "tor", "вайфай" to "wifi"
    )

    // Two ways of spelling Cyrillic in Latin: the usual one, and the other
    // that swaps и/й and х/ц, so both "вайбер" -> "vayber" and "vaiber".
    private fun transliterate(text: String): Set<String> {
        val a = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
            'ж' to "zh", 'з' to "z", 'и' to "i", 'і' to "i", 'ї' to "yi", 'й' to "y", 'к' to "k", 'л' to "l",
            'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
            'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y",
            'ь' to "", 'э' to "e", 'є' to "ye", 'ю' to "yu", 'я' to "ya"
        )
        val b = a + mapOf('и' to "y", 'й' to "i", 'х' to "kh", 'ц' to "c", 'ы' to "y", 'я' to "ia", 'ю' to "iu")
        return setOf(a, b).mapTo(linkedSetOf()) { m -> text.map { m[it] ?: it.toString() }.joinToString("") }
    }

    private const val LATIN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./"
    private const val CYR_UK = "йцукенгшщзхїфівапролджєячсмитьбю."
    private const val CYR_RU = "йцукенгшщзхъфывапролджэячсмитьбю."

    private val toCyrillic: Map<Char, Char> = LATIN.zip(CYR_UK).toMap()
    private val toLatin: Map<Char, Char> =
        (CYR_UK.zip(LATIN) + CYR_RU.zip(LATIN)).toMap()

    private fun swapLayout(q: String): String {
        val cyrillic = q.any { it in toLatin }
        val map = if (cyrillic) toLatin else toCyrillic
        return q.map { map[it] ?: it }.joinToString("")
    }

    private fun matchExact(label: String, q: String): Match? {
        val l = label.lowercase()
        if (l.startsWith(q)) return Match(TIER_PREFIX, listOf(0 until q.length))
        val starts = wordStarts(l)
        for (s in starts) {
            if (s > 0 && l.startsWith(q, s)) return Match(TIER_WORD_PREFIX, listOf(s until s + q.length))
        }
        val at = l.indexOf(q)
        if (at >= 0) return Match(TIER_SUBSTRING, listOf(at until at + q.length))
        runsFrom(l, starts, 0, q, 0)?.let { return Match(TIER_LOOSE, it) }
        if (q.length >= TYPO_MIN_CHARS) oneTypo(l, starts, q)?.let { return Match(TIER_TYPO, listOf(it)) }
        return null
    }

    private fun isSeparator(c: Char): Boolean = c == ' ' || c == '-' || c == '_' || c == '.' || c == '&' || c == '+' || c == '/'

    private fun wordStarts(l: String): List<Int> {
        val starts = ArrayList<Int>(4)
        for (i in l.indices) {
            if (!isSeparator(l[i]) && (i == 0 || isSeparator(l[i - 1]))) starts.add(i)
        }
        return starts
    }

    // Each query letter is spent either continuing the current run or at the
    // start of a later word. Runs are tried longest first and backtrack, so
    // "pla" still finds "Plx Launcher" as P + La.
    private fun runsFrom(l: String, starts: List<Int>, from: Int, q: String, qi: Int): List<IntRange>? {
        if (qi == q.length) return emptyList()
        for (k in from until starts.size) {
            val s = starts[k]
            if (l[s] != q[qi]) continue
            var max = 0
            while (s + max < l.length && qi + max < q.length &&
                l[s + max] == q[qi + max] && (max == 0 || !isSeparator(l[s + max]))
            ) {
                max++
            }
            for (len in max downTo 1) {
                val rest = runsFrom(l, starts, k + 1, q, qi + len) ?: continue
                return listOf(s until s + len) + rest
            }
        }
        return null
    }

    // One edit against a word-start prefix: a wrong letter, two letters
    // swapped, one letter too many, or one letter missing.
    private fun oneTypo(l: String, starts: List<Int>, q: String): IntRange? {
        for (s in starts) {
            val n = q.length
            if (l.length - s >= n) {
                var mismatches = 0
                for (i in 0 until n) if (l[s + i] != q[i]) mismatches++
                if (mismatches == 1) return s until s + n
                for (i in 0 until n - 1) {
                    if (q[i] == l[s + i + 1] && q[i + 1] == l[s + i] &&
                        l.regionMatches(s, q, 0, i) && l.regionMatches(s + i + 2, q, i + 2, n - i - 2)
                    ) {
                        return s until s + n
                    }
                }
            }
            // Query has a letter too many - only once what is left is still
            // three letters, or "pla" would find every "La..." word.
            if (n > TYPO_MIN_CHARS) {
                for (i in 0 until n) {
                    if (l.regionMatches(s, q, 0, i) && l.regionMatches(s + i, q, i + 1, n - i - 1)) {
                        return s until s + n - 1
                    }
                }
            }
            // Label has a letter the query skipped.
            if (l.length - s >= n + 1) {
                for (i in 1 until n) {
                    if (l.regionMatches(s, q, 0, i) && l.regionMatches(s + i + 1, q, i, n - i)) {
                        return s until s + n + 1
                    }
                }
            }
        }
        return null
    }

    private const val TYPO_MIN_CHARS = 3
}
