package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.ui.AppDrawerAdapter

// The content of one category's screen, with nothing about where it sits: the
// name and a back button on top, the category's apps in the drawer's own grid
// below. It draws with the drawer's own adapter, so icons, labels, badges, taps
// and long presses are the drawer's. Back, the arrow and a swipe to the right
// all go to [onBack]. The button on top chooses the category's large icons:
// tapped apps, in turn. The view is [view]; whoever hosts it (the full screen
// over the tiles, [CategoryScreen], or a page of a pager) adds it where it
// wants it and decides how it comes and goes.
internal class CategoryPage(
    context: Context,
    private val cells: AppDrawerAdapter,
    private val onBack: () -> Unit,
    // The apps chosen as large icons, in the order chosen.
    private val onPicked: (List<LaunchableApp>) -> Unit,
    // The header's "+": add apps to the category.
    private val onAdd: () -> Unit
) {

    private val density = context.resources.displayMetrics.density
    private val appContext = context
    private var picker: LargePicker<LaunchableApp>? = null
    private var shownApps: List<LaunchableApp> = emptyList()

    private val header = CategoryHeader(context).also {
        it.onBack = { onBack() }
        it.onPick = { startPicking() }
        it.onAdd = { onAdd() }
    }

    // The apps' grid; the controller hangs the drawer's tab swipe on it.
    val grid = RecyclerView(context).apply {
        val manager = GridLayoutManager(context, 1)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = cells.spanSizeAt(position, manager.spanCount)
        }
        layoutManager = manager
        adapter = cells
        itemAnimator = null
        clipToPadding = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    val view = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        // What is below is not to take a touch that lands between icons.
        isClickable = true
        val wide = ViewGroup.LayoutParams.MATCH_PARENT
        addView(header, LinearLayout.LayoutParams(wide, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(grid, LinearLayout.LayoutParams(wide, 0, 1f))
    }

    private val swipe = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        // A tap on empty space of the grid, between and under the icons, leaves the screen as Back does. Not while
        // the large icons are being chosen: a tap is a choice then.
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val onEmpty = picker == null && grid.findChildViewUnder(e.x, e.y) == null
            if (onEmpty) onBack()
            return onEmpty
        }

        override fun onFling(first: MotionEvent?, second: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            first ?: return false
            val goesBack = BackSwipe.isBack(second.x - first.x, second.y - first.y, velocityX, density)
            if (goesBack) onBack()
            return goesBack
        }
    })

    init {
        grid.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                swipe.onTouchEvent(e)
                return false
            }
        })
    }

    // The page takes a category: its name, and its apps in the grid laid out as [look] says, from the top.
    fun bind(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook) {
        picker = null
        shownApps = apps
        header.show(group.name, choosable = apps.size > 1)
        (grid.layoutManager as GridLayoutManager).spanCount = look.columns.coerceAtLeast(1)
        grid.setPadding(look.left, look.top, look.right, look.bottom)
        cells.submit(apps, emptyList(), true, AppDrawerAdapter.Order.UNSORTED, replace = true)
        grid.scrollToPosition(0)
    }

    // New apps for the open category: the grid diffs against what it had.
    fun update(apps: List<LaunchableApp>) {
        shownApps = apps
        header.setChoosable(apps.size > 1)
        cells.submit(apps, emptyList(), true, AppDrawerAdapter.Order.UNSORTED, replace = false)
    }

    // The page is let go: a choosing of large icons in it ends.
    fun release() {
        picker = null
    }

    fun rebind() = cells.notifyItemRangeChanged(0, cells.itemCount)

    fun setLook(textColor: Int, showIcon: Boolean) = header.setLook(textColor, showIcon)

    // True while choosing, whatever the tap was: it is the choosing's, not a launch.
    fun handleTap(app: LaunchableApp): Boolean {
        val current = picker ?: return false
        if (current.pick(app)) finishOrHint(current)
        return true
    }

    private fun finishOrHint(current: LargePicker<LaunchableApp>) {
        if (!current.isComplete) {
            header.setHint(hintFor(current), choosable = true)
            return
        }
        picker = null
        header.setHint(null, choosable = shownApps.size > 1)
        onPicked(current.picked)
    }

    // Back while choosing stops the choosing and leaves the screen open.
    fun cancelPicking(): Boolean {
        if (picker == null) return false
        picker = null
        header.setHint(null, choosable = shownApps.size > 1)
        return true
    }

    private fun startPicking() {
        val slots = minOf(CategoryLayout.LARGE_SLOTS, shownApps.size)
        if (slots < 1) return
        val current = LargePicker<LaunchableApp>(slots)
        picker = current
        header.setHint(hintFor(current), choosable = true)
    }

    private fun hintFor(current: LargePicker<LaunchableApp>): String =
        appContext.getString(R.string.fork_pick_large_hint, current.next, current.slots)
}
