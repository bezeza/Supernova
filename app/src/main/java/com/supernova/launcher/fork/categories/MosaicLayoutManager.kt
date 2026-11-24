package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// What the layout asks of the list's adapter: where a tile stands, and how wide the other rows are.
internal interface MosaicSource {
    fun cellAt(position: Int): Placed?
    fun spanAt(position: Int, spanCount: Int): Int

    // How high a row of the mosaic is for columns [columnWidth] wide.
    fun unitHeight(context: Context, columnWidth: Int): Int

    // The kind of the row: rows of one kind are as high as each other.
    fun kindAt(position: Int): Int
}

// The list of the Categories tiles laid out as a mosaic: each tile in its own cell of a grid with [spanCount]
// columns, so tiles of any size stand side by side with holes between them, and the rows around the tiles in
// order. The geometry is [MosaicRects]; this only measures, places and scrolls. The whole geometry is worked out
// at each layout (a list of tiles is short), the rows on the screen are all that is attached.
@Suppress("TooManyFunctions") // A layout manager is many small overrides.
internal class MosaicLayoutManager(private val source: MosaicSource) : RecyclerView.LayoutManager() {

    var spanCount = CellGrid.COLUMNS
        set(value) {
            if (field == value || value < 1) return
            field = value
            requestLayout()
        }

    private var host: RecyclerView? = null

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        host = view
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        super.onDetachedFromWindow(view, recycler)
        host = null
    }

    private var geometry = MosaicRects.Result(IntArray(0), 0)
    private var scrolled = 0

    // The height a kind of row has, measured once per width, for the rows the grid does not size.
    private val heights = HashMap<Int, Int>()
    private var measuredFor = 0

    // A row to bring to a place on the screen at the next layout: its position and where its top is to be.
    private var pendingPosition = RecyclerView.NO_POSITION
    private var pendingTop = 0

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
        RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun canScrollVertically(): Boolean = true

    override fun supportsPredictiveItemAnimations(): Boolean = false

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)
        val width = width - paddingLeft - paddingRight
        val context = host?.context
        if (itemCount == 0 || width <= 0 || context == null) {
            geometry = MosaicRects.Result(IntArray(0), 0)
            if (itemCount == 0) scrolled = 0
            return
        }
        val unit = source.unitHeight(context, width / spanCount).coerceAtLeast(1)
        if (measuredFor != width / spanCount) {
            heights.clear()
            measuredFor = width / spanCount
        }
        geometry = MosaicRects.compute(
            MosaicRects.Input(
                count = itemCount,
                cellAt = source::cellAt,
                spanAt = { source.spanAt(it, spanCount) },
                heightAt = { rowHeight(recycler, it, width) },
                width = width,
                columns = spanCount,
                unit = unit,
                top = paddingTop,
                left = paddingLeft,
                bottom = paddingBottom
            )
        )
        if (pendingPosition in 0 until itemCount) {
            scrolled = geometry.top(pendingPosition) - pendingTop
            pendingPosition = RecyclerView.NO_POSITION
        }
        scrolled = scrolled.coerceIn(0, maxScroll())
        fill(recycler)
    }

    // A row the grid does not size is as high as its kind of row is at this width, measured the first time.
    private fun rowHeight(recycler: RecyclerView.Recycler, position: Int, width: Int): Int {
        val span = source.spanAt(position, spanCount).coerceIn(1, spanCount)
        val type = source.kindAt(position)
        return heights.getOrPut(type) {
            val view = recycler.getViewForPosition(position)
            addView(view)
            val params = view.layoutParams as RecyclerView.LayoutParams
            view.measure(
                View.MeasureSpec.makeMeasureSpec(
                    width * span / spanCount - params.leftMargin - params.rightMargin, View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val measured = view.measuredHeight + params.topMargin + params.bottomMargin
            removeAndRecycleView(view, recycler)
            measured
        }
    }

    private fun maxScroll(): Int = (geometry.height - height).coerceAtLeast(0)

    private fun visible(position: Int): Boolean =
        geometry.bottom(position) - scrolled > 0 && geometry.top(position) - scrolled < height

    // The rows on the screen attached, the others given back.
    private fun fill(recycler: RecyclerView.Recycler) {
        val attached = HashSet<Int>()
        for (index in childCount - 1 downTo 0) {
            val child = getChildAt(index) ?: continue
            val position = getPosition(child)
            if (position !in 0 until itemCount || !visible(position)) {
                removeAndRecycleView(child, recycler)
            } else {
                attached.add(position)
            }
        }
        for (position in 0 until itemCount) {
            if (visible(position) && position !in attached) place(recycler, position)
        }
    }

    private fun place(recycler: RecyclerView.Recycler, position: Int) {
        val view = recycler.getViewForPosition(position)
        addView(view)
        val params = view.layoutParams as RecyclerView.LayoutParams
        val boxWidth = geometry.right(position) - geometry.left(position) - params.leftMargin - params.rightMargin
        val boxHeight = geometry.bottom(position) - geometry.top(position) - params.topMargin - params.bottomMargin
        val isTile = source.cellAt(position) != null
        view.measure(
            View.MeasureSpec.makeMeasureSpec(boxWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(
                boxHeight, if (isTile) View.MeasureSpec.AT_MOST else View.MeasureSpec.EXACTLY
            )
        )
        val left = geometry.left(position) + params.leftMargin
        val top = geometry.top(position) - scrolled + params.topMargin
        layoutDecorated(view, left, top, left + view.measuredWidth, top + view.measuredHeight)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val target = if (itemCount == 0 || childCount == 0) scrolled else (scrolled + dy).coerceIn(0, maxScroll())
        val consumed = target - scrolled
        if (consumed != 0) {
            scrolled = target
            offsetChildrenVertical(-consumed)
            fill(recycler)
        }
        return consumed
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int = scrolled

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int = geometry.height

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int = height

    override fun scrollToPosition(position: Int) {
        scrollToPositionWithOffset(position, 0)
    }

    // The row at [position] with its top [top] pixels from the top of the list, as a LinearLayoutManager's.
    fun scrollToPositionWithOffset(position: Int, top: Int) {
        pendingPosition = position
        pendingTop = top
        requestLayout()
    }

    // The first row on the screen, going by where rows stand from the top: the one nearest the top edge.
    fun findFirstVisibleItemPosition(): Int {
        var best = RecyclerView.NO_POSITION
        var bestTop = Int.MAX_VALUE
        for (index in 0 until childCount) {
            val child = getChildAt(index) ?: continue
            if (child.bottom > 0 && child.top < bestTop) {
                best = getPosition(child)
                bestTop = child.top
            }
        }
        return best
    }
}
