package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.View
import android.view.ViewGroup

// The two small tiles of a pair, one over the other, in a column as high as the
// normal tile beside it ([TileGeometry.pairHeight]): the first at the top, the
// second with its name on the line the normal tile's name is on. If a large
// font makes the two taller than that, their paddings overlap a little.
internal class PairColumn(context: Context) : ViewGroup(context) {

    // The gap under a normal tile's card and one line of its name, in pixels.
    var titleReserve: () -> Int = { 0 }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val across = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) getChildAt(index).measure(across, MeasureSpec.UNSPECIFIED)
        setMeasuredDimension(width, TileGeometry.pairHeight(width, titleReserve()))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val height = b - t
        val first: View? = getChildAt(0)
        val second: View? = getChildAt(1)
        first?.layout(0, 0, first.measuredWidth, first.measuredHeight)
        second?.layout(0, height - second.measuredHeight, second.measuredWidth, height)
    }
}
