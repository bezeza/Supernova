package com.supernova.launcher.search

// Label matching with the retries AppMatcher does not make, for a query and
// a name written in different ways. The plain match goes first; when it finds
// nothing the query is tried as another keyboard layout ("мшиук" finds Viber,
// "ntktuhfv" finds Телеграм, same length, so the bolded ranges stay valid),
// as the Latin brand of a spoken name ("вайбер", "телеграм"), and in
// transliteration. A name in Cyrillic is also transliterated; a match found
// that way can't point at letters of the real name, so it has no ranges.
object AppQuery {

    fun match(label: String, query: String): AppMatcher.Match? {
        val q = query.trim().lowercase()
        return if (q.isEmpty()) null else AppMatcher.match(label, q) ?: retried(label, q)
    }

    private fun retried(label: String, q: String): AppMatcher.Match? {
        val others = (listOf(swapLayout(q)) + brandQueries(q)).filter { it != q }.distinct()
        return others.firstNotNullOfOrNull { AppMatcher.match(label, it) }
            ?: viaTransliteratedName(label, listOf(q) + others)
    }

    private fun viaTransliteratedName(label: String, queries: List<String>): AppMatcher.Match? {
        val name = label.lowercase()
        val latin = queries.filter { query -> query.none { it in CYRILLIC } }
        val names = if (name.any { it in CYRILLIC }) transliterate(name) else emptySet()
        val found = names.firstNotNullOfOrNull { n -> latin.firstNotNullOfOrNull { AppMatcher.match(n, it) } }
        return found?.let { AppMatcher.Match(it.tier, emptyList()) }
    }

    // The brands a spoken name stands for, then the query in Latin letters.
    private fun brandQueries(q: String): List<String> {
        val brands = if (q.length >= ALIAS_MIN_CHARS) {
            ALIASES.filter { (spoken, _) -> spoken.startsWith(q) }.map { it.second }
        } else {
            emptyList()
        }
        return brands + if (q.any { it in CYRILLIC }) transliterate(q) else emptySet()
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

    // Two ways of spelling Cyrillic in Latin: the usual one, and another that
    // swaps и/й and х/ц, so both "вайбер" -> "vayber" and "vaiber".
    private val USUAL: Map<Char, String> = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'і' to "i", 'ї' to "yi", 'й' to "y", 'к' to "k", 'л' to "l",
        'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y",
        'ь' to "", 'э' to "e", 'є' to "ye", 'ю' to "yu", 'я' to "ya"
    )
    private val OTHER: Map<Char, String> =
        USUAL + mapOf('и' to "y", 'й' to "i", 'х' to "kh", 'ц' to "c", 'ы' to "y", 'я' to "ia", 'ю' to "iu")

    private fun transliterate(text: String): Set<String> =
        setOf(USUAL, OTHER).mapTo(linkedSetOf()) { m -> text.map { m[it] ?: it.toString() }.joinToString("") }

    private const val LATIN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./"
    private const val CYR_UK = "йцукенгшщзхїфівапролджєячсмитьбю."
    private const val CYR_RU = "йцукенгшщзхъфывапролджэячсмитьбю."

    private val toCyrillic: Map<Char, Char> = LATIN.zip(CYR_UK).toMap()
    private val toLatin: Map<Char, Char> = (CYR_UK.zip(LATIN) + CYR_RU.zip(LATIN)).toMap()

    // Same query typed with the other keyboard layout.
    private fun swapLayout(q: String): String {
        val map = if (q.any { it in toLatin }) toLatin else toCyrillic
        return q.map { map[it] ?: it }.joinToString("")
    }
}
