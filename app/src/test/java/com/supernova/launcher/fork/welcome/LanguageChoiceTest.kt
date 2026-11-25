package com.supernova.launcher.fork.welcome

import com.supernova.launcher.ui.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

// What the first screen offers as chosen, and when it changes nothing.
class LanguageChoiceTest {

    private val offered = listOf("en", "uk", "ru")

    @Test
    fun thePhonesLanguageIsOfferedElseEnglish() {
        assertEquals("uk", LanguageChoice.preselected("", "uk", offered))
        assertEquals("ru", LanguageChoice.preselected("", "ru", offered))
        assertEquals("en", LanguageChoice.preselected("", "de", offered))
    }

    @Test
    fun aLanguageSetForTheAppComesFirst() {
        assertEquals("ru", LanguageChoice.preselected("ru-RU", "uk", offered))
        assertEquals("uk", LanguageChoice.preselected("de", "uk", offered))
    }

    @Test
    fun leftAsOfferedNothingIsSet() {
        assertNull(LanguageChoice.toApply("uk", "uk"))
        assertEquals("en", LanguageChoice.toApply("uk", "en"))
    }

    @Test
    fun theScreenOffersTheLanguagesTheAppDeclares() {
        // Unit tests run in the module's folder.
        val declared = Regex("""<locale android:name="([^"]+)"""")
            .findAll(File("src/main/res/xml/locales_config.xml").readText())
            .map { it.groupValues[1] }.toList()
        assertEquals(declared, AppLanguage.choices.map { it.first })
    }
}
