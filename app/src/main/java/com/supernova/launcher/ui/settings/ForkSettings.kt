package com.supernova.launcher.ui.settings

import android.content.Context
import com.supernova.launcher.R
import com.supernova.launcher.data.Pref
import com.supernova.launcher.ui.LauncherViewModel

// The settings rows this fork adds. They are slipped into the catalogue's
// sections here, after the row they belong beside, so the catalogue's own
// lists stay as upstream has them.
object ForkSettings {

    // The catalogue's sections, with this fork's rows in them.
    fun sections(
        context: Context,
        viewModel: LauncherViewModel,
        actions: SettingsCatalogue.Actions
    ): List<SettingsSection> =
        SettingsCatalogue.sections(context, viewModel, actions).map { section ->
            if (section.title == R.string.settings_section_drawer) {
                val extra = autoSortRows(context, viewModel, actions)
                SettingsSection(section.title, afterSmartFolders(section.items, extra))
            } else {
                section
            }
        }

    private fun afterSmartFolders(items: List<SettingItem>, extra: List<SettingItem>): List<SettingItem> {
        val at = items.indexOfFirst { it.title == R.string.smart_folders }
        return if (at < 0) items + extra else items.take(at + 1) + extra + items.drop(at + 1)
    }

    private fun autoSortRows(
        context: Context,
        viewModel: LauncherViewModel,
        actions: SettingsCatalogue.Actions
    ): List<SettingItem> = listOf(
        SettingItem.Toggle(
            R.string.auto_sort_new_apps,
            get = { it.autoSortNewApps },
            set = { viewModel.set(Pref.autoSortNewApps, it) },
            subtitle = R.string.auto_sort_new_apps_summary
        ),
        SettingItem.Row(
            R.string.auto_sort_now,
            subtitle = { context.getString(R.string.auto_sort_now_summary) },
            onClick = actions.autoSortNow
        )
    )
}
