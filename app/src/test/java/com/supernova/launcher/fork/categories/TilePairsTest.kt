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
}
