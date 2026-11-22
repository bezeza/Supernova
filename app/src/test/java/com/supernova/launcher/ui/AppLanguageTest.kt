package com.supernova.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The removed Odesa language falls back to Ukrainian; no other language moves.
class AppLanguageTest {

    @Test
    fun theOdesaVariantBecomesUkrainian() {
        assertEquals("uk", AppLanguage.replacementFor("ru-Cyrl-UA-odesa"))
    }

    @Test
    fun theTagIsMatchedWhateverItsCase() {
        assertEquals("uk", AppLanguage.replacementFor("RU-cyrl-ua-ODESA"))
    }

    @Test
    fun ukrainianRussianAndEnglishStay() {
        assertNull(AppLanguage.replacementFor("uk"))
        assertNull(AppLanguage.replacementFor("ru"))
        assertNull(AppLanguage.replacementFor("en"))
    }

    @Test
    fun noLanguageSetStays() {
        assertNull(AppLanguage.replacementFor(""))
    }
}
