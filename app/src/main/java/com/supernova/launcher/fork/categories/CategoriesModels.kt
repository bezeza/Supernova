package com.supernova.launcher.fork.categories

import android.view.View
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp

// One category on the Categories drawer: a folder (smart or the user's) and
// its apps in the order the drawer sorts them.
class CategoryEntry(val group: DrawerGroup, val apps: List<LaunchableApp>)

// How the tiles look and respond, from the drawer's colours and the settings.
data class TileLook(
    val textColor: Int,
    // The drawer background's opacity: the tiles' fill follows it (CategoryTint).
    val backdrop: Float = 1f,
    // The category's icon before its name, a setting that is off unless chosen.
    val showIcons: Boolean = false,
    // A tap on a tile's large icon launches the app (iOS), or opens the category.
    val launchFromIcons: Boolean = true,
    // The menu of a tile: its surface and text colours (the popup surface of the style) and the accent that
    // marks the size the tile has.
    val menuSurface: Int = 0xFF2B2F36.toInt(),
    val menuText: Int = 0xFFFFFFFF.toInt(),
    val accent: Int = 0xFF4C8DF6.toInt()
)

// What the drawer hands over to draw: the categories in the folders' order,
// the apps no folder took, and the newest installs. Colours come from the
// drawer's own style, so the tiles follow the theme.
class CategoriesData(
    val categories: List<CategoryEntry>,
    val loose: List<LaunchableApp>,
    val recent: List<LaunchableApp>,
    val look: TileLook,
    val settings: CategorySettings = CategorySettings()
)

// Each category's own settings by folder id, the categories taken off the
// tiles, and how many newest installs the row on top shows.
class CategorySettings(
    val options: Map<String, CategoryOptions> = emptyMap(),
    val hidden: List<HiddenCategory> = emptyList(),
    val recentCount: Int = CategoryLayout.RECENT_DEFAULT
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
    val edits: CategoryEdits
)

// What changes a category: the large icons chosen, its settings, and the "+" on its screen.
class CategoryEdits(
    // The apps tapped as the category's large icons, in the order tapped: the folder takes them as its first apps.
    val onPlaceLarge: (DrawerGroup, List<LaunchableApp>) -> Unit,
    // A category's settings changed: its folder id and the change.
    val onOptions: (String, (CategoryOptions) -> CategoryOptions) -> Unit,
    // The "+" on a category's screen: pick the apps of the folder.
    val onAddApps: (DrawerGroup) -> Unit,
    // The tiles dragged to a new order: the ids of the shown folders, in that order.
    val onOrder: (List<String>) -> Unit,
    // Tiles put in cells, some given another size too, all in one change (a tile moved moves others).
    val onTiles: (Map<String, TileChange>) -> Unit = {}
)

// What arranging the tiles asks of the controller.
internal class EditHandlers(
    // A tap on a tile: its settings.
    val onTap: (View, CategoryEntry) -> Unit,
    val onDone: () -> Unit,
    // A tile's corner dragged to another size.
    val onResize: (String, CategorySize) -> Unit,
    // A tile tapped while arranging: its menu, for the tile with this folder id.
    val onMenu: (View, String) -> Unit = { _, _ -> }
)

internal fun appKey(app: LaunchableApp): String = "${app.userSerial}/${app.packageName}/${app.className}"
