package com.supernova.launcher.data

// A drawer folder lists its apps in the drawer's order, unless they were
// moved by hand: then manualOrder is set and the folder keeps the order of
// its members. Resetting clears only the flag, so the drawer's order applies
// again and Undo puts the same flag back. The members stay either way.
object FolderManualOrder {

    fun cleared(groups: List<DrawerGroup>, id: String): List<DrawerGroup> =
        groups.map { group -> if (group.id == id) group.copy(manualOrder = false) else group }

    fun restored(groups: List<DrawerGroup>, id: String): List<DrawerGroup> =
        groups.map { group -> if (group.id == id) group.copy(manualOrder = true) else group }
}
