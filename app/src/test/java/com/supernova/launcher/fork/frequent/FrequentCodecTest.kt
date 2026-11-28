package com.supernova.launcher.fork.frequent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrequentCodecTest {

    @Test
    fun aStatSurvivesTheRoundTrip() {
        val stat = AppStat(3.4567891234567, 1_732_000_000_000L, 9)
        val entry = FrequentCodec.encodeStat("0/com.x/com.x.Main", stat)
        assertEquals("0/com.x/com.x.Main" to stat, FrequentCodec.decodeStat(entry))
    }

    @Test
    fun anEntryThatCannotBeReadIsLeftOut() {
        listOf("", "a", "a\t1\t2", "a\tx\t2\t3", "a\t1\ty\t3", "a\t1\t2\tz", "a\tNaN\t2\t3", "a\t-1\t2\t3", "\t1\t2\t3")
            .forEach { assertNull(it, FrequentCodec.decodeStat(it)) }
        val read = FrequentCodec.decodeStats(setOf("b\t1.0\t5\t1", "broken"))
        assertEquals(setOf("b"), read.keys)
    }

    @Test
    fun aListKeepsItsOrder() {
        val keys = listOf("c", "a", "b")
        assertEquals(keys, FrequentCodec.decodeList(FrequentCodec.encodeList(keys)))
        assertEquals(emptyList<String>(), FrequentCodec.decodeList(null))
        assertEquals(emptyList<String>(), FrequentCodec.decodeList(""))
    }
}
