package com.supernova.launcher.fork.categories

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
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
internal class CategoryScreen(
    container: FrameLayout,
    private val cells: AppDrawerAdapter,
    private val onBack: () -> Unit
) {

    private val context = container.context
    private val density = context.resources.displayMetrics.density
    private var showIcon = false
    private var shownName: String? = null
    private var textColor = 0

    private val backButton = ImageButton(context).apply {
        setImageResource(R.drawable.ic_back)
        background = rippleBackground()
        contentDescription = context.getString(R.string.back)
        setOnClickListener { onBack() }
    }

    private val title = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }

    private val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val side = dp(HEADER_SIDE_DP)
        setPadding(side, dp(HEADER_VERTICAL_DP), side, dp(HEADER_VERTICAL_DP))
        val button = dp(BUTTON_DP)
        addView(backButton, LinearLayout.LayoutParams(button, button))
        addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(TITLE_GAP_DP)
        })
    }

    private val grid = RecyclerView(context).apply {
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

    fun show(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook, textColor: Int, animate: Boolean) {
        setTextColor(textColor)
        title.text = group.name
        shownName = group.name
        showGlyph(group.name, textColor)
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
        cells.submit(apps, emptyList(), true, AppDrawerAdapter.Order.UNSORTED, replace = false)
    }

    fun hide(animate: Boolean) {
        root.animate().cancel()
        if (!animate || !isShown) {
            root.visibility = View.GONE
            return
        }
        root.animate().alpha(0f).scaleX(OPEN_SCALE).scaleY(OPEN_SCALE).setDuration(ANIMATION_MS)
            .withEndAction { root.visibility = View.GONE }.start()
    }

    fun rebind() = cells.notifyItemRangeChanged(0, cells.itemCount)

    // Off by default: the name stands alone, as on a tile.
    fun setShowIcon(show: Boolean) {
        if (show == showIcon) return
        showIcon = show
        if (shownName != null) showGlyph(shownName.orEmpty(), textColor)
    }

    fun setTextColor(color: Int) {
        textColor = color
        title.setTextColor(color)
        backButton.imageTintList = ColorStateList.valueOf(color)
    }

    private fun showGlyph(name: String, color: Int) {
        val glyph = if (showIcon) CategoryGlyphs.forName(context, name) else null
        if (glyph == null) {
            title.setCompoundDrawablesRelative(null, null, null, null)
            return
        }
        val size = dp(GLYPH_DP)
        glyph.setTint(color)
        glyph.setBounds(0, 0, size, size)
        title.setCompoundDrawablesRelative(glyph, null, null, null)
        title.compoundDrawablePadding = dp(GLYPH_GAP_DP)
    }

    private fun rippleBackground() = TypedValue().let { value ->
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
        AppCompatResources.getDrawable(context, value.resourceId)
    }

    private fun dp(value: Float): Int = (value * density).toInt()

    private companion object {
        const val TITLE_SP = 20f
        const val HEADER_SIDE_DP = 8f
        const val HEADER_VERTICAL_DP = 4f
        const val BUTTON_DP = 48f
        const val TITLE_GAP_DP = 4f
        const val GLYPH_DP = 22f
        const val GLYPH_GAP_DP = 8f
        const val ANIMATION_MS = 180L
        const val OPEN_SCALE = 0.96f
    }
}
