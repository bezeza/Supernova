package com.supernova.launcher.fork.categories

import android.content.Context
import androidx.core.content.edit

// The short hints shown once, when a feature is first in front of the person.
internal enum class Hint(val key: String) {
    TILE_MENU("tile_menu"),
    COLUMNS("columns")
}

// Which hints have been seen. It is this device's own: not a launcher setting, not in the launcher's backup, and
// the system's backup takes only the app's files, not its shared preferences.
internal class HintFlags(private val read: (String) -> Boolean, private val write: (String) -> Unit) {

    fun seen(hint: Hint): Boolean = read(hint.key)

    // Seen once "Got it" is tapped, or what the hint tells of is done.
    fun markSeen(hint: Hint) {
        if (!seen(hint)) write(hint.key)
    }

    // The tiles' hint is for tiles that are there to be held: on screen, some of them, and not being arranged.
    fun tileMenuDue(tilesShown: Boolean, hasTiles: Boolean, arranging: Boolean): Boolean =
        tilesShown && hasTiles && !arranging && !seen(Hint.TILE_MENU)

    // The columns' hint is for an open category; while large icons are being chosen the screen is the choosing's.
    fun columnsDue(picking: Boolean): Boolean = !picking && !seen(Hint.COLUMNS)

    companion object {
        private const val FILE = "fork_hints"

        fun of(context: Context): HintFlags {
            val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return HintFlags({ prefs.getBoolean(it, false) }, { prefs.edit { putBoolean(it, true) } })
        }
    }
}
