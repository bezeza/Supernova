package com.supernova.launcher.fork.frequent

import com.supernova.launcher.R
import com.supernova.launcher.ui.LauncherViewModel
import com.supernova.launcher.ui.settings.SettingItem

// The Frequent category's switch in the settings: a tile of the categories (the drawer's and the home page's) with
// the apps opened most from the launcher. Off by default; off, nothing is collected, and what was collected stays.
object FrequentSettings {

    fun row(viewModel: LauncherViewModel): SettingItem = SettingItem.Toggle(
        R.string.fork_frequent_title,
        get = { it.frequentEnabled },
        set = { viewModel.setFrequentEnabled(it) },
        subtitle = R.string.fork_frequent_summary
    )
}
