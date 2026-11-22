package com.supernova.launcher.ui.settings

import android.content.Context
import com.supernova.launcher.R
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.data.Pref
import com.supernova.launcher.ui.DrawerStyle
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
                val withStyleRows = afterDrawerStyle(section.items, categoriesRows(viewModel))
                SettingsSection(section.title, afterSmartFolders(withStyleRows, extra))
            } else {
                section
            }
        }

    private fun afterDrawerStyle(items: List<SettingItem>, extra: List<SettingItem>): List<SettingItem> {
        val at = items.indexOfFirst { it.title == R.string.drawer_style }
        return if (at < 0) items else items.take(at + 1) + extra + items.drop(at + 1)
    }

    // Only while the Categories style is picked.
    private fun categoriesRows(viewModel: LauncherViewModel): List<SettingItem> = listOf(
        categoriesToggle(
            R.string.fork_show_recent_installs, R.string.fork_show_recent_installs_summary,
            { it.categoriesShowRecent }
        ) { viewModel.set(Pref.categoriesShowRecent, it) },
        categoriesToggle(
            R.string.fork_show_category_icons, R.string.fork_show_category_icons_summary,
            { it.categoriesShowIcons }
        ) { viewModel.set(Pref.categoriesShowIcons, it) },
        categoriesToggle(
            R.string.fork_search_at_bottom, R.string.fork_search_at_bottom_summary,
            { it.categoriesSearchBottom }
        ) { viewModel.set(Pref.categoriesSearchBottom, it) }
    )

    private fun categoriesToggle(
        title: Int,
        summary: Int,
        get: (LauncherPrefs) -> Boolean,
        set: (Boolean) -> Unit
    ): SettingItem = SettingItem.Toggle(title, get = get, set = set, subtitle = summary).hiddenWhen {
        SettingItem.HiddenBy(R.string.drawer_style, SettingItem.HiddenBy.Needs.OTHER)
            .takeIf { _ -> it.drawerStyle != DrawerStyle.CATEGORIES }
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
