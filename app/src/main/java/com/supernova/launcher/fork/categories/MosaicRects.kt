package com.supernova.launcher.fork.categories

// Where every row of the list lies, in pixels from the top of the whole list: the geometry of
// [MosaicLayoutManager], plain arithmetic so it can be tested without a view. Tiles stand in the cells they were
// given, a row of the grid being [unit] high and a column [width] / [columns] wide; the rows before the first tile
// (the strip of the newest installs) and after the last one (the apps no folder took, and the way back for the
// hidden categories) stand in order of reading, as many across as their spans allow, each row of them as high as its
// highest.
internal object MosaicRects {

    // [rects] holds left, top, right and bottom of each position, four ints after four.
    class Result(val rects: IntArray, val height: Int) {
        fun left(position: Int) = rects[position * PARTS + LEFT]
        fun top(position: Int) = rects[position * PARTS + TOP]
        fun right(position: Int) = rects[position * PARTS + RIGHT]
        fun bottom(position: Int) = rects[position * PARTS + BOTTOM]
    }

    // What the geometry needs from the list and the screen.
    @Suppress("LongParameterList") // A bag of what the screen gives, each read once.
    class Input(
        val count: Int,
        val cellAt: (Int) -> Placed?,
        val spanAt: (Int) -> Int,
        val heightAt: (Int) -> Int,
        val width: Int,
        val columns: Int,
        val unit: Int,
        val top: Int = 0,
        val left: Int = 0,
        val bottom: Int = 0
    )

    private const val PARTS = 4
    private const val LEFT = 0
    private const val TOP = 1
    private const val RIGHT = 2
    private const val BOTTOM = 3

    private fun x(input: Input, column: Int): Int = input.left + column * input.width / input.columns

    fun compute(input: Input): Result {
        val rects = IntArray(input.count * PARTS)
        val tiles = (0 until input.count).filter { input.cellAt(it) != null }
        val firstTile = tiles.firstOrNull() ?: input.count
        val lastTile = tiles.lastOrNull() ?: (firstTile - 1)
        var y = flow(input, rects, 0 until firstTile, input.top)
        val mosaicTop = y
        var mosaicRows = 0
        for (position in tiles) {
            val cell = input.cellAt(position) ?: continue
            put(
                rects, position,
                intArrayOf(
                    x(input, cell.col), mosaicTop + cell.row * input.unit,
                    x(input, cell.right), mosaicTop + cell.bottom * input.unit
                )
            )
            mosaicRows = maxOf(mosaicRows, cell.bottom)
        }
        y = mosaicTop + mosaicRows * input.unit
        y = flow(input, rects, (lastTile + 1) until input.count, y)
        return Result(rects, y + input.bottom)
    }

    private fun put(rects: IntArray, position: Int, box: IntArray) {
        box.copyInto(rects, position * PARTS)
    }

    // The rows in [range] one after another from [start]; the y under the last one.
    private fun flow(input: Input, rects: IntArray, range: IntRange, start: Int): Int {
        var y = start
        var column = 0
        var rowHeight = 0
        for (position in range) {
            val span = input.spanAt(position).coerceIn(1, input.columns)
            if (column + span > input.columns) {
                y += rowHeight
                column = 0
                rowHeight = 0
            }
            val height = input.heightAt(position)
            put(rects, position, intArrayOf(x(input, column), y, x(input, column + span), y + height))
            column += span
            rowHeight = maxOf(rowHeight, height)
        }
        return y + rowHeight
    }
}
