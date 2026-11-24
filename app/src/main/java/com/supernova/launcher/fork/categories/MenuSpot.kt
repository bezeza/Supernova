package com.supernova.launcher.fork.categories

// Where a tile's menu goes so that it does not lie over the tile's arc. Plain arithmetic, so it can be tested.
internal object MenuSpot {

    // A box on the screen: left, top, right and bottom edges.
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun meets(other: Box): Boolean =
            left < other.right && other.left < right && top < other.bottom && other.top < bottom
    }

    // The screen the menu must stay inside, and how far it keeps from what it avoids.
    class Room(val width: Int, val height: Int, val margin: Int, val gap: Int)

    // The corner of the menu that was meant to fill the box [want]: there if it keeps clear of [avoid], else above or
    // below it, else to its left; else where it was meant to go.
    fun place(want: Box, avoid: Box?, room: Room): Pair<Int, Int> {
        val x = want.left
        val y = want.top
        val menuW = want.right - want.left
        val menuH = want.bottom - want.top
        val spot = x to y
        if (avoid == null || !want.meets(avoid)) return spot
        val above = avoid.top - room.gap - menuH
        val below = avoid.bottom + room.gap
        val left = avoid.left - room.gap - menuW
        return when {
            above >= room.margin -> x to above
            below + menuH <= room.height - room.margin -> x to below
            left >= room.margin -> left to y
            else -> spot
        }
    }
}
