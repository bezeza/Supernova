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
internal class CategoryTileCard(context: Context) : EditableCard(context) {

    private val corner = View(context)
    private val large = List(CategoryLayout.LARGE_MAX) { ImageView(context) }
    private val mini = List(CategoryLayout.MINI_MAX) { ImageView(context) }
    private val shape = GradientDrawable()
    private var slots: List<TileSlots.Slot> = TileSlots.normal(CategoryLayout.LARGE_SLOTS)


    // A small tile's name, in a strip along the top of the card: the one name it has, none under the card.
    private val name = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, NAME_SP)
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        visibility = GONE
    }

    // Shown in place of the icons while the category holds no app.
    private val empty = TextView(context).apply {
        setText(R.string.fork_empty_category)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, EMPTY_SP)
        gravity = Gravity.CENTER
        maxLines = EMPTY_LINES
        ellipsize = TextUtils.TruncateAt.END
    }

    init {
        addView(name)
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
        val named = TileSlots.nameShare(layout) > 0f
        name.visibility = if (named) VISIBLE else GONE
        requestLayout()
    }

    fun setName(text: String) {
        name.text = text
    }

    // What the card says while it holds no app: [text], or the usual words for an empty category.
    fun setEmptyText(text: String?) {
        empty.text = text ?: context.getString(R.string.fork_empty_category)
    }

    fun setColors(card: Int, text: Int) {
        shape.setColor(card)
        empty.setTextColor(text)
        name.setTextColor(text)
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

    // The margin round a tile in the list: a double tile is as high as the normal one beside it.
    var gapPx = 0

    private val geometry = CardGeometry({ slots }, { gapPx })

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = geometry.height(width)
        setMeasuredDimension(width, height)
        val ref = geometry.reference(width, height)
        val largeSlots = slots.filter { it.large }
        val miniSlots = slots.filter { !it.large }
        fun measureIcon(view: View, slot: TileSlots.Slot?) {
            val size = if (slot == null) 0 else (slot.size * ref * ICON_FILL).toInt()
            val spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
            view.measure(spec, spec)
        }
        large.forEachIndexed { index, view -> measureIcon(view, largeSlots.getOrNull(index)) }
        mini.forEachIndexed { index, view -> measureIcon(view, miniSlots.getOrNull(index)) }
        val target = geometry.cornerTarget(width, height)
        corner.measure(
            MeasureSpec.makeMeasureSpec(target?.width ?: 0, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(target?.height ?: 0, MeasureSpec.EXACTLY)
        )
        empty.measure(
            MeasureSpec.makeMeasureSpec(width - 2 * geometry.padding(width, height), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(ref, MeasureSpec.AT_MOST)
        )
        val strip = (TileSlots.nameShare(slots) * ref).toInt()
        name.measure(
            MeasureSpec.makeMeasureSpec(width - 2 * geometry.padding(width, height), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(strip, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val pad = geometry.padding(width, height)
        val ref = geometry.reference(width, height)
        val x0 = geometry.left(width, height)
        val y0 = geometry.top(width, height)
        val largeSlots = slots.filter { it.large }
        val miniSlots = slots.filter { !it.large }
        large.forEachIndexed { index, view -> place(view, largeSlots.getOrNull(index), x0, y0, ref) }
        mini.forEachIndexed { index, view -> place(view, miniSlots.getOrNull(index), x0, y0, ref) }
        val target = geometry.cornerTarget(width, height)
        corner.layout(target?.left ?: 0, target?.top ?: 0, target?.right ?: 0, target?.bottom ?: 0)
        centre(empty, pad, pad, width - 2 * pad, ref)
        name.layout(pad, y0, pad + name.measuredWidth, y0 + name.measuredHeight)
    }

    private fun place(view: View, slot: TileSlots.Slot?, x0: Int, y0: Int, ref: Int) {
        if (slot == null) {
            view.layout(0, 0, 0, 0)
            return
        }
        val size = (slot.size * ref).toInt()
        centre(view, x0 + (slot.x * ref).toInt(), y0 + (slot.y * ref).toInt(), size, size)
    }

    private fun centre(view: View, left: Int, top: Int, width: Int, height: Int) {
        val x = left + (width - view.measuredWidth) / 2
        val y = top + (height - view.measuredHeight) / 2
        view.layout(x, y, x + view.measuredWidth, y + view.measuredHeight)
    }

    companion object {
        private const val CORNER_DP = 24f
        private const val ICON_FILL = 0.8f
        private const val EMPTY_SP = 11f
        private const val NAME_SP = 10f
        private const val EMPTY_LINES = 4
    }
}
