package com.supernova.launcher.ui.settings

import android.content.Context
import com.supernova.launcher.R
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.data.Pref
import com.supernova.launcher.fork.about.ForkAbout
import com.supernova.launcher.fork.backup.ForkBackupRows
import com.supernova.launcher.fork.categories.CategoryColumns
import com.supernova.launcher.fork.frequent.FrequentSettings
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
            if (section.title == R.string.settings_section_home) {
                SettingsSection(section.title, afterInfiniteScroll(section.items, listOf(homePageRow(viewModel))))
            } else if (section.title == R.string.settings_section_drawer) {
                val extra = autoSortRows(context, viewModel, actions)
                val extraRows = categoriesRows(viewModel) + columnsRow(context, viewModel, actions) +
                    folderScreenRow(viewModel) + FrequentSettings.row(viewModel)
                val withStyleRows = afterDrawerStyle(section.items, extraRows)
                SettingsSection(section.title, afterSmartFolders(withStyleRows, extra))
            } else if (section.title == R.string.settings_section_backup) {
                SettingsSection(section.title, ForkBackupRows.addTo(context, viewModel, section.items))
            } else if (section.title == R.string.about) {
                SettingsSection(section.title, ForkAbout.addTo(context, section.items))
            } else {
                section
            }
        }

    private fun afterInfiniteScroll(items: List<SettingItem>, extra: List<SettingItem>): List<SettingItem> {
        val at = items.indexOfFirst { it.title == R.string.infinite_scroll }
        return if (at < 0) items + extra else items.take(at + 1) + extra + items.drop(at + 1)
    }

    // The last page of the home screen: the category tiles, as in the Categories drawer.
    private fun homePageRow(viewModel: LauncherViewModel): SettingItem = SettingItem.Toggle(
        R.string.fork_home_page,
        get = { it.categoriesHomePage },
        set = { viewModel.set(Pref.categoriesHomePage, it) },
        subtitle = R.string.fork_home_page_summary
    )

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
        SettingItem.Slider(
            R.string.fork_recent_count,
            min = 2, max = 8,
            get = { it.categoriesRecentCount },
            set = { viewModel.set(Pref.categoriesRecentCount, it) },
            display = { it.toString() }
        ).hiddenWhen {
            when {
                it.drawerStyle != DrawerStyle.CATEGORIES ->
                    SettingItem.HiddenBy(R.string.drawer_style, SettingItem.HiddenBy.Needs.OTHER)
                !it.categoriesShowRecent ->
                    SettingItem.HiddenBy(R.string.fork_show_recent_installs, SettingItem.HiddenBy.Needs.ON)
                else -> null
            }
        },
        categoriesToggle(
            R.string.fork_show_category_icons, R.string.fork_show_category_icons_summary,
            { it.categoriesShowIcons }
        ) { viewModel.set(Pref.categoriesShowIcons, it) },
        categoriesToggle(
            R.string.fork_tap_large_icon, R.string.fork_tap_large_icon_summary,
            { it.categoriesLaunchLarge }
        ) { viewModel.set(Pref.categoriesLaunchLarge, it) },
        categoriesToggle(
            R.string.fork_search_at_bottom, R.string.fork_search_at_bottom_summary,
            { it.categoriesSearchBottom }
        ) { viewModel.set(Pref.categoriesSearchBottom, it) }
    )

    // How many columns an open category has. Shown wherever a category can open: the Categories style, the home
    // screen's page of categories, or folders opened on a screen of their own.
    private fun columnsRow(
        context: Context,
        viewModel: LauncherViewModel,
        actions: SettingsCatalogue.Actions
    ): SettingItem {
        val options = CategoryColumns.entries
        val labels = listOf(
            R.string.fork_columns_auto, R.string.fork_columns_menu, R.string.fork_columns_3,
            R.string.fork_columns_4, R.string.fork_columns_5
        ).map { context.getString(it) }
        return SettingItem.Row(
            R.string.fork_columns,
            subtitle = { labels[options.indexOf(it.categoriesColumns)] },
            onClick = {
                actions.pickChoice(
                    Choice(
                        R.string.fork_columns, labels, options.indexOf(viewModel.state.value.prefs.categoriesColumns),
                        { viewModel.set(Pref.categoriesColumns, options[it]) }
                    )
                )
            },
            visibleIf = { it.drawerStyle == DrawerStyle.CATEGORIES || it.categoriesHomePage || it.folderScreen }
        )
    }

    // Another drawer style: a folder of the drawer opens on a screen like a category's, with its "+".
    private fun folderScreenRow(viewModel: LauncherViewModel): SettingItem = SettingItem.Toggle(
        R.string.fork_folder_screen,
        get = { it.folderScreen },
        set = { viewModel.set(Pref.folderScreen, it) },
        subtitle = R.string.fork_folder_screen_summary
    ).hiddenWhen {
        SettingItem.HiddenBy(R.string.drawer_style, SettingItem.HiddenBy.Needs.OTHER)
            .takeIf { _ -> it.drawerStyle == DrawerStyle.CATEGORIES }
    }

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
