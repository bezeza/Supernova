package com.supernova.launcher.fork.categories

import android.view.View
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp

// One category on the Categories drawer: a folder (smart or the user's) and
// its apps in the order the drawer sorts them.
class CategoryEntry(val group: DrawerGroup, val apps: List<LaunchableApp>)

// What the drawer hands over to draw: the categories in the folders' order,
// the apps no folder took, and the newest installs. Colours come from the
// drawer's own style, so the tiles follow the theme.
class CategoriesData(
    val categories: List<CategoryEntry>,
    val loose: List<LaunchableApp>,
    val recent: List<LaunchableApp>,
    val textColor: Int,
    // The drawer background's opacity: the tiles' fill follows it (CategoryTint).
    val backdrop: Float = 1f,
    // The category's icon before its name, a setting that is off unless chosen.
    val showIcons: Boolean = false
)

// What a tap or a hold on a tile does. The drawer owns these: they launch an
// app, open a folder's card and show the menus the other drawer styles have.
class CategoriesActions(
    val onApp: (View, LaunchableApp) -> Unit,
    val onAppLongPress: (View, LaunchableApp) -> Unit,
    val onCategory: (View, DrawerGroup, List<LaunchableApp>) -> Unit,
    val onCategoryLongPress: (View, DrawerGroup, List<LaunchableApp>) -> Unit,
    // The apps tapped as the category's large icons, in the order tapped: the
    // folder takes them as its first apps.
    val onPlaceLarge: (DrawerGroup, List<LaunchableApp>) -> Unit
)

internal fun appKey(app: LaunchableApp): String = "${app.userSerial}/${app.packageName}/${app.className}"
