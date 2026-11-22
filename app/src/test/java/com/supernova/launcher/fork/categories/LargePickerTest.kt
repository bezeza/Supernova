package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Choosing a category's large icons by tapping apps in turn.
class LargePickerTest {

    @Test
    fun theFirstTapIsTheFirstLargeIcon() {
        val picker = LargePicker<String>(3)
        assertEquals(1, picker.next)
        assertTrue(picker.pick("A"))
        assertEquals(2, picker.next)
        assertEquals(listOf("A"), picker.picked)
    }

    @Test
    fun threeTapsFillThreeSlotsInTheOrderTapped() {
        val picker = LargePicker<String>(3)
        picker.pick("C")
        picker.pick("A")
        assertFalse(picker.isComplete)
        picker.pick("B")
        assertTrue(picker.isComplete)
        assertEquals(listOf("C", "A", "B"), picker.picked)
    }

    @Test
    fun anAppAlreadyChosenIsNotTakenTwice() {
        val picker = LargePicker<String>(3)
        picker.pick("A")
        assertFalse(picker.pick("A"))
        assertEquals(listOf("A"), picker.picked)
        assertEquals(2, picker.next)
    }

    @Test
    fun aTapAfterTheLastSlotIsIgnored() {
        val picker = LargePicker<String>(2)
        picker.pick("A")
        picker.pick("B")
        assertFalse(picker.pick("C"))
        assertEquals(listOf("A", "B"), picker.picked)
    }

    @Test
    fun aCategoryOfTwoAppsHasTwoSlots() {
        val picker = LargePicker<String>(2)
        picker.pick("B")
        picker.pick("A")
        assertTrue(picker.isComplete)
    }
}
