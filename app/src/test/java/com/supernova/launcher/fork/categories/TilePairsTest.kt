package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// Small tiles go two to a column: pairs of small ones, a lone small one left alone.
class TilePairsTest {

    // "s1" is a small tile, anything else is not; a pair of "s1" and "s2" is "(s1+s2)".
    private fun group(vararg items: String): List<String> =
        TilePairs.group(items.toList(), { it.startsWith("s") }) { a, b -> "($a+$b)" }

    private fun split(vararg items: String): List<String> =
        TilePairs.split(items.toList()) { if (it.startsWith("(")) it.trim('(', ')').split("+") else null }

    @Test
    fun twoSmallOnesInARowArePaired() {
        assertEquals(listOf("(s1+s2)"), group("s1", "s2"))
    }

    @Test
    fun fourSmallOnesAreTwoPairs() {
        assertEquals(listOf("(s1+s2)", "(s3+s4)"), group("s1", "s2", "s3", "s4"))
    }

    @Test
    fun anOddSmallOneIsLeftAloneAtTheEnd() {
        assertEquals(listOf("(s1+s2)", "s3"), group("s1", "s2", "s3"))
    }

    @Test
    fun aLoneSmallOneBesideBigOnesStaysAlone() {
        assertEquals(listOf("n1", "s1", "n2"), group("n1", "s1", "n2"))
    }

    @Test
    fun smallOnesApartAreNotPairedAcrossABiggerTile() {
        assertEquals(listOf("s1", "n1", "s2"), group("s1", "n1", "s2"))
    }

    @Test
    fun theOrderIsKept() {
        assertEquals(listOf("n1", "(s1+s2)", "n2", "(s3+s4)", "s5"), group("n1", "s1", "s2", "n2", "s3", "s4", "s5"))
    }

    @Test
    fun noItemsNoPairs() {
        assertEquals(emptyList<String>(), group())
    }

    @Test
    fun splittingTakesThePairsApartInOrder() {
        assertEquals(listOf("n1", "s1", "s2", "s3"), split("n1", "(s1+s2)", "s3"))
    }

    @Test
    fun groupingWhatWasSplitGivesTheSameRows() {
        val rows = listOf("n1", "(s1+s2)", "s3")
        assertEquals(rows, TilePairs.group(split(*rows.toTypedArray()), { it.startsWith("s") }) { a, b -> "($a+$b)" })
    }

    // A block of "s1".."s4" is "[s1 s2 s3 s4]".
    private fun blocks(vararg items: String): List<String> =
        TilePairs.blocks(items.toList(), { it.startsWith("s") }) { "[" + it.joinToString(" ") + "]" }

    @Test
    fun fourSmallOnesAreOneBlock() {
        assertEquals(listOf("[s1 s2 s3 s4]"), blocks("s1", "s2", "s3", "s4"))
    }

    @Test
    fun anIncompleteBlockOfTwoIsOneItem() {
        assertEquals(listOf("n1", "[s1 s2]", "n2"), blocks("n1", "s1", "s2", "n2"))
    }

    @Test
    fun aLoneSmallOneAndThreeSmallOnesAreEachOneBlock() {
        assertEquals(listOf("[s1]", "n1", "[s2 s3 s4]"), blocks("s1", "n1", "s2", "s3", "s4"))
    }

    @Test
    fun aRunOfSixIsAFullBlockAndOneOfTwo() {
        assertEquals(listOf("[s1 s2 s3 s4]", "[s5 s6]"), blocks("s1", "s2", "s3", "s4", "s5", "s6"))
    }

    @Test
    fun blocksDoNotReachAcrossABiggerTile() {
        assertEquals(listOf("[s1 s2]", "n1", "[s3 s4]"), blocks("s1", "s2", "n1", "s3", "s4"))
    }

    @Test
    fun theOrderIsKeptInBlocks() {
        assertEquals(
            listOf("n1", "[s1 s2 s3 s4]", "n2", "[s5]"),
            blocks("n1", "s1", "s2", "s3", "s4", "n2", "s5")
        )
    }

    @Test
    fun noItemsNoBlocks() {
        assertEquals(emptyList<String>(), blocks())
    }

    @Test
    fun aDraggedTileChangesPlacesWithAWholeBlock() {
        val stood = blocks("n1", "s1", "s2", "s3", "s4", "n2")
        val moved = CategoryReorder.moved(stood, 0, 1)
        val parts = TilePairs.split(moved) { if (it.startsWith("[")) it.trim('[', ']').split(" ") else null }
        assertEquals(listOf("s1", "s2", "s3", "s4", "n1", "n2"), parts)
    }
}
