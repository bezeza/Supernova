package com.supernova.launcher.fork.categories

// Where every row of the list lies, in pixels from the top of the whole list: the geometry of
// [MosaicLayoutManager], plain arithmetic so it can be tested without a view. Tiles stand in the cells they were
// given. The columns are as wide as a grid's would be (the pixels that do not divide evenly go to the same columns
// they go to there). The row lines lie as low as the tiles need: a line is at least [pitch] under the one above, and
// under the top of every tile that ends on it by that tile's own natural height ([natural]: its card, its name,
// its margins), so a tile is as high as it is on its own, whatever its size, and a row of tiles is as high as its
// highest, as the lists before the mosaic had it. The rows before the first tile
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
        val pitch: Int,
        val natural: (Int, Int) -> Int,
        val top: Int = 0,
        val left: Int = 0,
        val bottom: Int = 0
    )

    private const val PARTS = 4
    private const val LEFT = 0
    private const val TOP = 1
    private const val RIGHT = 2
    private const val BOTTOM = 3

    // The left edges of the columns, as a GridLayoutManager works them out: a width that does not divide evenly
    // gives its spare pixels to the columns that come at the same places there.
    fun borders(width: Int, columns: Int): IntArray {
        val out = IntArray(columns + 1)
        val spare = width % columns
        var spareUsed = 0
        var total = 0
        for (index in 1..columns) {
            var size = width / columns
            spareUsed += spare
            if (spareUsed > 0 && columns - spareUsed < spare) {
                size += 1
                spareUsed -= columns
            }
            total += size
            out[index] = total
        }
        return out
    }

    fun compute(input: Input): Result {
        val rects = IntArray(input.count * PARTS)
        val tiles = (0 until input.count).filter { input.cellAt(it) != null }
        val firstTile = tiles.firstOrNull() ?: input.count
        val lastTile = tiles.lastOrNull() ?: (firstTile - 1)
        var y = flow(input, rects, 0 until firstTile, input.top)
        val mosaicTop = y
        val edge = borders(input.width, input.columns)
        val cells = tiles.mapNotNull { position -> input.cellAt(position)?.let { position to it } }
        val mosaicRows = cells.maxOfOrNull { it.second.bottom } ?: 0
        val line = IntArray(mosaicRows + 1)
        line[0] = mosaicTop
        for (row in 1..mosaicRows) {
            var low = line[row - 1] + input.pitch
            for ((position, cell) in cells) {
                if (cell.bottom == row) {
                    val width = edge[cell.right] - edge[cell.col]
                    low = maxOf(low, line[cell.row] + input.natural(position, width))
                }
            }
            line[row] = low
        }
        for ((position, cell) in cells) {
            put(
                rects, position,
                intArrayOf(
                    input.left + edge[cell.col], line[cell.row], input.left + edge[cell.right], line[cell.bottom]
                )
            )
        }
        y = line[mosaicRows]
        y = flow(input, rects, (lastTile + 1) until input.count, y)
        return Result(rects, y + input.bottom)
    }

    private fun edge(input: Input, column: Int): Int = borders(input.width, input.columns)[column]

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
            put(
                rects, position,
                intArrayOf(input.left + edge(input, column), y, input.left + edge(input, column + span), y + height)
            )
            column += span
            rowHeight = maxOf(rowHeight, height)
        }
        return y + rowHeight
    }
}
