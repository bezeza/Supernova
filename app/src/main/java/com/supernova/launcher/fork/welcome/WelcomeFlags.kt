package com.supernova.launcher.fork.welcome

import android.content.Context
import androidx.core.content.edit

// What of the fork's welcome this device has been through. Its own file: not a launcher setting, in no backup.
internal class WelcomeFlags(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var languageAsked: Boolean
        get() = prefs.getBoolean(LANGUAGE, false)
        set(value) = prefs.edit { putBoolean(LANGUAGE, value) }

    var firstRun: Boolean
        get() = prefs.getBoolean(FIRST_RUN, false)
        set(value) = prefs.edit { putBoolean(FIRST_RUN, value) }

    var tourSeen: Boolean
        get() = prefs.getBoolean(TOUR, false)
        set(value) = prefs.edit { putBoolean(TOUR, value) }

    var newsSeen: Int
        get() = prefs.getInt(NEWS, 0)
        set(value) = prefs.edit { putInt(NEWS, value) }

    private companion object {
        const val FILE = "fork_welcome"
        const val LANGUAGE = "language_asked"
        const val FIRST_RUN = "first_run"
        const val TOUR = "tour_seen"
        const val NEWS = "news_seen"
    }
}
