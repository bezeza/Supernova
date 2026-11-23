package com.supernova.launcher.fork.folders

import android.os.SystemClock
import androidx.core.graphics.ColorUtils
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.ui.LauncherViewModel

// The folders' own options, found by the folder's id, for the few places that
// draw or open a folder. The activity names the folder just before its window
// opens (opening); the window takes the name (take) when it is built.
object ForkFolders {

    private const val ALPHA_MAX = 255
    private var pending: PendingFolder? = null

    // [key] is a placed folder's item id or a drawer folder's group id.
    fun opening(viewModel: LauncherViewModel, key: String) {
        pending = PendingFolder(options(viewModel.state.value.prefs, key), SystemClock.uptimeMillis())
    }

    // The options of the folder whose window is being built, once; null for a window nobody named.
    fun take(): FolderOptions? {
        val options = PendingFolder.usable(pending, SystemClock.uptimeMillis())
        pending = null
        return options
    }

    fun options(prefs: LauncherPrefs, key: String): FolderOptions? =
        FolderOptionsStore.decode(prefs.folderOptions)[key]

    // The preview a folder's icon asks for, or null to follow the settings.
    fun preview(prefs: LauncherPrefs, key: String): PreviewSpec? =
        options(prefs, key)?.let { FolderLooks.spec(it.look) }

    // The window's colour at the folder's opacity; [base] is what it would be.
    fun surface(options: FolderOptions?, base: Int): Int =
        options?.tint?.let { ColorUtils.setAlphaComponent(base, it * ALPHA_MAX / FolderOptions.TINT_MAX) } ?: base
}
