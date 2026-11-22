package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.data.AppCatalog
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.ui.AppDrawerAdapter

// The Categories drawer: a list of its own that sits over the drawer's usual
// one while the style is on, the All tab is showing and nothing is searched.
// Search results and the group tabs stay the drawer's own lists, so they work
// as they do in every other style. The drawer feeds it through [submit] and
// shows it through [setVisible]; nothing else of the drawer is replaced.
class CategoriesController(
    container: FrameLayout,
    catalog: AppCatalog,
    decorateIcon: (Drawable?, LaunchableApp) -> Drawable?,
    private val actions: CategoriesActions,
    // The drawer's own kind of adapter, for the apps of a category opened on its own screen.
    screenAdapter: AppDrawerAdapter
) {

    private var options: Map<String, CategoryOptions> = emptyMap()

    // A long press on a tile opens the category's options; the folder's own menu is a row in them.
    private val rowActions = CategoriesActions(
        actions.onApp, actions.onAppLongPress, actions.onCategory,
        { view, group, apps -> showOptions(view, group, apps) },
        actions.onPlaceLarge, actions.onOptions
    )
    private val rows = CategoriesAdapter(CategoryIcons(catalog, decorateIcon), rowActions) { context, hidden ->
        CategoryOptionsDialog.showHidden(context, hidden) { id -> actions.onOptions(id) { it.copy(hidden = false) } }
    }

    private val recycler = RecyclerView(container.context).apply {
        visibility = View.GONE
        layoutManager = GridLayoutManager(context, MIN_TILES * 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = rows.spanAt(position, spanCount)
            }
        }
        adapter = rows
        itemAnimator = null
        setHasFixedSize(true)
        clipToPadding = false
        overScrollMode = View.OVER_SCROLL_NEVER
        val side = (SIDE_DP * resources.displayMetrics.density).toInt()
        val bottom = resources.getDimensionPixelSize(R.dimen.space_ml)
        setPadding(side, 0, side, bottom)
        // A wide screen (landscape, a tablet) gets more tiles per row rather than bigger ones.
        addOnLayoutChangeListener { view, left, _, right, _, _, _, _, _ ->
            val tiles = ((right - left) / (TILE_MAX_DP * view.resources.displayMetrics.density))
                .toInt().coerceAtLeast(MIN_TILES)
            (layoutManager as GridLayoutManager).spanCount = tiles * 2
        }
    }

    private val state = CategoryScreenState()
    private val screen = CategoryScreen(container, screenAdapter, { back() }) { picked ->
        openGroup?.let { actions.onPlaceLarge(it, picked) }
    }
    private var openGroup: DrawerGroup? = null

    init {
        container.addView(
            recycler,
            1,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    // The drawer's swipe to the neighbouring tab, on the tiles and on a category's screen.
    fun addTabSwipe(onTiles: RecyclerView.OnItemTouchListener, onScreen: RecyclerView.OnItemTouchListener) {
        recycler.addOnItemTouchListener(onTiles)
        screen.grid.addOnItemTouchListener(onScreen)
    }

    // Whether the drawer wants the tiles on screen, open category or not.
    private var wanted = false

    val isVisible: Boolean get() = wanted

    fun setVisible(show: Boolean) {
        wanted = show
        // The tiles going away (search, a tab, another style) takes the screen with them.
        if (!show) closeScreen(animate = false)
        showTiles()
    }

    // Hidden, not gone, under a category's screen: the scroll position stays.
    private fun showTiles() {
        recycler.visibility = when {
            state.tilesShown(wanted) -> View.VISIBLE
            wanted -> View.INVISIBLE
            else -> View.GONE
        }
    }

    // A tile opened: the category's apps on a screen of their own.
    fun openScreen(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook) {
        state.open(group.id)
        openGroup = group
        showTiles()
        screen.show(group, apps, look, animate = true)
    }

    // Back: choosing large icons stops first, then the screen closes. True
    // when it did either, so the Back is spent.
    fun back(): Boolean = screen.cancelPicking() || closeScreen()

    // A tap on an app of the screen: a choice while choosing, else [launch].
    fun onScreenAppClick(launch: () -> Unit, app: LaunchableApp) {
        if (!screen.handleTap(app)) launch()
    }

    // True when a screen was open and is closed now: the Back that did it is spent.
    fun closeScreen(animate: Boolean = true): Boolean {
        val wasOpen = state.close()
        showTiles()
        screen.hide(animate && wasOpen)
        return wasOpen
    }

    // [showRecent] is the settings switch for the row over the tiles.
    fun submit(data: CategoriesData, showRecent: Boolean) {
        rows.setLook(data.look)
        options = data.settings.options
        screen.setLook(data.look.textColor, data.look.showIcons)
        if (state.isOpen) {
            val open = data.categories.firstOrNull { it.group.id == state.openId }
            if (open != null && state.sync(data.categories.map { it.group.id })) screen.update(open.apps)
            else closeScreen(animate = false)
        }
        val context = recycler.context
        val slots = data.settings.recentCount.coerceIn(CategoryLayout.RECENT_MIN, CategoryLayout.RECENT_MAX)
        val list = ArrayList<CategoryRow>()
        val recent = if (showRecent) {
            CategoryLayout.recentlyInstalled(data.recent, slots) { it.installTime }
        } else {
            emptyList()
        }
        if (recent.isNotEmpty()) list.add(CategoryRow.Recent(recent, slots))
        data.categories.mapTo(list) { CategoryRow.Tile(it, data.settings.options[it.group.id] ?: CategoryOptions()) }
        if (data.loose.isNotEmpty()) {
            if (data.categories.isNotEmpty()) {
                list.add(CategoryRow.Header(context.getString(R.string.fork_other_apps)))
            }
            data.loose.mapTo(list) { CategoryRow.Loose(it) }
        }
        if (data.settings.hidden.isNotEmpty()) list.add(CategoryRow.Hidden(data.settings.hidden))
        rows.submitList(list)
    }

    private fun showOptions(view: View, group: DrawerGroup, apps: List<LaunchableApp>) {
        val handlers = OptionsHandlers(
            onSort = { sort -> actions.onOptions(group.id) { it.copy(sort = sort) } },
            onSize = { size -> actions.onOptions(group.id) { it.copy(size = size) } },
            onLarge = { count -> actions.onOptions(group.id) { it.copy(large = count) } },
            onHide = { actions.onOptions(group.id) { it.copy(hidden = true) } },
            onFolderMenu = { actions.onCategoryLongPress(view, group, apps) }
        )
        CategoryOptionsDialog.show(view.context, group.name, options[group.id] ?: CategoryOptions(), handlers)
    }

    // A badge or an icon moved: the rows rebind, the list itself is unchanged.
    fun rebind() {
        rows.notifyItemRangeChanged(0, rows.itemCount)
        if (state.isOpen) screen.rebind()
    }

    private companion object {
        const val MIN_TILES = 2
        const val TILE_MAX_DP = 200f
        const val SIDE_DP = 6f
    }
}
