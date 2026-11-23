package com.supernova.launcher.fork.categories

// A wide tile is a row: the name, then up to six icons of a middle size, and
// "+N" for the apps that do not fit. Six slots always fit the row, so this is
// the one count that decides it.
internal object WideRow {

    const val SLOTS = 6

    // [shown] icons, and [more] apps left over; with more than SLOTS apps the
    // last slot is the "+N", so one fewer icon.
    class Plan(val shown: Int, val more: Int)

    fun plan(total: Int): Plan = when {
        total <= 0 -> Plan(0, 0)
        total <= SLOTS -> Plan(total, 0)
        else -> Plan(SLOTS - 1, total - (SLOTS - 1))
    }
}
