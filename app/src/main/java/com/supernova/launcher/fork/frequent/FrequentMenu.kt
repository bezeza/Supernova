package com.supernova.launcher.fork.frequent

import android.content.Context
import com.supernova.launcher.R
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.data.Pref
import com.supernova.launcher.ui.LauncherViewModel
import com.supernova.launcher.ui.SupernovaPopupMenu

// The row the menu of an app gets while the Frequent category's screen is open: take the app off it for good.
// LauncherActivity.showDrawerItemMenu hands the menu over in one call (marked Fork there).
object FrequentMenu {

    fun addRows(
        context: Context,
        menu: SupernovaPopupMenu,
        viewModel: LauncherViewModel,
        app: LaunchableApp,
        onFrequentScreen: Boolean
    ) {
        if (!onFrequentScreen) return
        menu.addRow(context.getString(R.string.fork_frequent_exclude), R.drawable.ic_menu_hide) {
            val excluded = viewModel.state.value.prefs.frequentExcluded
            viewModel.set(Pref.frequentExcluded, excluded + viewModel.hiddenKey(app))
        }
    }
}
