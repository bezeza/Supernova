package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.supernova.launcher.R
import com.supernova.launcher.data.LaunchableApp

// The round, translucent card of one category, square: its large icons (a tap
// launches the app) and small ones, laid out by [TileSlots]. Normally three
// large icons and a corner of four small ones, a tap on the corner opening the
// whole category. The card lays its children out by hand, so a bind touches
// the image views and nothing else.
internal class CategoryTileCard(context: Context) : ViewGroup(context) {

    private val corner = View(context)
    private val large = List(CategoryLayout.LARGE_SLOTS) { ImageView(context) }
    private val mini = List(CategoryLayout.MINI_MAX) { ImageView(context) }
    private val shape = GradientDrawable()
    private var slots: List<TileSlots.Slot> = TileSlots.normal(CategoryLayout.LARGE_SLOTS)

    // Shown in place of the icons while the category holds no app.
    private val empty = TextView(context).apply {
        setText(R.string.fork_empty_category)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, EMPTY_SP)
        gravity = Gravity.CENTER
        maxLines = EMPTY_LINES
        ellipsize = TextUtils.TruncateAt.END
    }

    init {
        addView(empty)
        (large + mini).forEach { view ->
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            addView(view)
        }
        // Last, so over the large icons beside it: its target is bigger than its cell.
        addView(corner)
        shape.cornerRadius = resources.displayMetrics.density * CORNER_DP
        background = shape
    }

    // True: a tap on a large icon launches the app, as in iOS. False: every tap opens the category.
    var launchFromIcons = true

    // The layout of the icons; a bind follows it.
    fun setSlots(layout: List<TileSlots.Slot>) {
        if (layout === slots) return
        slots = layout
        requestLayout()
    }

    fun setColors(card: Int, text: Int) {
        shape.setColor(card)
        empty.setTextColor(text)
    }

    fun bind(
        tile: CategoryLayout.Tile<LaunchableApp>,
        icons: CategoryIcons,
        actions: CategoriesActions,
        open: (View) -> Unit,
        openLongPress: (View) -> Unit
    ) {
        empty.visibility = if (tile.large.isEmpty() && tile.mini.isEmpty()) VISIBLE else GONE
        setOnClickListener(open)
        setOnLongClickListener { openLongPress(it); true }
        corner.setOnClickListener(open)
        corner.setOnLongClickListener { openLongPress(it); true }
        large.forEachIndexed { index, view ->
            val app = tile.large.getOrNull(index)
            view.visibility = if (app == null) INVISIBLE else VISIBLE
            if (app == null) {
                icons.clear(view)
                view.setOnClickListener(null)
                view.setOnLongClickListener(null)
                view.isClickable = false
            } else {
                icons.bind(view, app)
                view.contentDescription = app.label
                if (launchFromIcons) {
                    view.setOnClickListener { actions.onApp(it, app) }
                    view.setOnLongClickListener { actions.onAppLongPress(it, app); true }
                } else {
                    // Not tappable themselves: a tap anywhere on the tile opens
                    // the category, and apps launch from its screen.
                    view.setOnClickListener(null)
                    view.setOnLongClickListener(null)
                    view.isClickable = false
                }
            }
        }
        mini.forEachIndexed { index, view ->
            val app = tile.mini.getOrNull(index)
            view.visibility = if (app == null) INVISIBLE else VISIBLE
            if (app == null) icons.clear(view) else icons.bind(view, app, badge = false)
        }
    }

    // The settings preview: drawables straight in, nothing tappable.
    fun showPreview(largeIcons: List<Drawable>, miniIcons: List<Drawable>) {
        large.forEachIndexed { index, view -> view.setImageDrawable(largeIcons.getOrNull(index)) }
        mini.forEachIndexed { index, view -> view.setImageDrawable(miniIcons.getOrNull(index)) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val side = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(side, side)
        val inner = side - 2 * padding(side)
        val largeSlots = slots.filter { it.large }
        val miniSlots = slots.filter { !it.large }
        fun measureIcon(view: View, slot: TileSlots.Slot?) {
            val size = if (slot == null) 0 else (slot.size * inner * ICON_FILL).toInt()
            val spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
            view.measure(spec, spec)
        }
        large.forEachIndexed { index, view -> measureIcon(view, largeSlots.getOrNull(index)) }
        mini.forEachIndexed { index, view -> measureIcon(view, miniSlots.getOrNull(index)) }
        val target = cornerTarget(side)
        corner.measure(
            MeasureSpec.makeMeasureSpec(target?.width ?: 0, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(target?.height ?: 0, MeasureSpec.EXACTLY)
        )
        empty.measure(
            MeasureSpec.makeMeasureSpec(inner, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(inner, MeasureSpec.AT_MOST)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val side = r - l
        val pad = padding(side)
        val inner = side - 2 * pad
        val largeSlots = slots.filter { it.large }
        val miniSlots = slots.filter { !it.large }
        large.forEachIndexed { index, view -> place(view, largeSlots.getOrNull(index), pad, inner) }
        mini.forEachIndexed { index, view -> place(view, miniSlots.getOrNull(index), pad, inner) }
        val target = cornerTarget(side)
        corner.layout(target?.left ?: 0, target?.top ?: 0, target?.right ?: 0, target?.bottom ?: 0)
        centre(empty, pad, pad, inner)
    }

    // The corner of small icons has a target of its own only in the usual layout
    // (three large icons); in the others a tap off an icon is the card's.
    private fun cornerTarget(side: Int): TileGeometry.Rect? {
        if (TileSlots.largeCount(slots) != CategoryLayout.LARGE_SLOTS) return null
        val pad = padding(side)
        return TileGeometry.cornerTarget(side, pad, (side - 2 * pad) / 2)
    }

    private fun place(view: View, slot: TileSlots.Slot?, pad: Int, inner: Int) {
        if (slot == null) {
            view.layout(0, 0, 0, 0)
            return
        }
        centre(view, pad + (slot.x * inner).toInt(), pad + (slot.y * inner).toInt(), (slot.size * inner).toInt())
    }

    private fun centre(view: View, left: Int, top: Int, cell: Int) {
        val x = left + (cell - view.measuredWidth) / 2
        val y = top + (cell - view.measuredHeight) / 2
        view.layout(x, y, x + view.measuredWidth, y + view.measuredHeight)
    }

    private fun padding(side: Int): Int = (side * PADDING_SHARE).toInt()

    companion object {
        private const val CORNER_DP = 24f
        private const val PADDING_SHARE = 0.07f
        private const val ICON_FILL = 0.8f
        private const val EMPTY_SP = 11f
        private const val EMPTY_LINES = 4
    }
}
