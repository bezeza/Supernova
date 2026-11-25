package com.supernova.launcher.fork.backup

import android.content.Context
import androidx.core.content.edit

// What this device remembers about its weekly copies. Its own file: not a launcher setting, in no backup (a folder's
// grant means nothing on another install).
internal class BackupState(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var lastLocal: Long
        get() = prefs.getLong(LOCAL, 0)
        set(value) = prefs.edit { putLong(LOCAL, value) }

    var lastInFolder: Long
        get() = prefs.getLong(IN_FOLDER, 0)
        set(value) = prefs.edit { putLong(IN_FOLDER, value) }

    // The picked folder as a tree URI, or null for none.
    var folder: String?
        get() = prefs.getString(FOLDER, null)
        set(value) = prefs.edit { putString(FOLDER, value) }

    var folderFailed: Boolean
        get() = prefs.getBoolean(FAILED, false)
        set(value) = prefs.edit { putBoolean(FAILED, value) }

    private companion object {
        const val FILE = "fork_backup"
        const val LOCAL = "last_local"
        const val IN_FOLDER = "last_in_folder"
        const val FOLDER = "folder"
        const val FAILED = "folder_failed"
    }
}
