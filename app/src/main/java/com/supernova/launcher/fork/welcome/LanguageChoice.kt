package com.supernova.launcher.fork.welcome

// Which language the first screen offers as chosen, and whether leaving it changes anything.
internal object LanguageChoice {

    private const val FALLBACK = "en"

    // The app's own language if one is set already, else the phone's, else English: the same the launcher shows
    // when it follows the system. Only a language of [offered] is ever the answer.
    fun preselected(appTags: String, systemLanguage: String, offered: List<String>): String {
        val app = appTags.substringBefore(',').substringBefore('-')
        return listOf(app, systemLanguage).firstOrNull { it in offered } ?: FALLBACK
    }

    // The tag to set, or null to set nothing: left as offered, the launcher keeps following the system.
    fun toApply(preselected: String, chosen: String): String? = chosen.takeIf { it != preselected }
}
