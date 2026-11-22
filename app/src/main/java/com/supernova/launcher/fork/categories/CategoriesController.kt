package com.supernova.launcher.fork.categories

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.data.AppCatalog
import com.supernova.launcher.data.LaunchableApp

// The Categories drawer: a list of its own that sits over the drawer's usual
// one while the style is on, the All tab is showing and nothing is searched.
// Search results and the group tabs stay the drawer's own lists, so they work
// as they do in every other style. The drawer feeds it through [submit] and
// shows it through [setVisible]; nothing else of the drawer is replaced.
class CategoriesController(
    container: FrameLayout,
    catalog: AppCatalog,
    decorateIcon: (Drawable?, LaunchableApp) -> Drawable?,
    actions: CategoriesActions
) {

    private val rows = CategoriesAdapter(CategoryIcons(catalog, decorateIcon), actions)

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

    init {
        container.addView(
            recycler,
            1,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    val isVisible: Boolean get() = recycler.visibility == View.VISIBLE

    fun setVisible(show: Boolean) {
        recycler.visibility = if (show) View.VISIBLE else View.GONE
    }

    // [showRecent] is the settings switch for the row over the tiles.
    fun submit(data: CategoriesData, showRecent: Boolean) {
        rows.setTextColor(data.textColor)
        val context = recycler.context
        val list = ArrayList<CategoryRow>()
        val recent = if (showRecent) {
            CategoryLayout.recentlyInstalled(data.recent, CategoryLayout.RECENT_LIMIT) { it.installTime }
        } else {
            emptyList()
        }
        if (recent.isNotEmpty()) list.add(CategoryRow.Recent(recent))
        data.categories.filter { it.apps.isNotEmpty() }.mapTo(list) { CategoryRow.Tile(it) }
        if (data.loose.isNotEmpty()) {
            if (data.categories.isNotEmpty()) {
                list.add(CategoryRow.Header(context.getString(R.string.categories_other_title)))
            }
            data.loose.mapTo(list) { CategoryRow.Loose(it) }
        }
        rows.submitList(list)
    }

    // A badge or an icon moved: the rows rebind, the list itself is unchanged.
    fun rebind() = rows.notifyItemRangeChanged(0, rows.itemCount)

    private companion object {
        const val MIN_TILES = 2
        const val TILE_MAX_DP = 200f
        const val SIDE_DP = 6f
    }
}
