package com.supernova.launcher.fork.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// The fork's reports and source links lead to the fork, and its change list is told in all three languages.
class ForkAboutTest {

    // Unit tests run in the module's folder.
    private fun string(folder: String, name: String): String {
        val xml = File("src/main/res/$folder").listFiles().orEmpty()
            .filter { it.name.startsWith("strings") }
            .joinToString("\n") { it.readText() }
        return Regex("""<string name="$name"[^>]*>(.*?)</string>""").find(xml)!!.groupValues[1]
    }

    @Test
    fun sourceAndReportsLeadToTheFork() {
        assertEquals(ForkAbout.REPOSITORY, string("values", "about_source_url"))
    }

    @Test
    fun theIssueFormSendsNobodyToTheOriginal() {
        val forms = File("../.github/ISSUE_TEMPLATE").listFiles().orEmpty()
        assertTrue(forms.isNotEmpty())
        forms.forEach { assertFalse(it.name, it.readText().contains("tanujnotes")) }
    }

    @Test
    fun theChangeListHasTheSameLinesInEveryLanguage() {
        val counts = listOf("values", "values-uk", "values-ru")
            .map { string(it, "fork_changes").split("\\n").size }
        assertEquals(listOf(counts[0], counts[0], counts[0]), counts)
        assertTrue(counts[0] > 1)
    }
}
