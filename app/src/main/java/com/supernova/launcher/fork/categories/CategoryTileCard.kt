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

// The round, translucent card of one category: three large icons (a tap
// launches the app) and a corner with up to four small ones (a tap opens the
// whole category). The card is square and lays its children out by hand, so a
// bind touches seven image views and nothing else.
internal class CategoryTileCard(context: Context) : ViewGroup(context) {

    private val corner = View(context)
    private val large = List(CategoryLayout.LARGE_SLOTS) { ImageView(context) }
    private val mini = List(CategoryLayout.MINI_SLOTS) { ImageView(context) }
    private val shape = GradientDrawable()

    // Shown in place of the icons while the category holds no app.
    private val empty = TextView(context).apply {
        setText(R.string.fork_empty_category)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, EMPTY_SP)
        gravity = Gravity.CENTER
        maxLines = EMPTY_LINES
        ellipsize = TextUtils.TruncateAt.END
    }

    init {
        addView(corner)
        addView(empty)
        (large + mini).forEach { view ->
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            addView(view)
        }
        shape.cornerRadius = resources.displayMetrics.density * CORNER_DP
        background = shape
    }

    fun setCardColor(color: Int) = shape.setColor(color)

    fun setTextColor(color: Int) = empty.setTextColor(color)

    fun bind(
        tile: CategoryLayout.Tile<LaunchableApp>,
        icons: CategoryIcons,
        actions: CategoriesActions,
        open: (View) -> Unit,
        openLongPress: (View) -> Unit
    ) {
        empty.visibility = if (tile.large.isEmpty()) VISIBLE else GONE
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
                view.setOnClickListener { actions.onApp(it, app) }
                view.setOnLongClickListener { actions.onAppLongPress(it, app); true }
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
        val cell = cellSize(side)
        val largeSize = MeasureSpec.makeMeasureSpec((cell * ICON_FILL).toInt(), MeasureSpec.EXACTLY)
        large.forEach { it.measure(largeSize, largeSize) }
        val miniSize = MeasureSpec.makeMeasureSpec((cell / 2 * ICON_FILL).toInt(), MeasureSpec.EXACTLY)
        mini.forEach { it.measure(miniSize, miniSize) }
        val cellSpec = MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY)
        corner.measure(cellSpec, cellSpec)
        val inner = side - 2 * padding(side)
        empty.measure(
            MeasureSpec.makeMeasureSpec(inner, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(inner, MeasureSpec.AT_MOST)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val side = r - l
        val pad = padding(side)
        val cell = cellSize(side)
        large.forEachIndexed { index, view ->
            centre(view, pad + (index % 2) * cell, pad + (index / 2) * cell, cell)
        }
        val cornerX = pad + cell
        val cornerY = pad + cell
        corner.layout(cornerX, cornerY, cornerX + cell, cornerY + cell)
        centre(empty, pad, pad, side - 2 * pad)
        val half = cell / 2
        mini.forEachIndexed { index, view ->
            centre(view, cornerX + (index % 2) * half, cornerY + (index / 2) * half, half)
        }
    }

    private fun centre(view: View, left: Int, top: Int, cell: Int) {
        val x = left + (cell - view.measuredWidth) / 2
        val y = top + (cell - view.measuredHeight) / 2
        view.layout(x, y, x + view.measuredWidth, y + view.measuredHeight)
    }

    private fun padding(side: Int): Int = (side * PADDING_SHARE).toInt()

    private fun cellSize(side: Int): Int = (side - 2 * padding(side)) / 2

    companion object {
        private const val CORNER_DP = 24f
        private const val PADDING_SHARE = 0.07f
        private const val ICON_FILL = 0.8f
        private const val EMPTY_SP = 11f
        private const val EMPTY_LINES = 4
    }
}
