package com.supernova.launcher.fork.categories

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// What is stored for a tile's cell and for the new size: an older backup opens as it was, and a newer one
// opens in an older build without a crash.
class CategoryOptionsCellsTest {

    // What the build before the mosaic read: no cells, no strip.
    @Serializable
    private class LegacyStored(
        val sort: String = "",
        val hidden: Boolean = false,
        val size: String = "",
        val large: Int = CategoryOptions.LARGE_DEFAULT
    )

    private val legacyJson = Json { ignoreUnknownKeys = true }

    private fun wideA(raw: String) =
        CategoryOptionsStore.update(raw, "A", existing = listOf("A")) { it.copy(size = CategorySize.WIDE) }

    @Test
    fun anOldBackupHasNoCells() {
        val raw = """{"A":{"size":"WIDE"},"B":{"sort":"ALPHA","size":"SMALL","large":1}}"""
        val all = CategoryOptionsStore.decode(raw)
        assertNull(all.getValue("A").col)
        assertNull(all.getValue("A").row)
        assertEquals(CategorySize.SMALL, all.getValue("B").size)
    }

    @Test
    fun anOldBackupIsNotRewrittenWhenNothingChanged() {
        val raw = """{"A":{"size":"WIDE"}}"""
        val written = CategoryOptionsStore.encode(CategoryOptionsStore.decode(raw))
        // No cell kept, so no cell written: an older build finds only the keys it knows.
        assertFalse(written.contains("col"))
        assertFalse(written.contains("row"))
        assertEquals(CategorySize.WIDE, CategoryOptionsStore.decode(written).getValue("A").size)
    }

    @Test
    fun aCellIsStoredAndReadBack() {
        val map = mapOf("A" to CategoryOptions(size = CategorySize.NORMAL, col = 2, row = 4))
        val back = CategoryOptionsStore.decode(CategoryOptionsStore.encode(map))
        assertEquals(2, back.getValue("A").col)
        assertEquals(4, back.getValue("A").row)
    }

    @Test
    fun aTileWithACellIsNotTheDefaultEvenAtTheDefaultSize() {
        assertFalse(CategoryOptions(col = 0, row = 0).isDefault)
        assertTrue(CategoryOptions().isDefault)
    }

    @Test
    fun aBackupWithCellsAndAStripOpensInAnOlderBuildWithoutACrash() {
        val raw = CategoryOptionsStore.encode(
            mapOf(
                "A" to CategoryOptions(size = CategorySize.STRIP, col = 0, row = 3),
                "B" to CategoryOptions(size = CategorySize.WIDE, large = 1, col = 0, row = 0)
            )
        )
        val old = legacyJson.decodeFromString<Map<String, LegacyStored>>(raw)
        assertEquals("STRIP", old.getValue("A").size)
        // The old build reads a size it does not know as NORMAL.
        assertEquals(CategorySize.NORMAL, CategorySize.fromName("NOT_A_SIZE"))
        assertEquals(1, old.getValue("B").large)
    }

    @Test
    fun aHalfTileSurvivesTheBackupAndAnOlderBuildReadsItAsNormal() {
        val raw = CategoryOptionsStore.encode(mapOf("A" to CategoryOptions(size = CategorySize.HALF, col = 2, row = 1)))
        val back = CategoryOptionsStore.decode(raw).getValue("A")
        assertEquals(CategorySize.HALF, back.size)
        assertEquals(2 to 1, back.col to back.row)
        // The older build knows no HALF and falls back to NORMAL on the name it finds.
        assertEquals("HALF", legacyJson.decodeFromString<Map<String, LegacyStored>>(raw).getValue("A").size)
    }

    @Test
    fun theRecentRowKeepsItsHalfWidthSizeThoughItIsTheDefaultOfACategory() {
        val raw = CategoryOptionsStore.encode(
            mapOf(CategoryOptionsStore.RECENT_KEY to CategoryOptions(size = CategorySize.NORMAL))
        )
        val back = CategoryOptionsStore.decode(raw).getValue(CategoryOptionsStore.RECENT_KEY)
        assertEquals(CategorySize.NORMAL, back.size)
        assertEquals(CategorySize.NORMAL, RecentLayout.sizeOf(back))
        // A category at its defaults still leaves no trace.
        assertEquals("", CategoryOptionsStore.encode(mapOf("A" to CategoryOptions())))
    }

    @Test
    fun theStripOfNewInstallsKeepsItsEntryWhenAnotherCategoryChanges() {
        val strip = CategoryOptions(size = CategorySize.STRIP, col = 0, row = 0)
        val raw = CategoryOptionsStore.encode(mapOf(CategoryOptionsStore.RECENT_KEY to strip))
        val updated = wideA(raw)
        val all = CategoryOptionsStore.decode(updated)
        assertEquals(CategorySize.STRIP, all.getValue(CategoryOptionsStore.RECENT_KEY).size)
        assertEquals(CategorySize.WIDE, all.getValue("A").size)
    }

    @Test
    fun aCategoryThatIsGoneStillTakesItsCellWithIt() {
        val raw = CategoryOptionsStore.encode(mapOf("Gone" to CategoryOptions(col = 1, row = 1)))
        val updated = wideA(raw)
        assertFalse(CategoryOptionsStore.decode(updated).containsKey("Gone"))
    }
}
