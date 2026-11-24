package com.supernova.launcher.fork.categories

import com.supernova.launcher.data.SettingEnumSerializer
import kotlin.math.max
import kotlin.math.min

// How many columns an open category's apps stand in: by how many apps it has (AUTO), as many as the drawer's own grid
// (MENU, which is how the screen was before this setting), or a number of its own. Its name is what is stored.
@kotlinx.serialization.Serializable(with = CategoryColumns.Names::class)
enum class CategoryColumns {
    AUTO,
    MENU,
    THREE,
    FOUR,
    FIVE;

    // Its name in the store and in a backup, read back leniently.
    object Names : SettingEnumSerializer<CategoryColumns>("CategoryColumns", entries, AUTO)
}

// Where an open category's apps stand, as plain arithmetic: the columns, the icon's size to fill them, and the sides
// left over when the apps are fewer than the columns. The column keeps the width it has for the columns the setting
// gives, so a category of two apps does not get icons as big as the screen: the block of them is centred instead.
internal object CategoryColumnsLayout {

    // The most apps AUTO gives three columns, and the most it gives four; more is as many as the menu has.
    const val AUTO_THREE_MAX = 9
    const val AUTO_FOUR_MAX = 16

    // What the icon is of its column, at most, and how far an icon grows over the drawer's own size.
    private const val ICON_SHARE = 0.6f
    private const val MAX_SCALE = 2f

    // The icon box the drawer's tile starts from, in dp (AppDrawerAdapter.TILE_ICON_DP): its scale is on top of it.
    private const val BASE_ICON_DP = 48

    private const val THREE = 3
    private const val FOUR = 4
    private const val FIVE = 5

    class Result(
        // The columns the setting gives, which set a column's width.
        val columns: Int,
        // The columns the apps fill: fewer than [columns] when there are fewer apps.
        val shown: Int,
        val iconScale: Float,
        // What the grid gives up on each side to keep the block of apps in the middle.
        val sidePx: Int,
        val columnPx: Int
    )

    fun columns(setting: CategoryColumns, apps: Int, menuColumns: Int): Int = when (setting) {
        CategoryColumns.AUTO -> when {
            apps <= AUTO_THREE_MAX -> THREE
            apps <= AUTO_FOUR_MAX -> FOUR
            else -> menuColumns
        }
        CategoryColumns.MENU -> menuColumns
        CategoryColumns.THREE -> THREE
        CategoryColumns.FOUR -> FOUR
        CategoryColumns.FIVE -> FIVE
    }

    // [widthPx] is the grid's width without its padding, [menuScale] the icon scale the drawer has.
    @Suppress("LongParameterList") // Six numbers the screen gives, each used once.
    fun compute(
        setting: CategoryColumns,
        apps: Int,
        menuColumns: Int,
        menuScale: Float,
        widthPx: Int,
        density: Float
    ): Result {
        val columns = max(1, columns(setting, apps, menuColumns))
        val shown = min(columns, max(1, apps))
        val columnPx = max(1, widthPx / columns)
        val wanted = columnPx * ICON_SHARE / (BASE_ICON_DP * density)
        val scale = max(menuScale, min(MAX_SCALE, wanted))
        return Result(columns, shown, scale, (columns - shown) * columnPx / 2, columnPx)
    }
}
