package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
@Suppress("TooManyFunctions") // A controller is many small entry points for the drawer.
class CategoriesController(
    container: FrameLayout,
    catalog: AppCatalog,
    decorateIcon: (Drawable?, LaunchableApp) -> Drawable?,
    private val actions: CategoriesActions,
    // The drawer's own kind of adapter, for the apps of a category opened on its own screen.
    screenAdapter: AppDrawerAdapter
) {

    // How far the bottom of this list lies under what the host puts over it (a dock, a page indicator): the tiles
    // and the bar that ends the arranging are kept clear of that much.
    var bottomClearance: () -> Int = { 0 }


    private var options: Map<String, CategoryOptions> = emptyMap()

    // A tap on a tile while the tiles are arranged opens the category's options (the folder's own menu is a row in
    // them); a corner dragged to another size changes the category's size.
    private val editHandlers: EditHandlers = EditHandlers(
        onTap = { view, entry -> showOptions(view, entry.group, entry.apps) },
        onDone = { arranger.stop() },
        onResize = { id, size -> actions.edits.onOptions(id) { it.copy(size = size) } },
        onMenu = { view, id -> openMenuAtCentre(view, id) }
    )

    // A long press on a tile opens its menu, and a move of the finger from there takes the tile off to be dragged.
    private val rowActions: CategoriesActions = CategoriesActions(
        actions.onApp, actions.onAppLongPress, actions.onCategory,
        { view, group, _ -> if (!state.isOpen) holdTile(view, group.id) },
        actions.edits
    )
    private val icons: CategoryIcons = CategoryIcons(catalog, decorateIcon)
    private val rows: CategoriesAdapter = CategoriesAdapter(icons, rowActions, editHandlers) { context, hidden ->
        CategoryOptionsDialog.showHidden(context, hidden) { id ->
            actions.edits.onOptions(id) { it.copy(hidden = false) }
        }
    }

    private val sidePx = (SIDE_DP * container.resources.displayMetrics.density).toInt()
    private val baseBottomPx = container.resources.getDimensionPixelSize(R.dimen.space_ml)

    private val recycler: RecyclerView = RecyclerView(container.context).apply {
        visibility = View.GONE
        layoutManager = MosaicLayoutManager(rows).apply { spanCount = MIN_TILES * 2 }
        adapter = rows
        itemAnimator = null
        clipToPadding = false
        overScrollMode = View.OVER_SCROLL_NEVER
        setPadding(sidePx, 0, sidePx, baseBottomPx)
        // A wide screen (landscape, a tablet) gets more tiles per row rather than bigger ones.
        addOnLayoutChangeListener { view, left, _, right, _, _, _, _, _ ->
            val tiles = ((right - left) / (TILE_MAX_DP * view.resources.displayMetrics.density))
                .toInt().coerceAtLeast(MIN_TILES)
            (layoutManager as MosaicLayoutManager).spanCount = tiles * 2
            rows.setColumns(tiles * 2)
            keepClear()
        }
    }

    // The bar that ends the arranging: over the bottom of the list, seen however far the list is scrolled.
    private val doneBar: PinnedDoneBar = PinnedDoneBar(container.context) { arranger.stop() }.apply {
        visibility = View.GONE
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> keepClear() }
    }

    // What the dragging of a tile asks of this list and changes in it.
    private val dragHost = object : MosaicDrag.Host {
        override fun placed(): List<Placed> = rows.placed()

        override fun preview(cells: List<Placed>?) = rows.showCells(cells)

        override fun commit(cells: List<Placed>) = commitCells(cells, null)

        override fun idOf(view: View): String? =
            recycler.getChildViewHolder(view)?.bindingAdapterPosition?.let { rows.tileIdAt(it) }

        override fun dragStarted() = menu.dismiss()

        override fun arranging(): Boolean = rows.editing

        override fun menuShowing(): Boolean = menu.isShowing

        override fun dismissMenu() = menu.dismiss()

        override fun resize(id: String, size: CategorySize) = resizeTile(id, size)
    }

    private val drag = MosaicDrag(recycler, dragHost).also { recycler.addOnItemTouchListener(it) }

    // The menu of a tile, made again for the colours of the moment.
    private var look = TileLook(textColor = 0)
    private var menuOwner: TileMenu? = null
    private val menu: TileMenu
        get() = menuOwner ?: TileMenu(recycler.context, look).also {
            it.onDismiss = { if (!drag.isResizing) drag.hideHandle() }
            menuOwner = it
        }

    // The finger holds a tile outside the arranging: its menu opens, and a move of the finger takes the tile off.
    private fun holdTile(view: View, id: String) {
        val item = recycler.findContainingItemView(view) ?: return
        if (rows.editing) return
        drag.arm(item, id, drag.lastDownX, drag.lastDownY)
        openMenu(item, id, drag.lastDownX, drag.lastDownY)
    }

    private fun openMenuAtCentre(view: View, id: String) {
        val at = IntArray(2)
        view.getLocationOnScreen(at)
        openMenu(view, id, at[0] + view.width / 2f, at[1] + view.height / 2f)
    }

    private fun openMenu(view: View, id: String, x: Float, y: Float) {
        val tile = rows.tile(id) ?: return
        val context = recycler.context
        closeMenu()
        menuOwner = null
        val rowsOfMenu = buildList {
            add(MenuRow(context.getString(R.string.fork_menu_open), R.drawable.ic_menu_open) {
                actions.onCategory(view, tile.entry.group, tile.entry.apps)
            })
            add(MenuRow(context.getString(R.string.fork_menu_options), R.drawable.ic_menu_edit) {
                showOptions(view, tile.entry.group, tile.entry.apps)
            })
            add(MenuRow(context.getString(R.string.fork_folder_menu), R.drawable.ic_menu_move_to_folder) {
                actions.onCategoryLongPress(view, tile.entry.group, tile.entry.apps)
            })
            add(MenuRow(context.getString(R.string.fork_menu_compact), R.drawable.ic_menu_sort) {
                commitCells(CellGrid.compact(rows.placed()), null)
            })
            if (!rows.editing) {
                add(MenuRow(context.getString(R.string.fork_menu_arrange), R.drawable.ic_menu_widgets) {
                    arranger.start()
                })
            }
        }
        val layouts = layoutRow(context, id, tile.options)
        drag.outlineColor = look.accent
        menu.show(view, x, y, tile.options.size, sizeChoices(context), layouts, rowsOfMenu) { size ->
            resizeTile(id, size)
        }
        val item = recycler.findContainingItemView(view) ?: view
        drag.showHandle(item, id, look.menuText)
    }

    private fun sizeChoices(context: Context): List<SizeChoice> = listOf(
        Triple(CategorySize.SMALL, R.drawable.ic_fork_size_small, R.string.fork_menu_size_small),
        Triple(CategorySize.NORMAL, R.drawable.ic_fork_size_normal, R.string.fork_menu_size_normal),
        Triple(CategorySize.TALL, R.drawable.ic_fork_size_tall, R.string.fork_menu_size_tall),
        Triple(CategorySize.WIDE, R.drawable.ic_fork_size_wide, R.string.fork_menu_size_wide),
        Triple(CategorySize.STRIP, R.drawable.ic_fork_size_strip, R.string.fork_menu_size_strip),
        Triple(CategorySize.LARGE, R.drawable.ic_fork_size_large, R.string.fork_menu_size_large)
    ).map { (size, icon, label) -> SizeChoice(size, icon, context.getString(label)) }

    // How a tile lays out its icons: how many of them are large. Only a normal tile (3, 2, 1 or none) and a strip (4 or
    // 5 icons) have a choice; the other sizes have one layout.
    private fun layoutRow(context: Context, id: String, options: CategoryOptions): LayoutRow {
        val choices = when (options.size) {
            CategorySize.NORMAL -> CategoryOptions.LARGE_CHOICES.reversed().map { count ->
                LayoutChoice(count, LARGE_ICONS.getValue(count), context.getString(LARGE_LABELS.getValue(count)))
            }
            CategorySize.STRIP -> CategoryOptions.STRIP_CHOICES.map { count ->
                LayoutChoice(count, LARGE_ICONS.getValue(count), context.getString(LARGE_LABELS.getValue(count)))
            }
            else -> emptyList()
        }
        val current = if (options.size == CategorySize.STRIP) {
            CategoryOptions.stripCount(options.large)
        } else {
            minOf(options.large, CategoryLayout.LARGE_SLOTS)
        }
        return if (choices.isEmpty()) LayoutRow.NONE else LayoutRow(choices, current) { count ->
            actions.edits.onOptions(id) { it.copy(large = count) }
        }
    }

    // A tile given another size: the tiles it now covers go down, and everything is kept in one change.
    private fun resizeTile(id: String, size: CategorySize) {
        val cells = CellGrid.resize(rows.placed(), id, CellFlow.footprint(size))
        commitCells(cells, id to size)
    }

    // The tiles in these cells kept, the order of the folders with them (the other drawer styles read it).
    private fun commitCells(cells: List<Placed>, sized: Pair<String, CategorySize>?) {
        val changes = cells.associate { cell ->
            cell.id to TileChange(sized?.takeIf { it.first == cell.id }?.second, cell.col, cell.row)
        }
        rows.showCells(cells)
        actions.edits.onTiles(changes)
        actions.edits.onOrder(CellGrid.order(cells))
    }

    private val arranger: TileArranger = TileArranger(
        recycler, rows, { actions.edits.onOrder(it) }
    ) { editing ->
        doneBar.visibility = if (editing) View.VISIBLE else View.GONE
        keepClear()
    }

    private val state = CategoryScreenState()
    private val screen = CategoryScreen(container, screenAdapter, { back() }, { picked ->
        openGroup?.let { actions.edits.onPlaceLarge(it, picked) }
    }) { openGroup?.let { actions.edits.onAddApps(it) } }
    // A folder of the drawer opened on a screen in another style: the tiles are not there to close it.
    private var standalone = false
    // Told when a screen opens or closes, so the drawer can hide or show its own list.
    var onScreenChanged: (() -> Unit)? = null
    val isScreenOpen: Boolean get() = state.isOpen
    private var openGroup: DrawerGroup? = null

    init {
        container.addView(
            recycler,
            minOf(1, container.childCount),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        // Over the tiles, under a category's screen.
        container.addView(
            doneBar,
            container.indexOfChild(recycler) + 1,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            )
        )
    }

    // The list keeps clear of what is over its bottom, and, while arranging, of the bar over it; the bar keeps clear
    // of what is over the bottom too.
    private fun keepClear() {
        val clearance = bottomClearance() + systemBarsOverCopy()
        val gap = (BAR_GAP_DP * recycler.resources.displayMetrics.density).toInt()
        val bar = doneBar.layoutParams as FrameLayout.LayoutParams
        if (bar.bottomMargin != clearance + gap || bar.marginStart != sidePx || bar.marginEnd != sidePx) {
            bar.setMargins(sidePx, 0, sidePx, clearance + gap)
            doneBar.layoutParams = bar
        }
        val barSpace = if (arranger.isOn) doneBar.height + gap else 0
        val bottom = baseBottomPx + clearance + barSpace
        if (recycler.paddingBottom != bottom) recycler.setPadding(sidePx, 0, sidePx, bottom)
    }

    // How much of the bottom of this list the system's navigation bar covers: the drawer's list runs under it, the
    // home screen's pages stop above the dock.
    private fun systemBarsOverCopy(): Int {
        val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        val bar = ViewCompat.getRootWindowInsets(recycler)?.getInsets(types)?.bottom
        val host = recycler.parent as? View
        if (bar == null || host == null) return 0
        val at = IntArray(2)
        host.getLocationInWindow(at)
        return BottomClearance.underNavigation(at[1] + host.height, host.rootView.height, bar)
    }

    // The drawer's swipe to the neighbouring tab, on the tiles and on a category's screen. Not while the
    // tiles are arranged: a drag is the tile's then.
    fun addTabSwipe(onTiles: RecyclerView.OnItemTouchListener, onScreen: RecyclerView.OnItemTouchListener) {
        recycler.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean =
                !arranger.isOn && onTiles.onInterceptTouchEvent(rv, e)

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = onTiles.onTouchEvent(rv, e)

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) =
                onTiles.onRequestDisallowInterceptTouchEvent(disallowIntercept)
        })
        screen.grid.addOnItemTouchListener(onScreen)
    }

    // Whether the drawer wants the tiles on screen, open category or not.
    private var wanted = false

    val isVisible: Boolean get() = wanted

    fun setVisible(show: Boolean) {
        wanted = show
        if (!show) arranger.stop()
        // The tiles going away (search, a tab, another style) takes the screen with them.
        if (!show && !standalone) closeScreen(animate = false)
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
    fun openScreen(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook, standalone: Boolean = false) {
        this.standalone = standalone
        state.open(group.id)
        openGroup = group
        showTiles()
        screen.show(group, apps, look, animate = true)
        onScreenChanged?.invoke()
    }

    // Back: choosing large icons stops first, then the screen closes. True
    // when it did either, so the Back is spent.
    fun back(): Boolean = closeMenu() || arranger.stop() || screen.cancelPicking() || closeScreen()

    // A tile's menu is up: Back closes it first.
    private fun closeMenu(): Boolean {
        val showing = menuOwner?.isShowing == true
        if (showing) menuOwner?.dismiss()
        return showing
    }

    // A tap on an app of the screen: a choice while choosing, else [launch].
    fun onScreenAppClick(launch: () -> Unit, app: LaunchableApp) {
        if (!screen.handleTap(app)) launch()
    }

    // True when a screen was open and is closed now: the Back that did it is spent.
    fun closeScreen(animate: Boolean = true): Boolean {
        arranger.stop()
        val wasOpen = state.close()
        standalone = false
        showTiles()
        if (wasOpen) onScreenChanged?.invoke()
        screen.hide(animate && wasOpen)
        return wasOpen
    }

    // [showRecent] is the settings switch for the row over the tiles.
    fun submit(data: CategoriesData, showRecent: Boolean) {
        rows.setLook(data.look)
        look = data.look
        if (menuOwner?.isShowing != true) menuOwner = null
        doneBar.setLook(data.look.textColor, CategoryTint.cardColor(data.look.textColor, data.look.backdrop))
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
            onSort = { sort -> actions.edits.onOptions(group.id) { it.copy(sort = sort) } },
            onSize = { size -> actions.edits.onOptions(group.id) { it.copy(size = size) } },
            onLarge = { count -> actions.edits.onOptions(group.id) { it.copy(large = count) } },
            onHide = { actions.edits.onOptions(group.id) { it.copy(hidden = true) } },
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
        // The icon and the name of each layout, by how many icons are large (a strip's 4 and 5 are its icons).
        val LARGE_ICONS = mapOf(
            0 to R.drawable.ic_fork_large_0, 1 to R.drawable.ic_fork_large_1, 2 to R.drawable.ic_fork_large_2,
            3 to R.drawable.ic_fork_large_3, 4 to R.drawable.ic_fork_strip_4, 5 to R.drawable.ic_fork_strip_5
        )
        val LARGE_LABELS = mapOf(
            0 to R.string.fork_menu_large_0, 1 to R.string.fork_menu_large_1, 2 to R.string.fork_menu_large_2,
            3 to R.string.fork_menu_large_3, 4 to R.string.fork_menu_strip_4, 5 to R.string.fork_menu_strip_5
        )
        const val MIN_TILES = 2
        const val TILE_MAX_DP = 200f
        const val SIDE_DP = 6f
        const val BAR_GAP_DP = 8f
    }
}
