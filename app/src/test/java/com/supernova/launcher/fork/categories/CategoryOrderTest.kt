package com.supernova.launcher.fork.categories

import org.junit.Assert.assertEquals
import org.junit.Test

// How a category's option orders its apps; strings stand in for apps.
class CategoryOrderTest {

    private val apps = listOf("banana", "cherry", "apple")
    private val orderings = Orderings<String>(
        alphabetical = { list -> list.sorted() },
        byUse = { list -> list.sortedByDescending { it.length } },
        keyOf = { it }
    )

    private fun order(sort: CategorySort, members: List<String> = emptyList()) =
        CategoryOrder.apply(apps, sort, members, orderings)

    @Test
    fun theDrawerOrderIsKept() {
        assertEquals(apps, order(CategorySort.DRAWER))
    }

    @Test
    fun alphabeticalAsksTheDrawersNameOrder() {
        assertEquals(listOf("apple", "banana", "cherry"), order(CategorySort.ALPHA))
    }

    @Test
    fun frequencyAsksTheDrawersUsageOrder() {
        assertEquals(listOf("banana", "cherry", "apple"), order(CategorySort.FREQUENCY))
    }

    @Test
    fun manualFollowsTheFoldersMembers() {
        val members = listOf("cherry", "apple", "banana")
        assertEquals(members, order(CategorySort.MANUAL, members))
    }

    @Test
    fun manualPutsAnAppTheFolderDoesNotListLastInItsOrder() {
        assertEquals(listOf("apple", "banana", "cherry"), order(CategorySort.MANUAL, listOf("apple")))
    }

    @Test
    fun manualWithNoMembersChangesNothing() {
        assertEquals(apps, order(CategorySort.MANUAL))
    }
}
