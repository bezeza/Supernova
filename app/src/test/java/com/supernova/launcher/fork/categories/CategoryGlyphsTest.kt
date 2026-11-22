package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Each approved category has its own icon, found by name.
class CategoryGlyphsTest {

    private val approved = listOf(
        "Соцмережі", "Месенджери", "Банки й фінанси", "Держава і зв’язок", "Покупки",
        "Транспорт", "Кіно і відео", "Музика і аудіо", "Фото і монтаж", "Ігри", "Робота і пошта",
        "ШІ", "Медицина", "Годинники", "Теми і віджети", "Мережа і VPN", "Інструменти", "Системні",
        "Навчання", "Інше"
    )

    @Test
    fun everyApprovedCategoryHasAnIcon() {
        approved.forEach { assertNotNull(it, CategoryGlyphs.resFor(it)) }
    }

    @Test
    fun theIconsAreAllDifferent() {
        assertEquals(approved.size, approved.mapNotNull { CategoryGlyphs.resFor(it) }.toSet().size)
    }

    @Test
    fun aSpaceAroundTheNameIsIgnored() {
        assertEquals(CategoryGlyphs.resFor("Ігри"), CategoryGlyphs.resFor("  Ігри "))
    }

    @Test
    fun anotherNameHasNone() {
        assertNull(CategoryGlyphs.resFor("Моя тека"))
    }
}
