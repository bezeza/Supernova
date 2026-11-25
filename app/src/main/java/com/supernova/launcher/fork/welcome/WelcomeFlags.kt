package com.supernova.launcher.fork.welcome

import android.content.Context
import androidx.core.content.edit

// What of the fork's welcome this device has been through. Its own file: not a launcher setting, in no backup.
internal class WelcomeFlags(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var languageAsked: Boolean
        get() = prefs.getBoolean(LANGUAGE, false)
        set(value) = prefs.edit { putBoolean(LANGUAGE, value) }

    private companion object {
        const val FILE = "fork_welcome"
        const val LANGUAGE = "language_asked"
    }
}
