package com.supernova.launcher.fork.categories

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.ui.AppDrawerAdapter

// How the drawer's own grid is laid out, which the category's screen copies so
// its apps sit as they do in the drawer: the column count and the grid's padding.
class ScreenLook(val columns: Int, val left: Int, val top: Int, val right: Int, val bottom: Int)

// One category on a screen of its own, over the tiles, like a folder in App
// Library or a category in HyperOS: the name and a back button on top, the
// category's apps in the drawer's own grid below. It draws with the drawer's
// own adapter, so icons, labels, badges, taps and long presses are the
// drawer's. Back, the arrow and a swipe to the right all return to the tiles.
// The button on top chooses the category's large icons: tapped apps, in turn.
internal class CategoryScreen(
    container: FrameLayout,
    private val cells: AppDrawerAdapter,
    private val onBack: () -> Unit,
    // The apps chosen as large icons, in the order chosen.
    private val onPicked: (List<LaunchableApp>) -> Unit
) {

    private val context = container.context
    private val density = context.resources.displayMetrics.density
    private var picker: LargePicker<LaunchableApp>? = null
    private var shownApps: List<LaunchableApp> = emptyList()

    private val header = CategoryHeader(context).also {
        it.onBack = { onBack() }
        it.onPick = { startPicking() }
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

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        // The tiles below are not to take a touch that lands between icons.
        isClickable = true
        val wide = ViewGroup.LayoutParams.MATCH_PARENT
        addView(header, LinearLayout.LayoutParams(wide, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(grid, LinearLayout.LayoutParams(wide, 0, 1f))
    }

    private val swipe = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(first: MotionEvent?, second: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            first ?: return false
            val goesBack = BackSwipe.isBack(second.x - first.x, second.y - first.y, velocityX, density)
            if (goesBack) onBack()
            return goesBack
        }
    })

    init {
        // Above the tiles' list, which sits at index 1.
        container.addView(
            root,
            container.childCount,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        grid.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                swipe.onTouchEvent(e)
                return false
            }
        })
    }

    val isShown: Boolean get() = root.isVisible

    fun show(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook, animate: Boolean) {
        picker = null
        shownApps = apps
        header.show(group.name, choosable = apps.size > 1)
        (grid.layoutManager as GridLayoutManager).spanCount = look.columns.coerceAtLeast(1)
        grid.setPadding(look.left, look.top, look.right, look.bottom)
        cells.submit(apps, emptyList(), true, AppDrawerAdapter.Order.UNSORTED, replace = true)
        grid.scrollToPosition(0)
        root.animate().cancel()
        root.visibility = View.VISIBLE
        if (animate) {
            root.alpha = 0f
            root.scaleX = OPEN_SCALE
            root.scaleY = OPEN_SCALE
            root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ANIMATION_MS).start()
        } else {
            root.alpha = 1f
            root.scaleX = 1f
            root.scaleY = 1f
        }
    }

    // New apps for the open category: the grid diffs against what it had.
    fun update(apps: List<LaunchableApp>) {
        shownApps = apps
        header.setChoosable(apps.size > 1)
        cells.submit(apps, emptyList(), true, AppDrawerAdapter.Order.UNSORTED, replace = false)
    }

    fun hide(animate: Boolean) {
        picker = null
        root.animate().cancel()
        if (!animate || !isShown) {
            root.visibility = View.GONE
            return
        }
        root.animate().alpha(0f).scaleX(OPEN_SCALE).scaleY(OPEN_SCALE).setDuration(ANIMATION_MS)
            .withEndAction { root.visibility = View.GONE }.start()
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
        context.getString(R.string.fork_pick_large_hint, current.next, current.slots)

    private companion object {
        const val ANIMATION_MS = 180L
        const val OPEN_SCALE = 0.96f
    }
}
